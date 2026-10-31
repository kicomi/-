package com.campusbook.controller;

import com.campusbook.common.R;
import com.campusbook.common.UserContext;
import com.campusbook.entity.User;
import com.campusbook.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<User> me() {
        return R.ok(userService.get(UserContext.requireUserId()));
    }

    @PutMapping("/me")
    public R<User> update(@RequestBody User patch) {
        Long uid = UserContext.requireUserId();
        userService.updateProfile(uid, patch);
        return R.ok(userService.get(uid));
    }

    @GetMapping("/{id}")
    public R<User> get(@PathVariable Long id) {
        return R.ok(userService.get(id));
    }
}
