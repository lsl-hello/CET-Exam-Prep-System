package com.cet6.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
}
