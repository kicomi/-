package com.campusbook.config;

import com.campusbook.common.BizException;
import com.campusbook.common.UserContext;
import com.campusbook.entity.User;
import com.campusbook.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * 登录校验 + 账号状态校验。
 * 光验签是不够的：用户被封禁后，手里的旧 token 仍然有效，
 * 之前能在被封后继续下单、发布，这里必须每次请求都查一次状态。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException(401, "未登录，请先登录");
        }

        Long uid;
        String role;
        try {
            Claims claims = jwtUtil.parse(auth.substring(7));
            uid = Long.valueOf(claims.getSubject());
            role = String.valueOf(claims.get("role"));
        } catch (Exception e) {
            throw new BizException(401, "登录状态无效，请重新登录");
        }

        // 管理员不走封禁校验
        if (!"admin".equals(role)) {
            User u = userMapper.selectById(uid);
            if (u == null) throw new BizException(401, "账号不存在，请重新登录");

            if (u.getStatus() != null && u.getStatus() == 0) {
                // 临时封禁到期就自动放行，和登录时的处理保持一致
                if (u.getBanUntil() != null && u.getBanUntil().isBefore(LocalDateTime.now())) {
                    u.setStatus(1);
                    u.setBanUntil(null);
                    u.setBanReason(null);
                    userMapper.updateById(u);
                } else {
                    String until = u.getBanUntil() == null ? "永久" : u.getBanUntil().toLocalDate().toString();
                    throw new BizException(403, "账号已被封禁（至 " + until + "），只能查看，不能操作");
                }
            }
        }

        UserContext.set(uid, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
