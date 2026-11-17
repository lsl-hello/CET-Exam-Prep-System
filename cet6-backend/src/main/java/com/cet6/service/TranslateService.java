package com.cet6.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cet6.common.GlobalExceptionHandler.BusinessException;
import com.cet6.entity.Cet6Word;
import com.cet6.entity.TranslateRecord;
import com.cet6.mapper.Cet6WordMapper;
import com.cet6.mapper.TranslateRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中英互译服务。
 * 翻译方向由文本是否含中文自动判断；单词优先查本地六级词库（可返回词性 + 同根关联词），
 * 未命中或整句翻译时调用 MyMemory 免费接口，并把结果写入翻译记录。
 */
@Service
@RequiredArgsConstructor
public class TranslateService {

    private final Cet6WordMapper cet6WordMapper;
    private final TranslateRecordMapper recordMapper;

    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private static final int TIMEOUT_MILLIS = 10000;
    /** 免费接口单次请求上限约 500 字节，这里做长度保护 */
    private static final int MAX_LENGTH = 450;
    /** 关联词（同根词）最多返回条数 */
    private static final int RELATED_LIMIT = 5;
    /** 翻译记录最多保留展示条数 */
    private static final int RECORD_LIMIT = 50;
    /** 词根识别时剥离的后缀，长后缀在前，避免误截断 */
    private static final String[] STEM_SUFFIXES = {
            "ations", "ation", "ments", "ment", "ingly", "edly", "ings", "ing",
            "ions", "ion", "ies", "ed", "es", "ly", "est", "er", "s",
            "ness", "ity", "ance", "ence", "able", "ible", "ous", "ive", "al"
    };

    /**
     * 翻译并保存记录。
     *
     * @param text   原文
     * @param userId 当前用户id
     * @return result 译文 / speech 词性 / relatedWords 关联词 / record 保存的记录
     */
    public Map<String, Object> translate(String text, Long userId) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException("请输入需要翻译的内容");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new BusinessException("单次翻译内容过长，请控制在 " + MAX_LENGTH + " 个字符以内");
        }
        boolean hasChinese = containsChinese(trimmed);
        String sourceLang = hasChinese ? "zh-CN" : "en";
        String targetLang = hasChinese ? "en" : "zh-CN";

        String result;
        String speech = null;
        List<Map<String, Object>> relatedWords = new ArrayList<>();

        // 单个英文单词先查本地词库，命中则用词库释义并带出词性、同根关联词
        Cet6Word dictWord = (!hasChinese && isSingleWord(trimmed)) ? lookupWord(trimmed) : null;
        if (dictWord != null) {
            result = dictWord.getTranslate();
            speech = toNullIfBlank(dictWord.getSpeech());
            relatedWords = relatedWordsOf(dictWord.getWord());
        } else {
            result = requestTranslation(trimmed, sourceLang, targetLang);
        }

        TranslateRecord record = new TranslateRecord();
        record.setUserId(userId);
        record.setSourceText(trimmed);
        record.setResultText(result);
        record.setSpeech(speech);
        record.setSourceLang(sourceLang);
        record.setTargetLang(targetLang);
        recordMapper.insert(record);

        Map<String, Object> data = new HashMap<>();
        data.put("result", result);
        data.put("speech", speech);
        data.put("relatedWords", relatedWords);
        data.put("record", record);
        return data;
    }

    /** 当前用户的翻译记录，按时间倒序，最多 50 条 */
    public List<TranslateRecord> listRecords(Long userId) {
        return recordMapper.selectList(new LambdaQueryWrapper<TranslateRecord>()
                .eq(TranslateRecord::getUserId, userId)
                .orderByDesc(TranslateRecord::getCreateTime)
                .last("LIMIT " + RECORD_LIMIT));
    }

    /** 删除单条记录（仅限本人） */
    public void deleteRecord(Long id, Long userId) {
        recordMapper.delete(new LambdaQueryWrapper<TranslateRecord>()
                .eq(TranslateRecord::getId, id)
                .eq(TranslateRecord::getUserId, userId));
    }

    /** 清空当前用户的全部记录 */
    public void clearRecords(Long userId) {
        recordMapper.delete(new LambdaQueryWrapper<TranslateRecord>()
                .eq(TranslateRecord::getUserId, userId));
    }

    private boolean containsChinese(String text) {
        return text.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    /** 是否单个英文单词（不含空格，仅字母和连字符/撇号） */
    private boolean isSingleWord(String text) {
        return text.matches("[a-zA-Z][a-zA-Z'\\-]*");
    }

    private Cet6Word lookupWord(String word) {
        // utf8mb4_0900_ai_ci 为大小写不敏感排序规则，eq 即可忽略大小写
        return cet6WordMapper.selectOne(new LambdaQueryWrapper<Cet6Word>()
                .eq(Cet6Word::getStatus, 1)
                .eq(Cet6Word::getWord, word.toLowerCase())
                .last("LIMIT 1"));
    }

    /** 同根关联词：按剥离后缀得到的词根做前缀匹配 */
    private List<Map<String, Object>> relatedWordsOf(String word) {
        String lower = word.toLowerCase();
        String stem = stemOf(lower);
        if (stem.length() < 3) {
            return new ArrayList<>();
        }
        List<Cet6Word> list = cet6WordMapper.selectList(new LambdaQueryWrapper<Cet6Word>()
                .eq(Cet6Word::getStatus, 1)
                .likeRight(Cet6Word::getWord, stem)
                .ne(Cet6Word::getWord, lower)
                .orderByAsc(Cet6Word::getWord)
                .last("LIMIT " + RELATED_LIMIT));
        return list.stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("word", w.getWord());
            m.put("speech", w.getSpeech());
            m.put("translate", w.getTranslate());
            return m;
        }).collect(Collectors.toList());
    }

    private String stemOf(String word) {
        for (String suffix : STEM_SUFFIXES) {
            if (word.length() >= suffix.length() + 3 && word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }

    private String toNullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String requestTranslation(String text, String sourceLang, String targetLang) {
        String langPair = sourceLang + "|" + targetLang;
        String url = API_URL
                + "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&langpair=" + URLEncoder.encode(langPair, StandardCharsets.UTF_8);
        String body;
        try {
            body = HttpUtil.get(url, TIMEOUT_MILLIS);
        } catch (Exception e) {
            throw new BusinessException("翻译服务连接失败，请检查网络后重试");
        }
        try {
            JSONObject json = JSONUtil.parseObj(body);
            if (json.getInt("responseStatus", 0) != 200) {
                throw new BusinessException(json.getStr("responseDetails", "翻译服务暂时不可用"));
            }
            JSONObject data = json.getJSONObject("responseData");
            String translated = data == null ? null : data.getStr("translatedText");
            if (translated == null || translated.isEmpty()) {
                throw new BusinessException("翻译失败，请稍后重试");
            }
            if (translated.startsWith("MYMEMORY WARNING")) {
                throw new BusinessException("免费翻译额度已用完，请稍后再试");
            }
            return translated;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("翻译服务返回异常，请稍后重试");
        }
    }
}
