package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_word")
public class UserWord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    /** 1熟练 2模糊 3遗忘 */
    private Integer memoryLevel;
    private LocalDateTime lastReviewTime;
    private LocalDateTime nextReviewTime;
    private Integer errorCount;
}
