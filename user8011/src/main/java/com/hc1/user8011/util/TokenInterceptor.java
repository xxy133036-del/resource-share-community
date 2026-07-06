package com.hc1.user8011.util;

import com.auth0.jwt.exceptions.TokenExpiredException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TokenInterceptor implements HandlerInterceptor {

    /** 内部调用标记头：admin8012 通过 Feign 调用时携带，跳过用户 JWT 鉴权 */
    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 内部调用（admin8012 Feign）：跳过用户 JWT 校验，以管理员模式运行
        if ("true".equals(request.getHeader(INTERNAL_CALL_HEADER))) {
            AdminCallContext.setAdminCall(true);
            UserContext.setUserId(null); // userId=null → 业务层自然跳过积分/点赞逻辑
            return true;
        }

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

            Integer userId = JWTUtil.getUserIdFromToken(token);
            if (userId == null) {
                throw new RuntimeException("Token已失效，请重新登录");
            }

            UserContext.setUserId(userId);
            return true;
        } catch (TokenExpiredException e) {
            throw new RuntimeException("Token已过期，请重新登录");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Token解析失败，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUserId();
        AdminCallContext.remove();
    }
}
