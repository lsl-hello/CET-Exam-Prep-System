package com.cet6.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cet6.dto.WordReviewDTO;
import com.cet6.entity.Cet6Word;

import java.util.List;
import java.util.Map;

public interface WordStudyService extends IService<Cet6Word> {

    /** 获取今日待背/复习单词 */
    List<Map<String, Object>> getTodayWords(Long bookId, int limit);

    /** 提交背词结果（艾宾浩斯） */
    void submitReview(WordReviewDTO dto);

    /** 生词薄弱分析 */
    Map<String, Object> getWeakWordReport();

    /** 单词掌握统计：熟练/模糊/遗忘数量 */
    Map<String, Object> getWordStat();
}
