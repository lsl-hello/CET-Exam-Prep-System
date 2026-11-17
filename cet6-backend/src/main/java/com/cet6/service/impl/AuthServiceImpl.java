package com.cet6.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cet6.common.Constants;
import com.cet6.common.GlobalExceptionHandler.BusinessException;
import com.cet6.dto.ChangePasswordDTO;
import com.cet6.dto.LoginDTO;
import com.cet6.dto.RegisterDTO;
import com.cet6.entity.SysUser;
import com.cet6.security.JwtUtils;
import com.cet6.security.SecurityUtils;
import com.cet6.service.AuthService;
import com.cet6.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", buildUserInfo(user));
        // 首次登录/密码被重置时提示前端强制改密
        result.put("forceChange", user.getForceChange() != null && user.getForceChange() == 1);
        return result;
    }

    @Override
    public void register(RegisterDTO dto) {
        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone() != null ? dto.getPhone() : "");
        user.setRole(Constants.ROLE_STUDENT);
        user.setStatus(1);
        sysUserService.save(user);
    }

    @Override
    public Map<String, Object> getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserService.getById(userId);
        return buildUserInfo(user);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验原密码（支持从明文迁移期过渡：原密码可能是 BCrypt 哈希）
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setForceChange(0);
        sysUserService.updateById(user);
    }

    @Override
    public Map<String, Object> updateProfile(String nickname, String phone) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }
        if (phone != null) {
            user.setPhone(phone.trim());
        }
        sysUserService.updateById(user);
        return buildUserInfo(user);
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("phone", user.getPhone());
        info.put("role", user.getRole());
        return info;
    }
}
