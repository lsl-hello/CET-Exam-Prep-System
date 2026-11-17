package com.cet6.controller;

import com.cet6.common.Result;
import com.cet6.dto.ChangePasswordDTO;
import com.cet6.dto.LoginDTO;
import com.cet6.dto.RegisterDTO;
import com.cet6.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.success(authService.getCurrentUser());
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success();
    }

    /** 修改个人资料：昵称、电话 */
    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body) {
        return Result.success(authService.updateProfile(body.get("nickname"), body.get("phone")));
    }
}
