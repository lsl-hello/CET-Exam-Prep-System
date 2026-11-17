package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_study_plan")
public class UserStudyPlan extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String planName;
    /** 1背单词 2刷题 */
    private Integer planType;
    private Integer dailyTargetNum;
    private LocalTime remindTime;
    /** 1每天 2仅工作日 */
    private Integer cycleType;
    private Integer status;
    /** 关联单词书 id（planType=1 背单词时必填） */
    private Long bookId;
    /** 关联试卷 id（planType=2 刷题时选择试题用） */
    private Long paperId;
}
