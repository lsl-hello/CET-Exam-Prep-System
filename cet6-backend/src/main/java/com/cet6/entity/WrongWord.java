package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wrong_word")
public class WrongWord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordId;
    /** 加入原因：vague 模糊 / forget 遗忘 / manual 手动加入 */
    private String reason;
}
