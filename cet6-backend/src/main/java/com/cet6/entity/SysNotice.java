package com.cet6.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cet6.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Integer sort;
    /** 0下架 1上架 */
    private Integer status;
}
