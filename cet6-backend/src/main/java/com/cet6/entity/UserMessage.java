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
@TableName("user_message")
public class UserMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 1学习计划提醒 2系统公告推送 */
    private Integer msgType;
    private String title;
    private String content;
    /** 0未读 1已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}
