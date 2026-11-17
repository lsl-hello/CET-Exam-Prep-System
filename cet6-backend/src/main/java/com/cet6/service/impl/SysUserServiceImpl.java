package com.cet6.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cet6.entity.SysUser;
import com.cet6.mapper.SysUserMapper;
import com.cet6.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}
