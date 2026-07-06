package com.hc1.admin8012.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author xwj
 * @Description Feign 请求拦截器：每次调用 user8011 时自动注入 X-Internal-Call 标记头
 *              user8011 检测到该头后跳过用户 JWT 校验，以管理员模式运行
 * @Date 2026-07-03
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    @Override
    public void apply(RequestTemplate template) {
        template.header(INTERNAL_CALL_HEADER, "true");
    }
}
