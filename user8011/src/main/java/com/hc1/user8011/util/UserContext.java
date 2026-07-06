package com.hc1.user8011.util;

public class UserContext {
    private static final ThreadLocal<Integer> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUserId(Integer userId) {
        USER_THREAD_LOCAL.set(userId);
    }

    public static Integer getUserId() {
        return USER_THREAD_LOCAL.get();
    }

    public static void removeUserId() {
        USER_THREAD_LOCAL.remove();
    }
}