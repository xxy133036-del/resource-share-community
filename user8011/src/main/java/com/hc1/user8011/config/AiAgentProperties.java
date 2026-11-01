package com.hc1.user8011.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Agent（Python 服务）对接配置。
 *
 * @Author xwj
 * @Date 2026-10-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-agent")
public class AiAgentProperties {

    /** 服务地址，如 http://localhost:8013 */
    private String url = "http://localhost:8013";

    /** Java 持有的 RSA PKCS#8 私钥文件路径；Python 仅配置对应公钥。 */
    private String privateKeyFile = "";

    /** 建立连接超时（毫秒） */
    private int connectTimeout = 5000;

    /**
     * 读取超时（毫秒）。
     * 流式场景下为"两次数据之间的最大间隔"，不是总时长，因此可以给得较大。
     */
    private int readTimeout = 300000;

    /** 非流式问答超时（毫秒） */
    private int askTimeout = 120000;

    /** 流式转发的线程池大小 */
    private int streamPoolSize = 8;

    /** 单次请求最大问题长度，与 Python 端校验保持一致 */
    private int maxQuestionLength = 1000;
}
