package com.cet6.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cet6.common.PageQuery;
import com.cet6.common.PageResult;
import com.cet6.common.Result;
import com.cet6.dto.AnswerSubmitDTO;
import com.cet6.dto.WordReviewDTO;
import com.cet6.entity.*;
import com.cet6.mapper.*;
import com.cet6.security.SecurityUtils;
import com.cet6.service.QuestionStudyService;
import com.cet6.service.TranslateService;
import com.cet6.service.WordStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final WordStudyService wordStudyService;
    private final QuestionStudyService questionStudyService;
    private final Cet6WordBookMapper wordBookMapper;
    private final Cet6WordMapper wordMapper;
    private final Cet6PaperMapper paperMapper;
    private final Cet6QuestionMapper questionMapper;
    private final UserErrorQuestionMapper errorMapper;
    private final UserQuestionRecordMapper recordMapper;
    private final UserWordMapper userWordMapper;
    private final WrongWordMapper wrongWordMapper;
    private final UserStudyPlanMapper planMapper;
    private final UserMessageMapper messageMapper;
    private final TranslateService translateService;

    // ===== 背单词 =====
    @GetMapping("/word/books")
    public Result<?> wordBooks(@RequestParam(required = false) Integer level) {
        return Result.success(wordBookMapper.selectList(new LambdaQueryWrapper<Cet6WordBook>()
                .eq(Cet6WordBook::getStatus, 1)
                .eq(level != null, Cet6WordBook::getLevel, level)
                .orderByAsc(Cet6WordBook::getSort)));
    }

    @GetMapping("/word/today")
    public Result<?> todayWords(@RequestParam(required = false) Long bookId,
                                @RequestParam(defaultValue = "30") int limit) {
        return Result.success(wordStudyService.getTodayWords(bookId, limit));
    }

    /**
     * 查询单词详情（用于错题本"重新背诵"跳转后插入今日列表）
     */
    @GetMapping("/word/detail/{wordId}")
    public Result<?> wordDetail(@PathVariable Long wordId) {
        return Result.success(wordMapper.selectById(wordId));
    }

    @PostMapping("/word/review")
    public Result<Void> reviewWord(@RequestBody WordReviewDTO dto) {
        wordStudyService.submitReview(dto);
        return Result.success();
    }

    @GetMapping("/word/my")
    public Result<?> myWords(PageQuery query) {
        Long userId = SecurityUtils.getUserId();
        Page<UserWord> page = userWordMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<UserWord>().eq(UserWord::getUserId, userId));
        return Result.success(PageResult.of(page));
    }

    @GetMapping("/word/weak-report")
    public Result<?> weakWordReport() {
        return Result.success(wordStudyService.getWeakWordReport());
    }

    @GetMapping("/word/stat")
    public Result<?> wordStat() {
        return Result.success(wordStudyService.getWordStat());
    }

    /**
     * 学习计划信息：用于背单词页选择每日词数 + 显示预计天数
     * - 如果用户有 plan_type=1 + status=1 的计划 → dailyLimit = plan.dailyTargetNum
     * - 没有计划 → dailyLimit = 30（默认）
     * - totalWords = cet6_word 表 status=1 的总数
     * - estimatedDays = ceil(totalWords / dailyLimit)
     */
    @GetMapping("/word/plan-info")
    public Result<?> wordPlanInfo() {
        Long userId = SecurityUtils.getUserId();
        UserStudyPlan plan = planMapper.selectOne(new LambdaQueryWrapper<UserStudyPlan>()
                .eq(UserStudyPlan::getUserId, userId)
                .eq(UserStudyPlan::getPlanType, 1)
                .eq(UserStudyPlan::getStatus, 1)
                .last("LIMIT 1"));
        long totalWords = wordMapper.selectCount(new LambdaQueryWrapper<Cet6Word>()
                .eq(Cet6Word::getStatus, 1));
        int dailyLimit = plan != null && plan.getDailyTargetNum() != null && plan.getDailyTargetNum() > 0
                ? plan.getDailyTargetNum() : 30;
        int estimatedDays = (int) Math.ceil((double) totalWords / dailyLimit);
        // 查关联的单词书名字，用于背单词页面展示
        String bookName = null;
        if (plan != null && plan.getBookId() != null) {
            Cet6WordBook book = wordBookMapper.selectById(plan.getBookId());
            if (book != null) bookName = book.getBookName();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalWords", totalWords);
        result.put("dailyLimit", dailyLimit);
        result.put("estimatedDays", estimatedDays);
        result.put("hasPlan", plan != null);
        result.put("planName", plan != null ? plan.getPlanName() : null);
        result.put("bookName", bookName);
        return Result.success(result);
    }

    // ===== 单词错题本 =====
    /**
     * 加入错题本（幂等：同一 user_id + word_id 已存在则只更新 reason）
     * @param wordId 单词id
     * @param reason vague / forget / manual，默认 manual
     */
    @PostMapping("/word/wrong/{wordId}")
    public Result<Void> addWrongWord(@PathVariable Long wordId,
                                     @RequestParam(defaultValue = "manual") String reason) {
        Long userId = SecurityUtils.getUserId();
        // 用 INSERT ON DUPLICATE KEY UPDATE 做幂等：记录存在（含历史软删除）就更新 reason + 恢复，
        // 不存在才插入，彻底避免唯一索引 uk_user_word 冲突
        wrongWordMapper.upsertWrongWord(userId, wordId, reason);
        return Result.success();
    }

    /**
     * 检查某单词是否已在错题本（用于背单词页星星按钮初始高亮状态）
     */
    @GetMapping("/word/wrong/check/{wordId}")
    public Result<Map<String, Object>> checkWrongWord(@PathVariable Long wordId) {
        Long userId = SecurityUtils.getUserId();
        WrongWord w = wrongWordMapper.selectOne(new LambdaQueryWrapper<WrongWord>()
                .eq(WrongWord::getUserId, userId)
                .eq(WrongWord::getWordId, wordId));
        Map<String, Object> result = new HashMap<>();
        result.put("inWrong", w != null);
        result.put("id", w != null ? w.getId() : null);
        return Result.success(result);
    }

    /**
     * 按 wordId 从错题本移除（背单词页星星按钮 toggle 用，前端只知道 wordId）
     */
    @DeleteMapping("/word/wrong/by-word/{wordId}")
    public Result<Void> removeWrongWordByWordId(@PathVariable Long wordId) {
        // 物理删除（绕过逻辑删除），避免软删除记录残留导致下次再加入时唯一索引冲突
        wrongWordMapper.physicalDeleteWrongWord(SecurityUtils.getUserId(), wordId);
        return Result.success();
    }

    /**
     * 错题本列表（带单词详情，按加入时间倒序）
     * @param reason 可选过滤：vague / forget / manual，不传则返回全部
     */
    @GetMapping("/word/wrong")
    public Result<?> wrongWordList(PageQuery query,
                                   @RequestParam(required = false) String reason) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<WrongWord> wrapper = new LambdaQueryWrapper<WrongWord>()
                .eq(WrongWord::getUserId, userId)
                .orderByDesc(WrongWord::getCreateTime);
        if (reason != null && !reason.isEmpty()) {
            wrapper.eq(WrongWord::getReason, reason);
        }
        Page<WrongWord> page = wrongWordMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        // 附加单词详情
        List<Map<String, Object>> records = page.getRecords().stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getId());
            m.put("wordId", w.getWordId());
            m.put("reason", w.getReason());
            m.put("createTime", w.getCreateTime());
            Cet6Word word = wordMapper.selectById(w.getWordId());
            m.put("word", word != null ? word.getWord() : null);
            m.put("phonetic", word != null ? word.getPhonetic() : null);
            m.put("speech", word != null ? word.getSpeech() : null);
            m.put("translate", word != null ? word.getTranslate() : null);
            m.put("audioUrl", word != null ? word.getAudioUrl() : null);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", page.getTotal());
        return Result.success(result);
    }

    /**
     * 从错题本移除
     */
    @DeleteMapping("/word/wrong/{id}")
    public Result<Void> removeWrongWord(@PathVariable Long id) {
        WrongWord w = wrongWordMapper.selectById(id);
        if (w != null && w.getUserId().equals(SecurityUtils.getUserId())) {
            wrongWordMapper.deleteById(id);
        }
        return Result.success();
    }

    // ===== 刷题 =====
    @GetMapping("/paper/list")
    public Result<?> paperList(@RequestParam(required = false) Integer level) {
        List<Cet6Paper> papers = paperMapper.selectList(
                new LambdaQueryWrapper<Cet6Paper>()
                        .eq(Cet6Paper::getStatus, 1)
                        .eq(level != null, Cet6Paper::getLevel, level)
                        .orderByDesc(Cet6Paper::getPaperYear)
        );

        // 为每个试卷附加题目ID列表
        List<Map<String, Object>> result = papers.stream().map(paper -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", paper.getId());
            map.put("paperName", paper.getPaperName());
            map.put("paperYear", paper.getPaperYear());
            map.put("level", paper.getLevel());
            map.put("status", paper.getStatus());

            // 查询该试卷下的所有题目ID
            List<Cet6Question> questions = questionMapper.selectList(
                    new LambdaQueryWrapper<Cet6Question>()
                            .eq(Cet6Question::getPaperId, paper.getId())
                            .eq(Cet6Question::getStatus, 1)
                            .orderByAsc(Cet6Question::getSort)
            );

            List<Long> questionIds = questions.stream()
                    .map(Cet6Question::getId)
                    .collect(Collectors.toList());

            map.put("questionIds", questionIds);
            map.put("totalCount", questionIds.size());  // 直接使用查询到的数量

            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/question/list")
    public Result<?> questionList(PageQuery query,
                                  @RequestParam(required = false) Long paperId,
                                  @RequestParam(required = false) Integer questionType) {
        LambdaQueryWrapper<Cet6Question> w = new LambdaQueryWrapper<Cet6Question>()
                .eq(Cet6Question::getStatus, 1);
        if (paperId != null) w.eq(Cet6Question::getPaperId, paperId);
        if (questionType != null) w.eq(Cet6Question::getQuestionType, questionType);
        if (query.getKeyword() != null) w.like(Cet6Question::getTitle, query.getKeyword());
        w.orderByAsc(Cet6Question::getSort);
        Page<Cet6Question> page = questionMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), w);
        return Result.success(PageResult.of(page));
    }

    @GetMapping("/question/{id}")
    public Result<?> questionDetail(@PathVariable Long id) {
        Cet6Question q = questionMapper.selectById(id);
        if (q != null) {
            q.setStandardAnswer(null);
            q.setAnalysis(null);
        }
        return Result.success(q);
    }

    /**
     * 查看答案解析：返回标准答案 + 解析（用于"查看答案解析"按钮，主观题/未作答客观题直接展示）
     */
    @GetMapping("/question/reveal/{id}")
    public Result<Map<String, Object>> revealQuestion(@PathVariable Long id) {
        Cet6Question q = questionMapper.selectById(id);
        Map<String, Object> data = new HashMap<>();
        if (q != null) {
            data.put("id", q.getId());
            data.put("questionType", q.getQuestionType());
            data.put("title", q.getTitle());
            data.put("options", q.getOptions());
            data.put("audioUrl", q.getAudioUrl());
            data.put("standardAnswer", q.getStandardAnswer());
            data.put("analysis", q.getAnalysis());
        }
        return Result.success(data);
    }

    @PostMapping("/question/submit")
    public Result<Map<String, Object>> submitAnswer(@RequestBody AnswerSubmitDTO dto) {
        return Result.success(questionStudyService.submitAnswer(dto));
    }

    @GetMapping("/error/list")
    public Result<?> errorList(PageQuery query) {
        Long userId = SecurityUtils.getUserId();
        Page<UserErrorQuestion> page = errorMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<UserErrorQuestion>()
                        .eq(UserErrorQuestion::getUserId, userId)
                        .eq(UserErrorQuestion::getIsMaster, 0)
                        .orderByDesc(UserErrorQuestion::getErrorCount));
        // 富集：题目详情 + 最近一次作答，避免前端表格只看到错题记录本身
        List<UserErrorQuestion> errors = page.getRecords();
        List<Map<String, Object>> records = new java.util.ArrayList<>();
        if (errors != null && !errors.isEmpty()) {
            Set<Long> qIds = errors.stream().map(UserErrorQuestion::getQuestionId).collect(Collectors.toSet());
            // 批量查题目
            Map<Long, Cet6Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                    .collect(Collectors.toMap(Cet6Question::getId, q -> q));
            // 批量查该用户相关作答记录，按 questionId 分组取最近一次
            List<UserQuestionRecord> allRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<UserQuestionRecord>()
                            .eq(UserQuestionRecord::getUserId, userId)
                            .in(UserQuestionRecord::getQuestionId, qIds)
                            .orderByDesc(UserQuestionRecord::getAnswerTime));
            Map<Long, UserQuestionRecord> latestByQ = new HashMap<>();
            for (UserQuestionRecord r : allRecords) {
                latestByQ.putIfAbsent(r.getQuestionId(), r);
            }
            for (UserErrorQuestion err : errors) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", err.getId());
                m.put("questionId", err.getQuestionId());
                m.put("errorCount", err.getErrorCount());
                m.put("isMaster", err.getIsMaster());
                Cet6Question q = qMap.get(err.getQuestionId());
                if (q != null) {
                    m.put("title", q.getTitle());
                    m.put("questionType", q.getQuestionType());
                    m.put("typeName", typeName(q.getQuestionType()));
                    m.put("subType", q.getSubType());
                    m.put("standardAnswer", q.getStandardAnswer());
                    m.put("analysis", q.getAnalysis());
                    m.put("options", q.getOptions());
                    m.put("audioUrl", q.getAudioUrl());
                }
                UserQuestionRecord latest = latestByQ.get(err.getQuestionId());
                m.put("userAnswer", latest != null ? latest.getUserAnswer() : null);
                records.add(m);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        return Result.success(data);
    }

    private String typeName(Integer t) {
        if (t == null) return "未知";
        return switch (t) { case 1 -> "听力"; case 2 -> "阅读"; case 3 -> "翻译"; case 4 -> "写作"; default -> "未知"; };
    }

    @PutMapping("/error/master/{id}")
    public Result<Void> markMaster(@PathVariable Long id) {
        UserErrorQuestion err = errorMapper.selectById(id);
        if (err != null && err.getUserId().equals(SecurityUtils.getUserId())) {
            err.setIsMaster(1);
            errorMapper.updateById(err);
        }
        return Result.success();
    }

    /**
     * 收藏题目到错题本（刷题页收藏键）：幂等，已存在则不重复插入。
     * 用于学生主动收藏，区别于答错自动加入。
     */
    @PostMapping("/error/favorite/{questionId}")
    public Result<Void> favoriteQuestion(@PathVariable Long questionId) {
        Long userId = SecurityUtils.getUserId();
        UserErrorQuestion exist = errorMapper.selectOne(new LambdaQueryWrapper<UserErrorQuestion>()
                .eq(UserErrorQuestion::getUserId, userId)
                .eq(UserErrorQuestion::getQuestionId, questionId));
        if (exist == null) {
            UserErrorQuestion err = new UserErrorQuestion();
            err.setUserId(userId);
            err.setQuestionId(questionId);
            err.setErrorCount(0);
            err.setIsMaster(0);
            errorMapper.insert(err);
        } else if (exist.getIsMaster() != null && exist.getIsMaster() == 1) {
            // 已掌握的重新收藏：取消掌握标记
            exist.setIsMaster(0);
            errorMapper.updateById(exist);
        }
        return Result.success();
    }

    /**
     * 查询当前用户错题本中所有题目ID（刷题页判断收藏键是否点亮用）
     */
    @GetMapping("/error/ids")
    public Result<List<Long>> errorQuestionIds() {
        Long userId = SecurityUtils.getUserId();
        List<UserErrorQuestion> list = errorMapper.selectList(new LambdaQueryWrapper<UserErrorQuestion>()
                .eq(UserErrorQuestion::getUserId, userId)
                .eq(UserErrorQuestion::getIsMaster, 0));
        List<Long> ids = list.stream().map(UserErrorQuestion::getQuestionId).collect(Collectors.toList());
        return Result.success(ids);
    }

    @GetMapping("/question/weak-report")
    public Result<?> weakQuestionReport() {
        return Result.success(questionStudyService.getWeakQuestionReport());
    }

    // ===== 学习计划 =====
    @GetMapping("/plan/list")
    public Result<?> planList() {
        return Result.success(planMapper.selectList(new LambdaQueryWrapper<UserStudyPlan>()
                .eq(UserStudyPlan::getUserId, SecurityUtils.getUserId())));
    }

    @PostMapping("/plan")
    public Result<Void> savePlan(@RequestBody UserStudyPlan plan) {
        plan.setUserId(SecurityUtils.getUserId());
        if (plan.getId() != null) {
            planMapper.updateById(plan);
        } else {
            planMapper.insert(plan);
        }
        return Result.success();
    }

    @DeleteMapping("/plan/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planMapper.delete(new LambdaQueryWrapper<UserStudyPlan>()
                .eq(UserStudyPlan::getId, id)
                .eq(UserStudyPlan::getUserId, SecurityUtils.getUserId()));
        return Result.success();
    }

    // ===== 消息 =====
    @GetMapping("/message/list")
    public Result<?> messageList(PageQuery query) {
        Page<UserMessage> page = messageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<UserMessage>()
                        .eq(UserMessage::getUserId, SecurityUtils.getUserId())
                        .orderByDesc(UserMessage::getCreateTime));
        return Result.success(PageResult.of(page));
    }

    @PutMapping("/message/read/{id}")
    public Result<Void> readMessage(@PathVariable Long id) {
        UserMessage msg = messageMapper.selectById(id);
        if (msg != null && msg.getUserId().equals(SecurityUtils.getUserId())) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }
        return Result.success();
    }

    // ===== 中英互译 =====
    /**
     * 中英互译：输入中文译英文，输入英文译中文。
     * 方向由文本是否包含中文字符自动判断，无需前端指定；结果同时写入翻译记录。
     */
    @PostMapping("/translate")
    public Result<Map<String, Object>> translate(@RequestBody Map<String, String> body) {
        return Result.success(translateService.translate(body.get("text"), SecurityUtils.getUserId()));
    }

    /** 翻译记录列表（最近 50 条，按时间倒序） */
    @GetMapping("/translate/records")
    public Result<List<TranslateRecord>> translateRecords() {
        return Result.success(translateService.listRecords(SecurityUtils.getUserId()));
    }

    /** 删除单条翻译记录 */
    @DeleteMapping("/translate/records/{id}")
    public Result<Void> deleteTranslateRecord(@PathVariable Long id) {
        translateService.deleteRecord(id, SecurityUtils.getUserId());
        return Result.success();
    }

    /** 清空当前用户的全部翻译记录 */
    @DeleteMapping("/translate/records")
    public Result<Void> clearTranslateRecords() {
        translateService.clearRecords(SecurityUtils.getUserId());
        return Result.success();
    }
}
