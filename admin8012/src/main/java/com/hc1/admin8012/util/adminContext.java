package com.hc1.admin8012.util;

public class adminContext {
    private static final ThreadLocal<Integer> ADMIN_THREAD_LOCAL = new ThreadLocal<>();

    public static void setAdminId(Integer adminId) {
        ADMIN_THREAD_LOCAL.set(adminId);
    }

    public static Integer getAdminId() {
        return ADMIN_THREAD_LOCAL.get();
    }

    public static void removeAdminId() {
        ADMIN_THREAD_LOCAL.remove();
    }
}