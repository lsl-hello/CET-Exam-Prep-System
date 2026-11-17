package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_error_question")
public class UserErrorQuestion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private Integer errorCount;
    /** 0未掌握 1已吃透 */
    private Integer isMaster;
}
