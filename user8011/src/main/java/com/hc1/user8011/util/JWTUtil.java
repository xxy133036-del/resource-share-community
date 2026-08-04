package com.hc1.user8011.util;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

public class JWTUtil {

    private static final Logger log = LoggerFactory.getLogger(JWTUtil.class);

    private final static String ENCRYPT_KEY = "jd_hc"; // 加密的密钥
    private final static int EXPIRE_TIME = 60; // token 过期时间，单位分钟
    private static final String ISSUER = "弘创小明";

    public static String createToken(Map<String,String> subject) {
        JWTCreator.Builder builder = JWT.create();
        subject.forEach((k, v) -> builder.withClaim(k, v));
        return builder.withIssuer(ISSUER)
                .withExpiresAt(DateUtil.offsetMinute(new Date(), EXPIRE_TIME))
                .sign(Algorithm.HMAC256(ENCRYPT_KEY));
    }

    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(ENCRYPT_KEY))
                .withIssuer(ISSUER)
                .build().verify(token);
    }

    public static Integer getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            String userIdStr = jwt.getClaim("id").asString();

            if (userIdStr == null) {
                log.warn("Token中未找到 id 字段");
                return null;
            }

            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("Token中id字段格式错误：{}", e.getMessage());
            return null;
        }
    }
}