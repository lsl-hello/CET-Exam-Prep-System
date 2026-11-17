package com.cet6.service;

import com.cet6.dto.ChangePasswordDTO;
import com.cet6.dto.LoginDTO;
import com.cet6.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {

    Map<String, Object> login(LoginDTO dto);

    void register(RegisterDTO dto);

    Map<String, Object> getCurrentUser();

    /** 修改密码：校验原密码、加密新密码、清掉 forceChange 标记 */
    void changePassword(ChangePasswordDTO dto);

    /** 修改个人资料：昵称、电话 */
    Map<String, Object> updateProfile(String nickname, String phone);
}
