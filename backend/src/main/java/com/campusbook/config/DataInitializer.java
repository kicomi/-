package com.campusbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.entity.User;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;

    @Override
    public void run(ApplicationArguments args) {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, "admin-openid").last("limit 1"));
        if (admin == null) {
            log.warn("未找到管理员账号，跳过密码初始化");
            return;
        }
        if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
            admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
            userMapper.updateById(admin);
            log.info("已初始化管理员密码: admin123");
        }
    }
}
