package com.hc1.admin8012.util;

import com.auth0.jwt.exceptions.TokenExpiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理端 Token 拦截器。
 *
 * <p>改造要点（2026-10-06）：{@link JWTUtil} 由静态工具类改为 Spring Bean，
 * 这里通过构造器注入，密钥来源随配置走。
 *
 * @Author xwj
 * @Date 2026-10-06
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String HEADER_LOWER = "authorization";
    private static final String HEADER_UPPER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTUtil jwtUtil;

    public TokenInterceptor(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(HEADER_LOWER);
        if (token == null || token.isEmpty()) {
            token = request.getHeader(HEADER_UPPER);
        }

        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("未登录，请先登录");
        }

        try {
            if (token.startsWith(BEARER_PREFIX)) {
                token = token.substring(BEARER_PREFIX.length());
            }

            Integer adminId = jwtUtil.getAdminIdFromToken(token);
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
