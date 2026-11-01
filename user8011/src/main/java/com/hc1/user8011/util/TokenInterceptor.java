package com.hc1.user8011.util;

import com.auth0.jwt.exceptions.TokenExpiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户端 Token 拦截器。
 *
 * <p>改造要点（2026-10-06）：{@link JWTUtil} 由静态工具类改为 Spring Bean，
 * 这里通过构造器注入，密钥来源随配置走。
 *
 * @Author xwj
 * @Date 2026-10-06
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    /** 内部调用标记头：admin8012 通过 Feign 调用时携带，跳过用户 JWT 鉴权 */
    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    private static final String HEADER_LOWER = "authorization";
    private static final String HEADER_UPPER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTUtil jwtUtil;

    public TokenInterceptor(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 内部调用（admin8012 Feign）：跳过用户 JWT 校验，以管理员模式运行
        if ("true".equals(request.getHeader(INTERNAL_CALL_HEADER))) {
            AdminCallContext.setAdminCall(true);
            UserContext.setUserId(null); // userId=null -> 业务层自然跳过积分/点赞逻辑
            return true;
        }

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

            Integer userId = jwtUtil.getUserIdFromToken(token);
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
