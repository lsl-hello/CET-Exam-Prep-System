package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cet6_word")
public class Cet6Word extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private String word;
    private String phonetic;
    private String speech;
    private String translate;
    private String sentence;
    private String audioUrl;
    private Integer sort;
    private Integer status;

    /** 非数据库字段：单词本名称，仅用于管理后台列表展示（join 查询或 service 层补值） */
    @TableField(exist = false)
    private String bookName;
}
