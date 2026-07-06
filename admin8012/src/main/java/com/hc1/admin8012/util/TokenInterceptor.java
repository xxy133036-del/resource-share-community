package com.hc1.admin8012.util;

import com.auth0.jwt.exceptions.TokenExpiredException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if (token == null || token.isEmpty()) {
            token = request.getHeader("Authorization");
        }

        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("未登录，请先登录");
        }

        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Integer adminId = JWTUtil.getAdminIdFromToken(token);
            if (adminId == null) {
                throw new RuntimeException("Token已失效，请重新登录");
            }

            adminContext.setAdminId(adminId);
            return true;
        } catch (TokenExpiredException e) {
            // Token过期异常，抛出由全局异常处理器处理
            throw new RuntimeException("Token已过期，请重新登录");
        } catch (RuntimeException e) {
            // 直接抛出运行时异常，由全局异常处理器处理
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Token解析失败，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        adminContext.removeAdminId();
    }
}
