package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_question_record")
public class UserQuestionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    /** 0错误/主观题 1客观题正确 */
    private Integer isCorrect;
    private LocalDateTime answerTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
