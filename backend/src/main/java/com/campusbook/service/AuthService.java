package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.common.BizException;
import com.campusbook.config.JwtUtil;
import com.campusbook.dto.AdminLoginReq;
import com.campusbook.dto.LoginReq;
import com.campusbook.dto.LoginResp;
import com.campusbook.entity.User;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 小程序登录。演示模式下 code = demo-001 会映射到种子用户 o_stu_001，
     * 无需真实 AppID/AppSecret 即可完整演示。
     */
    public LoginResp wxLogin(LoginReq req) {
        String code = req.getCode() == null ? "" : req.getCode().trim();
        if (code.isEmpty()) throw new BizException("缺少登录 code");

        String openid;
        if (code.startsWith("demo-")) {
            String n = code.substring(5);
            try {
                openid = "o_stu_" + String.format("%03d", Integer.parseInt(n));
            } catch (NumberFormatException e) {
                openid = "o_stu_001";
            }
        } else {
            openid = "wx_" + md5(code).substring(0, 16);
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid).last("limit 1"));

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                    ? "同学" + openid.substring(Math.max(0, openid.length() - 3)) : req.getNickname());
            user.setAvatar(req.getAvatar() == null ? "" : req.getAvatar());
            user.setRole("student");
            // 新号没有任何交易记录，从 80 分起步（白银），靠完成交易攒信用
            user.setCreditScore(80);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("新用户注册: {} {}", user.getId(), user.getNickname());
        }

        // 先让 decorate 处理到期自动解封，再判断是否仍在封禁期
        userService.decorate(user);
        if (user.getStatus() != null && user.getStatus() == 0) {
            String msg = user.getBanUntil() == null
                    ? "账号已被永久封禁，请联系管理员"
                    : "账号封禁至 " + user.getBanUntil().toLocalDate() + "，请到期后再试";
            throw new BizException(403, msg);
        }

        // 小程序端要直接展示专业名，这里回填一次，省得前端再查一次字典
        userService.fillMajor(user);

        String token = jwtUtil.create(user.getId(), user.getRole());
        return new LoginResp(token, user);
    }

    /**
     * 账号注册。演示环境用手机号/学号当账号，密码用 BCrypt 存。
     * openid 用 "reg_" + md5(account) 生成，和微信登录的 o_xxx 区分开。
     */
    @org.springframework.transaction.annotation.Transactional
    public LoginResp register(com.campusbook.dto.RegisterReq req) {
        String account = req.getAccount() == null ? "" : req.getAccount().trim();
        if (account.isEmpty()) throw new BizException("账号不能为空");

        String openid = "reg_" + md5(account);
        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid).last("limit 1"));
        if (exist != null) throw new BizException("该账号已注册，请直接登录");

        User u = new User();
        u.setOpenid(openid);
        u.setNickname(req.getNickname());
        u.setStudentNo(req.getStudentNo());
        u.setRealName(req.getRealName());
        u.setRealPhone(account.matches("\\d{11}") ? account : null);
        u.setPhone(account.matches("\\d{11}") ? account : null);
        u.setMajorId(req.getMajorId());
        u.setCustomMajor(req.getCustomMajor());
        u.setGrade(req.getGrade());
        u.setGender(req.getGender());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRole("student");
        u.setCreditScore(80);
        u.setStatus(1);
        userMapper.insert(u);

        userService.fillMajor(u);
        // 注册即登录，省一次跳转
        return new LoginResp(jwtUtil.create(u.getId(), u.getRole()), u);
    }

    /** 账号密码登录 */
    public LoginResp login(com.campusbook.dto.PasswordLoginReq req) {
        String account = req.getAccount() == null ? "" : req.getAccount().trim();
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, "reg_" + md5(account)).last("limit 1"));
        if (u == null || u.getPassword() == null || !encoder.matches(req.getPassword(), u.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        userService.decorate(u);
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizException(403, "账号已被封禁，请联系管理员");
        }
        userService.fillMajor(u);
        return new LoginResp(jwtUtil.create(u.getId(), u.getRole()), u);
    }

    public LoginResp adminLogin(AdminLoginReq req) {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, "admin-openid").last("limit 1"));
        if (admin == null) throw new BizException("管理员账号未初始化");
        if (admin.getPassword() == null || !encoder.matches(req.getPassword(), admin.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        userService.fillMajor(admin);
        return new LoginResp(jwtUtil.create(admin.getId(), admin.getRole()), admin);
    }

    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
