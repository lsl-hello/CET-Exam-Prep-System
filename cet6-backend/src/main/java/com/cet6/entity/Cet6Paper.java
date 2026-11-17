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
@TableName("cet6_paper")
public class Cet6Paper extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String paperName;
    private Integer paperYear;
    private Integer paperMonth;
    /** 考试等级：4=大学英语四级，6=大学英语六级 */
    private Integer level;
    private Integer status;
    /** 非持久化：该试卷下的试题数（后台统计用） */
    @TableField(exist = false)
    private Integer questionCount;
}
