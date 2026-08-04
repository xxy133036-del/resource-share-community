package com.hc1.user8011.config;

import com.hc1.user8011.bean.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理运行时异常 (NullPointerException 属于这个范畴)
     */
    @ExceptionHandler(RuntimeException.class)
    public CommonResult<?> handleRuntimeException(RuntimeException e, HttpServletResponse response) {
        log.error("运行时异常", e);

        String message = e.getMessage();

        // 判断是否是认证相关的异常
        if (message != null && (message.equals("未登录，请先登录") || 
                                message.equals("Token已失效，请重新登录") ||
                                message.equals("Token已过期，请重新登录"))) {
            // 设置HTTP状态码为401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return CommonResult.error(401, message);
        }

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return CommonResult.error(message == null ? "服务器发生运行时异常(如空指针)，请查看后端控制台日志" : message);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e, HttpServletResponse response) {
        log.error("未处理异常", e);

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String message = e.getMessage();
        return CommonResult.error("服务器内部错误：" + (message == null ? e.getClass().getSimpleName() : message));
    }
}