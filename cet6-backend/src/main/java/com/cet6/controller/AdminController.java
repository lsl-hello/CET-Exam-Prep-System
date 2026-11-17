package com.cet6.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cet6.common.PageQuery;
import com.cet6.common.PageResult;
import com.cet6.common.Result;
import com.cet6.entity.*;
import com.cet6.mapper.*;
import com.cet6.security.SecurityUtils;
import com.cet6.util.ExcelImportUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper userMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysOperLogMapper operLogMapper;
    private final Cet6WordBookMapper wordBookMapper;
    private final Cet6WordMapper wordMapper;
    private final Cet6PaperMapper paperMapper;
    private final Cet6QuestionMapper questionMapper;
    private final UserQuestionRecordMapper recordMapper;
    private final UserWordRecordMapper wordRecordMapper;
    private final PasswordEncoder passwordEncoder;

    private void logOper(String type, String desc, HttpServletRequest req) {
        SysOperLog log = new SysOperLog();
        log.setAdminId(SecurityUtils.getUserId());
        log.setOperateType(type);
        log.setOperateDesc(desc);
        log.setIpAddr(req.getRemoteAddr());
        operLogMapper.insert(log);
    }

    // ===== 用户管理 =====
    @GetMapping("/user/list")
    public Result<?> userList(PageQuery query) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            w.like(SysUser::getUsername, query.getKeyword())
                    .or().like(SysUser::getNickname, query.getKeyword());
        }
        Page<SysUser> page = userMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), w);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/user")
    public Result<Void> saveUser(@RequestBody SysUser user, HttpServletRequest req) {
        if (user.getId() == null) {
            // 新增用户：默认密码或管理员给的初始密码，都强制首次登录改密
            user.setPassword(passwordEncoder.encode(user.getPassword() != null ? user.getPassword() : "123456"));
            user.setForceChange(1);
            userMapper.insert(user);
            logOper("新增", "新增用户 " + user.getUsername(), req);
        } else {
            if (user.getPassword() != null && !user.getPassword().isBlank()) {
                // 管理员重置密码：加密保存，并强制该用户下次登录改密
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                user.setForceChange(1);
            } else {
                // 不动密码，也不能让 MyBatis-Plus 把 password 字段 update 成 null
                user.setPassword(null);
                // forceChange 不主动设置，避免覆盖既有值（前端没传时 updateById 会跳过 null 字段）
            }
            userMapper.updateById(user);
            logOper("编辑", "编辑用户 " + user.getUsername(), req);
        }
        return Result.success();
    }

    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest req) {
        userMapper.deleteById(id);
        logOper("删除", "删除用户 id=" + id, req);
        return Result.success();
    }

    // ===== 公告管理 =====
    @GetMapping("/notice/list")
    public Result<?> noticeList(PageQuery query) {
        Page<SysNotice> page = noticeMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysNotice>().orderByDesc(SysNotice::getSort));
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/notice")
    public Result<Void> saveNotice(@RequestBody SysNotice notice, HttpServletRequest req) {
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
            logOper("新增", "新增公告 " + notice.getTitle(), req);
        } else {
            noticeMapper.updateById(notice);
            logOper("编辑", "编辑公告 " + notice.getTitle(), req);
        }
        return Result.success();
    }

    @DeleteMapping("/notice/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id, HttpServletRequest req) {
        noticeMapper.deleteById(id);
        logOper("删除", "删除公告 id=" + id, req);
        return Result.success();
    }

    // ===== 词书管理 =====
    @GetMapping("/wordbook/list")
    public Result<?> wordBookList(PageQuery query, @RequestParam(required = false) Integer level) {
        Page<Cet6WordBook> page = wordBookMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<Cet6WordBook>()
                        .eq(level != null, Cet6WordBook::getLevel, level)
                        .orderByAsc(Cet6WordBook::getSort));
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/wordbook")
    public Result<Void> saveWordBook(@RequestBody Cet6WordBook book, HttpServletRequest req) {
        if (book.getId() == null) {
            wordBookMapper.insert(book);
        } else {
            wordBookMapper.updateById(book);
        }
        logOper("编辑", "编辑词书 " + book.getBookName(), req);
        return Result.success();
    }

    @DeleteMapping("/wordbook/{id}")
    public Result<Void> deleteWordBook(@PathVariable Long id, HttpServletRequest req) {
        wordBookMapper.deleteById(id);
        logOper("删除", "删除词书 id=" + id, req);
        return Result.success();
    }

    // ===== 单词管理 =====
    @GetMapping("/word/list")
    public Result<?> wordList(PageQuery query, @RequestParam(required = false) Long bookId) {
        LambdaQueryWrapper<Cet6Word> w = new LambdaQueryWrapper<>();
        if (bookId != null) w.eq(Cet6Word::getBookId, bookId);
        if (query.getKeyword() != null) w.like(Cet6Word::getWord, query.getKeyword());
        Page<Cet6Word> page = wordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), w);
        // 补 bookName 字段：批量查相关 bookId，避免 N+1 查询
        List<Cet6Word> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> bookIds = records.stream()
                    .map(Cet6Word::getBookId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!bookIds.isEmpty()) {
                Map<Long, String> bookNameMap = wordBookMapper.selectBatchIds(bookIds).stream()
                        .collect(Collectors.toMap(Cet6WordBook::getId, Cet6WordBook::getBookName));
                records.forEach(word -> word.setBookName(bookNameMap.get(word.getBookId())));
            }
        }
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/word")
    public Result<Void> saveWord(@RequestBody Cet6Word word, HttpServletRequest req) {
        if (word.getId() == null) {
            wordMapper.insert(word);
        } else {
            wordMapper.updateById(word);
        }
        logOper("编辑", "编辑单词 " + word.getWord(), req);
        return Result.success();
    }

    @DeleteMapping("/word/{id}")
    public Result<Void> deleteWord(@PathVariable Long id, HttpServletRequest req) {
        wordMapper.deleteById(id);
        logOper("删除", "删除单词 id=" + id, req);
        return Result.success();
    }

    @PostMapping("/word/import")
    public Result<Map<String, Object>> importWords(@RequestParam MultipartFile file,
                                                    @RequestParam Long bookId,
                                                    HttpServletRequest req) {
        List<Cet6Word> words = ExcelImportUtil.parseWords(file, bookId);
        words.forEach(wordMapper::insert);
        logOper("导入导出", "批量导入单词 " + words.size() + " 条", req);
        return Result.success(Map.of("count", words.size()));
    }

    /**
     * 批量文本导入单词：从 PDF / Excel / 任意文本复制粘贴进来，按行解析
     * 支持两种格式：
     *   1. 显式分隔符：单词|音标|词性|释义|例句（分隔符：| \t , ，）
     *   2. PDF 复制格式：peril [ˈperəl] n 严重危险（空格分隔，[音标]用方括号包裹）
     * 字段顺序：单词 | 音标 | 词性 | 释义 | 例句（例句可省略，至少要有单词）
     */
    @PostMapping("/word/batch-text")
    public Result<Map<String, Object>> batchTextWords(@RequestBody @jakarta.validation.Valid com.cet6.dto.BatchWordTextDTO dto,
                                                       HttpServletRequest req) {
        int success = 0, fail = 0;
        StringBuilder failMsg = new StringBuilder();
        String[] lines = dto.getText().split("\\r?\\n");
        int sort = 1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            try {
                Cet6Word word = parseLine(trimmed);
                if (word == null || word.getWord() == null || word.getWord().isBlank()) {
                    fail++;
                    continue;
                }
                word.setBookId(dto.getBookId());
                word.setSort(sort++);
                word.setStatus(1);
                wordMapper.insert(word);
                success++;
            } catch (Exception e) {
                fail++;
                if (failMsg.length() == 0) failMsg.append("第 ").append(sort).append(" 行：").append(e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("failMsg", failMsg.toString());
        logOper("导入导出", "批量文本导入单词 成功" + success + " 失败" + fail, req);
        return Result.success(result);
    }

    /**
     * 智能解析一行单词文本，兼容两种格式：
     *   1. 显式分隔符格式：ambitious|/æmˈbɪʃəs/|adj.|有雄心的；有抱负的|He is ambitious.
     *   2. PDF 复制格式：peril [ˈperəl] n 严重危险
     *      或：account [əˈkaʊnt] n 账户；描述 v 认为是
     * 解析策略：
     *   - 先用方括号 [...] 提取音标，从原行中移除
     *   - 剩余部分先尝试 | \t , ， 分隔
     *   - 如果第一段还包含空格（说明是 PDF 复制格式），再按空格切分：单词 词性 释义
     */
    private Cet6Word parseLine(String line) {
        Cet6Word word = new Cet6Word();
        String remaining = line;
        // 1. 提取 [音标]
        int bStart = remaining.indexOf('[');
        int bEnd = remaining.indexOf(']');
        if (bStart >= 0 && bEnd > bStart) {
            word.setPhonetic(remaining.substring(bStart + 1, bEnd).trim());
            remaining = (remaining.substring(0, bStart) + " " + remaining.substring(bEnd + 1)).trim();
        }
        // 2. 尝试 | \t , ， 分隔
        String[] parts = remaining.split("[|\\t,，]", 5);
        if (parts.length >= 2 && !parts[0].contains(" ")) {
            // 显式分隔符格式
            word.setWord(parts[0].trim());
            if (word.getPhonetic() == null || word.getPhonetic().isEmpty()) {
                word.setPhonetic(parts.length > 1 ? parts[1].trim() : "");
            }
            word.setSpeech(parts.length > 2 ? parts[2].trim() : "");
            word.setTranslate(parts.length > 3 ? parts[3].trim() : "");
            word.setSentence(parts.length > 4 ? parts[4].trim() : "");
        } else {
            // PDF 复制格式：单词 词性 释义（释义里可能也含空格，比如 "n 账户；描述 v 认为是"）
            // 用 \s+ 切分，最多 3 段：单词、词性、释义
            String[] sub = parts[0].split("\\s+", 3);
            if (sub.length == 0 || sub[0].isEmpty()) return null;
            word.setWord(sub[0]);
            // 词性通常是 1-3 个字母（n/v/a/adj/vt/vi/n&v 等），如果在第二段就当作词性
            if (sub.length > 1 && sub[1].length() <= 5 && sub[1].matches("[a-zA-Z&]+\\.?")) {
                word.setSpeech(sub[1]);
                word.setTranslate(sub.length > 2 ? sub[2] : "");
            } else if (sub.length > 1) {
                // 第二段不是词性（比如 "v. account"），把第二段当作释义开头
                word.setSpeech("");
                word.setTranslate(sub.length > 1 ? java.util.Arrays.stream(sub, 1, sub.length).reduce("", (a, b) -> a + " " + b).trim() : "");
            }
        }
        return word;
    }

    // ===== 试卷管理 =====
    @GetMapping("/paper/list")
    public Result<?> paperList(PageQuery query, @RequestParam(required = false) Integer level) {
        Page<Cet6Paper> page = paperMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<Cet6Paper>()
                        .eq(level != null, Cet6Paper::getLevel, level)
                        .orderByDesc(Cet6Paper::getPaperYear));
        // 富集：统计每张试卷下的试题数（总题数自动统计）
        List<Cet6Paper> papers = page.getRecords();
        if (papers != null && !papers.isEmpty()) {
            Set<Long> paperIds = papers.stream().map(Cet6Paper::getId).collect(Collectors.toSet());
            // 按 paper_id 分组统计题目数量
            List<Cet6Question> allQ = questionMapper.selectList(
                    new LambdaQueryWrapper<Cet6Question>().in(Cet6Question::getPaperId, paperIds));
            Map<Long, Long> countMap = allQ.stream()
                    .collect(Collectors.groupingBy(Cet6Question::getPaperId, Collectors.counting()));
            papers.forEach(p -> p.setQuestionCount(countMap.getOrDefault(p.getId(), 0L).intValue()));
        }
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/paper")
    public Result<Void> savePaper(@RequestBody Cet6Paper paper, HttpServletRequest req) {
        if (paper.getId() == null) {
            paperMapper.insert(paper);
        } else {
            paperMapper.updateById(paper);
        }
        logOper("编辑", "编辑试卷 " + paper.getPaperName(), req);
        return Result.success();
    }

    @DeleteMapping("/paper/{id}")
    public Result<Void> deletePaper(@PathVariable Long id, HttpServletRequest req) {
        paperMapper.deleteById(id);
        logOper("删除", "删除试卷 id=" + id, req);
        return Result.success();
    }

    // ===== 试题管理 =====
    @GetMapping("/question/list")
    public Result<?> questionList(PageQuery query,
                                  @RequestParam(required = false) Long paperId,
                                  @RequestParam(required = false) Integer questionType) {
        LambdaQueryWrapper<Cet6Question> w = new LambdaQueryWrapper<>();
        if (paperId != null) w.eq(Cet6Question::getPaperId, paperId);
        if (questionType != null) w.eq(Cet6Question::getQuestionType, questionType);
        if (query.getKeyword() != null) w.like(Cet6Question::getTitle, query.getKeyword());
        Page<Cet6Question> page = questionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), w);
        // 补 paperName：批量查相关 paperId，避免 N+1
        List<Cet6Question> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> paperIds = records.stream()
                    .map(Cet6Question::getPaperId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!paperIds.isEmpty()) {
                Map<Long, String> paperNameMap = paperMapper.selectBatchIds(paperIds).stream()
                        .collect(Collectors.toMap(Cet6Paper::getId, Cet6Paper::getPaperName));
                records.forEach(q -> q.setPaperName(paperNameMap.get(q.getPaperId())));
            }
        }
        return Result.success(PageResult.of(page));
    }

    @PostMapping("/question")
    public Result<Void> saveQuestion(@RequestBody Cet6Question question, HttpServletRequest req) {
        if (question.getId() == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
        }
        logOper("编辑", "编辑试题 id=" + question.getId(), req);
        return Result.success();
    }

    @DeleteMapping("/question/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id, HttpServletRequest req) {
        questionMapper.deleteById(id);
        logOper("删除", "删除试题 id=" + id, req);
        return Result.success();
    }

    @PostMapping("/question/import")
    public Result<Map<String, Object>> importQuestions(@RequestParam MultipartFile file,
                                                         @RequestParam Long paperId,
                                                         HttpServletRequest req) {
        List<Cet6Question> questions = ExcelImportUtil.parseQuestions(file, paperId);
        questions.forEach(questionMapper::insert);
        logOper("导入导出", "批量导入试题 " + questions.size() + " 道", req);
        return Result.success(Map.of("count", questions.size()));
    }

    // ===== 全局统计 =====
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("wordCount", wordMapper.selectCount(null));
        data.put("questionCount", questionMapper.selectCount(null));
        data.put("paperCount", paperMapper.selectCount(null));
        data.put("todayQuestionRecords", recordMapper.selectCount(new LambdaQueryWrapper<UserQuestionRecord>()
                .ge(UserQuestionRecord::getAnswerTime, java.time.LocalDate.now().atStartOfDay())));
        data.put("todayWordRecords", wordRecordMapper.selectCount(new LambdaQueryWrapper<UserWordRecord>()
                .eq(UserWordRecord::getReviewDate, java.time.LocalDate.now())));
        return Result.success(data);
    }

    // ===== 操作日志 =====
    @GetMapping("/log/list")
    public Result<?> logList(PageQuery query) {
        Page<SysOperLog> page = operLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysOperLog>().orderByDesc(SysOperLog::getCreateTime));
        return Result.success(PageResult.of(page));
    }
}
