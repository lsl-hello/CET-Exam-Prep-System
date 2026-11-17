package com.cet6.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cet6.common.Constants;
import com.cet6.common.GlobalExceptionHandler.BusinessException;
import com.cet6.dto.WordReviewDTO;
import com.cet6.entity.Cet6Word;
import com.cet6.entity.UserWord;
import com.cet6.entity.UserWordRecord;
import com.cet6.entity.WrongWord;
import com.cet6.mapper.Cet6WordMapper;
import com.cet6.mapper.UserWordMapper;
import com.cet6.mapper.UserWordRecordMapper;
import com.cet6.mapper.WrongWordMapper;
import com.cet6.security.SecurityUtils;
import com.cet6.service.WordStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordStudyServiceImpl extends ServiceImpl<Cet6WordMapper, Cet6Word> implements WordStudyService {

    private final UserWordMapper userWordMapper;
    private final UserWordRecordMapper userWordRecordMapper;
    private final WrongWordMapper wrongWordMapper;

    /** 艾宾浩斯复习间隔（天）：认识/模糊/遗忘 */
    private static final int[] INTERVALS = {7, 3, 1};

    @Override
    public List<Map<String, Object>> getTodayWords(Long bookId, int limit) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> resultWordIds = new HashSet<>();   // 防止同一个单词重复进入列表

        // 1. 先加入错题本里的单词（不算在 limit 计划里，额外加）
        //    不管 reason 是 vague/forget/manual，都拿出来复习
        List<WrongWord> wrongWords = wrongWordMapper.selectList(new LambdaQueryWrapper<WrongWord>()
                .eq(WrongWord::getUserId, userId));
        for (WrongWord ww : wrongWords) {
            Cet6Word word = getById(ww.getWordId());
            if (word != null && (bookId == null || bookId.equals(word.getBookId()))
                    && !resultWordIds.contains(word.getId())) {
                result.add(buildWordMap(word, 3, Constants.MEMORY_FORGET));  // reviewType=3 错题复习
                resultWordIds.add(word.getId());
            }
        }

        // 2. 今日需复习的生词（按 spaced repetition 调度 + 模糊/遗忘每次显示）
        //    limit 只控制计划词数量，错题本单词不算在 limit 里
        List<UserWord> reviewList = userWordMapper.selectList(new LambdaQueryWrapper<UserWord>()
                .eq(UserWord::getUserId, userId)
                .and(w -> w.le(UserWord::getNextReviewTime, now)
                        .or().in(UserWord::getMemoryLevel, Constants.MEMORY_FUZZY, Constants.MEMORY_FORGET))
                .orderByAsc(UserWord::getNextReviewTime));

        Set<Long> reviewWordIds = reviewList.stream().map(UserWord::getWordId).collect(Collectors.toSet());

        int plannedCount = 0;  // 只统计计划词数量（不含错题本）
        for (UserWord uw : reviewList) {
            if (plannedCount >= limit) break;
            Cet6Word word = getById(uw.getWordId());
            if (word != null && (bookId == null || bookId.equals(word.getBookId()))
                    && !resultWordIds.contains(word.getId())) {
                result.add(buildWordMap(word, 2, uw.getMemoryLevel()));
                resultWordIds.add(word.getId());
                plannedCount++;
            }
        }

        // 3. 补充新词（算 limit）
        if (plannedCount < limit) {
            LambdaQueryWrapper<Cet6Word> wrapper = new LambdaQueryWrapper<Cet6Word>()
                    .eq(Cet6Word::getStatus, 1)
                    .orderByAsc(Cet6Word::getSort);
            if (bookId != null) {
                wrapper.eq(Cet6Word::getBookId, bookId);
            }
            List<Cet6Word> allWords = list(wrapper);
            for (Cet6Word word : allWords) {
                if (plannedCount >= limit) break;
                if (!reviewWordIds.contains(word.getId()) && !resultWordIds.contains(word.getId())) {
                    long learned = userWordMapper.selectCount(new LambdaQueryWrapper<UserWord>()
                            .eq(UserWord::getUserId, userId)
                            .eq(UserWord::getWordId, word.getId()));
                    if (learned == 0) {
                        result.add(buildWordMap(word, 1, Constants.MEMORY_FORGET));
                        resultWordIds.add(word.getId());
                        plannedCount++;
                    }
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void submitReview(WordReviewDTO dto) {
        Long userId = SecurityUtils.getUserId();
        Cet6Word word = getById(dto.getWordId());
        if (word == null) {
            throw new BusinessException("单词不存在");
        }

        int memoryResult = dto.getMemoryResult();
        int memoryLevel = memoryResult;
        int intervalDays = INTERVALS[Math.min(memoryResult - 1, 2)];
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReview = now.plusDays(intervalDays);

        UserWord userWord = userWordMapper.selectOne(new LambdaQueryWrapper<UserWord>()
                .eq(UserWord::getUserId, userId)
                .eq(UserWord::getWordId, dto.getWordId()));

        if (userWord == null) {
            userWord = new UserWord();
            userWord.setUserId(userId);
            userWord.setWordId(dto.getWordId());
            userWord.setMemoryLevel(memoryLevel);
            userWord.setLastReviewTime(now);
            userWord.setNextReviewTime(nextReview);
            userWord.setErrorCount(memoryResult == Constants.MEMORY_FORGET ? 1 : 0);
            userWordMapper.insert(userWord);
        } else {
            userWord.setMemoryLevel(memoryLevel);
            userWord.setLastReviewTime(now);
            userWord.setNextReviewTime(nextReview);
            if (memoryResult == Constants.MEMORY_FORGET) {
                userWord.setErrorCount(userWord.getErrorCount() + 1);
            }
            userWordMapper.updateById(userWord);
        }

        UserWordRecord record = new UserWordRecord();
        record.setUserId(userId);
        record.setWordId(dto.getWordId());
        record.setReviewType(dto.getReviewType() != null ? dto.getReviewType() : 1);
        record.setMemoryResult(memoryResult);
        record.setReviewDate(LocalDate.now());
        userWordRecordMapper.insert(record);
    }

    @Override
    public Map<String, Object> getWeakWordReport() {
        Long userId = SecurityUtils.getUserId();
        List<UserWord> weakWords = userWordMapper.selectList(new LambdaQueryWrapper<UserWord>()
                .eq(UserWord::getUserId, userId)
                .in(UserWord::getMemoryLevel, Constants.MEMORY_FUZZY, Constants.MEMORY_FORGET)
                .orderByDesc(UserWord::getErrorCount));

        Map<String, Object> report = new HashMap<>();
        report.put("totalWeak", weakWords.size());
        report.put("forgetCount", weakWords.stream().filter(w -> w.getMemoryLevel() == Constants.MEMORY_FORGET).count());
        report.put("fuzzyCount", weakWords.stream().filter(w -> w.getMemoryLevel() == Constants.MEMORY_FUZZY).count());

        List<Map<String, Object>> details = new ArrayList<>();
        for (UserWord uw : weakWords.stream().limit(20).toList()) {
            Cet6Word word = getById(uw.getWordId());
            if (word != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("word", word.getWord());
                item.put("translate", word.getTranslate());
                item.put("memoryLevel", uw.getMemoryLevel());
                item.put("errorCount", uw.getErrorCount());
                details.add(item);
            }
        }
        report.put("details", details);
        return report;
    }

    @Override
    public Map<String, Object> getWordStat() {
        Long userId = SecurityUtils.getUserId();
        List<UserWord> userWords = userWordMapper.selectList(new LambdaQueryWrapper<UserWord>()
                .eq(UserWord::getUserId, userId));

        long know = userWords.stream().filter(w -> w.getMemoryLevel() != null && w.getMemoryLevel() == Constants.MEMORY_MASTER).count();
        long vague = userWords.stream().filter(w -> w.getMemoryLevel() != null && w.getMemoryLevel() == Constants.MEMORY_FUZZY).count();
        long forget = userWords.stream().filter(w -> w.getMemoryLevel() != null && w.getMemoryLevel() == Constants.MEMORY_FORGET).count();

        Map<String, Object> stat = new HashMap<>();
        stat.put("know", know);
        stat.put("vague", vague);
        stat.put("forget", forget);
        stat.put("total", (long) userWords.size());
        return stat;
    }

    private Map<String, Object> buildWordMap(Cet6Word word, int reviewType, int memoryLevel) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", word.getId());
        map.put("word", word.getWord());
        map.put("phonetic", word.getPhonetic());
        map.put("speech", word.getSpeech());
        map.put("translate", word.getTranslate());
        map.put("sentence", word.getSentence());
        map.put("audioUrl", word.getAudioUrl());
        map.put("reviewType", reviewType);
        map.put("memoryLevel", memoryLevel);
        return map;
    }
}
