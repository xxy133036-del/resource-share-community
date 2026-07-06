package com.hc1.user8011.util;

/**
 * @Author xwj
 * @Description 标记当前请求是否为管理员内部调用（由 admin8012 通过 Feign 发起）
 *              管理员调用：跳过 JWT 校验、不扣积分、不填点赞状态、不过滤文件状态
 * @Date 2026-07-03
 */
public class AdminCallContext {
    private static final ThreadLocal<Boolean> ADMIN_CALL = new ThreadLocal<>();

    public static void setAdminCall(boolean adminCall) {
        ADMIN_CALL.set(adminCall);
    }

    public static boolean isAdminCall() {
        return Boolean.TRUE.equals(ADMIN_CALL.get());
    }

    public static void remove() {
        ADMIN_CALL.remove();
    }
}
