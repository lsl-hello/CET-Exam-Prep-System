package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cet6_word_book")
public class Cet6WordBook extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bookName;
    private String bookDesc;
    /** 考试等级：4=大学英语四级，6=大学英语六级 */
    private Integer level;
    private Integer sort;
    private Integer status;
}
