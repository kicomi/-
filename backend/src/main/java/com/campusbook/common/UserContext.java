package com.campusbook.common;

public class UserContext {
    private static final ThreadLocal<Long> UID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(Long uid, String role) {
        UID.set(uid);
        ROLE.set(role);
    }

    public static Long getUserId() {
        return UID.get();
    }

    public static Long requireUserId() {
        Long id = UID.get();
        if (id == null) throw new BizException(401, "未登录或登录已过期");
        return id;
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static boolean isAdmin() {
        return "admin".equals(ROLE.get());
    }

    public static void requireAdmin() {
        if (!isAdmin()) throw new BizException(403, "需要管理员权限");
    }

    public static void clear() {
        UID.remove();
        ROLE.remove();
    }
}
