package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_word_record")
public class UserWordRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    /** 1新词学习 2复习旧词 */
    private Integer reviewType;
    /** 1认识 2模糊 3遗忘 */
    private Integer memoryResult;
    private LocalDate reviewDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
