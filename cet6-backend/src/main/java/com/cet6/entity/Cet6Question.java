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
@TableName("cet6_question")
public class Cet6Question extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    /** 1听力 2阅读 3翻译 4写作 */
    private Integer questionType;
    /** 101-103听力 201-203阅读 301翻译 401写作 */
    private Integer subType;
    private Integer difficult;
    private String title;
    private String audioUrl;
    /** JSON数组 */
    private String options;
    private String standardAnswer;
    private String analysis;
    private Integer sort;
    private Integer status;

    /** 非持久化：试题列表展示所属试卷名用 */
    @TableField(exist = false)
    private String paperName;
}
