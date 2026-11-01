package com.hc1.user8011.util;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hc1.user8011.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT 签发与校验工具。
 *
 * <p>改造要点（2026-10-06）：密钥、签发者、有效期全部改为从 JwtProperties
 * 读取（最终来源是配置文件 / 环境变量），不再硬编码在源码里。
 *
 * <p>同时由静态工具类改为 Spring Bean，这样密钥可以随环境切换：
 * 开发、测试、生产各用各的密钥，且真实密钥永远不进 Git。
 *
 * <p>注意：user8011 与 admin8012 必须配置同一组 JWT RSA 密钥与 jwt.issuer，
 * 否则两边签发的 Token 无法互认。
 *
 * @Author xwj
 * @Date 2026-10-06
 */
@Component
public class JWTUtil {

    private static final Logger log = LoggerFactory.getLogger(JWTUtil.class);

    private final JwtProperties jwtProperties;

    public JWTUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成 Token。
     *
     * @param subject 载荷，例如 id / username / nickname
     */
    public String createToken(Map<String, String> subject) {
        JWTCreator.Builder builder = JWT.create();
        subject.forEach(builder::withClaim);
        return builder.withIssuer(jwtProperties.getIssuer())
                .withExpiresAt(DateUtil.offsetMinute(new Date(), jwtProperties.getExpireMinutes()))
                .sign(Algorithm.RSA256(jwtProperties.getVerificationKey(), jwtProperties.getSigningKey()));
    }

    /**
     * 校验并解析 Token。签名不对、已过期、issuer 不符都会抛出异常。
     */
    public DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.RSA256(jwtProperties.getVerificationKey(), null))
                .withIssuer(jwtProperties.getIssuer())
                .build()
                .verify(token);
    }

    /**
     * 读取 Token 中的 id 并转成 Integer，取不到或格式错误返回 null。
     */
    public Integer getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            String idStr = jwt.getClaim("id").asString();

            if (idStr == null) {
                log.warn("Token 中未找到 id 字段");
                return null;
            }

            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            log.warn("Token 中 id 字段格式错误：{}", e.getMessage());
            return null;
        }
    }
}
