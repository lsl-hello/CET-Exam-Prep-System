package com.cet6.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cet6.common.GlobalExceptionHandler.BusinessException;
import com.cet6.dto.AnswerSubmitDTO;
import com.cet6.entity.Cet6Question;
import com.cet6.entity.UserErrorQuestion;
import com.cet6.entity.UserQuestionRecord;
import com.cet6.mapper.Cet6QuestionMapper;
import com.cet6.mapper.UserErrorQuestionMapper;
import com.cet6.mapper.UserQuestionRecordMapper;
import com.cet6.security.SecurityUtils;
import com.cet6.service.QuestionStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionStudyServiceImpl implements QuestionStudyService {

    private final Cet6QuestionMapper questionMapper;
    private final UserQuestionRecordMapper recordMapper;
    private final UserErrorQuestionMapper errorMapper;

    private static final Map<Integer, String> TYPE_NAMES = Map.of(
            1, "听力", 2, "阅读", 3, "翻译", 4, "写作"
    );

    @Override
    @Transactional
    public Map<String, Object> submitAnswer(AnswerSubmitDTO dto) {
        Long userId = SecurityUtils.getUserId();
        Cet6Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        boolean isCorrect = judgeAnswer(question, dto.getUserAnswer());
        int correctFlag = isCorrect ? 1 : 0;

        UserQuestionRecord record = new UserQuestionRecord();
        record.setUserId(userId);
        record.setQuestionId(dto.getQuestionId());
        record.setUserAnswer(dto.getUserAnswer());
        record.setIsCorrect(correctFlag);
        record.setAnswerTime(LocalDateTime.now());
        recordMapper.insert(record);

        if (!isCorrect) {
            UserErrorQuestion error = errorMapper.selectOne(new LambdaQueryWrapper<UserErrorQuestion>()
                    .eq(UserErrorQuestion::getUserId, userId)
                    .eq(UserErrorQuestion::getQuestionId, dto.getQuestionId()));
            if (error == null) {
                error = new UserErrorQuestion();
                error.setUserId(userId);
                error.setQuestionId(dto.getQuestionId());
                error.setErrorCount(1);
                error.setIsMaster(0);
                errorMapper.insert(error);
            } else {
                error.setErrorCount(error.getErrorCount() + 1);
                error.setIsMaster(0);
                errorMapper.updateById(error);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("standardAnswer", question.getStandardAnswer());
        result.put("analysis", question.getAnalysis());
        return result;
    }

    @Override
    public Map<String, Object> getWeakQuestionReport() {
        Long userId = SecurityUtils.getUserId();
        List<UserErrorQuestion> errors = errorMapper.selectList(new LambdaQueryWrapper<UserErrorQuestion>()
                .eq(UserErrorQuestion::getUserId, userId)
                .eq(UserErrorQuestion::getIsMaster, 0)
                .orderByDesc(UserErrorQuestion::getErrorCount));

        Map<Integer, Integer> typeCount = new HashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (UserErrorQuestion err : errors) {
            Cet6Question q = questionMapper.selectById(err.getQuestionId());
            if (q != null) {
                typeCount.merge(q.getQuestionType(), 1, Integer::sum);
                Map<String, Object> item = new HashMap<>();
                item.put("questionId", q.getId());
                item.put("title", q.getTitle().length() > 50 ? q.getTitle().substring(0, 50) + "..." : q.getTitle());
                item.put("questionType", q.getQuestionType());
                item.put("typeName", TYPE_NAMES.getOrDefault(q.getQuestionType(), "未知"));
                item.put("subType", q.getSubType());
                item.put("errorCount", err.getErrorCount());
                details.add(item);
            }
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        typeCount.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", TYPE_NAMES.getOrDefault(type, "未知"));
            item.put("value", count);
            chartData.add(item);
        });

        Map<String, Object> report = new HashMap<>();
        report.put("totalErrors", errors.size());
        report.put("chartData", chartData);
        report.put("details", details.stream().limit(20).toList());
        return report;
    }

    private boolean judgeAnswer(Cet6Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) {
            return false;
        }
        // 写作/翻译主观题不做自动判分
        if (question.getQuestionType() == 3 || question.getQuestionType() == 4) {
            return false;
        }
        String standard = question.getStandardAnswer().trim();
        String answer = userAnswer.trim();
        // 选择题：忽略大小写比较
        if (question.getOptions() != null && !question.getOptions().isBlank()) {
            return standard.equalsIgnoreCase(answer);
        }
        return standard.equalsIgnoreCase(answer);
    }
}
