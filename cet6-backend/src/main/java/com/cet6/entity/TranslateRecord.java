package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cet6_translate_record")
public class TranslateRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 原文 */
    private String sourceText;
    /** 译文 */
    private String resultText;
    /** 词性（单词命中词库时才有，如 adj.） */
    private String speech;
    /** 源语言 zh-CN / en */
    private String sourceLang;
    /** 目标语言 zh-CN / en */
    private String targetLang;
}
