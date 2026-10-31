package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.dto.AdminLoginReq;
import com.campusbook.dto.LoginReq;
import com.campusbook.dto.LoginResp;
import com.campusbook.entity.User;
import com.campusbook.service.AuthService;
import com.campusbook.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "小程序登录（演示模式 code=demo-001）")
    @PostMapping("/wx-login")
    public R<LoginResp> wxLogin(@RequestBody LoginReq req) {
        return R.ok(authService.wxLogin(req));
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/admin-login")
    public R<LoginResp> adminLogin(@Valid @RequestBody AdminLoginReq req) {
        return R.ok(authService.adminLogin(req));
    }

    @Operation(summary = "账号注册")
    @PostMapping("/register")
    public R<LoginResp> register(@Valid @RequestBody com.campusbook.dto.RegisterReq req) {
        return R.ok(authService.register(req));
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public R<LoginResp> login(@Valid @RequestBody com.campusbook.dto.PasswordLoginReq req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<User> me() {
        return R.ok(userService.get(UserContext.requireUserId()));
    }
}
