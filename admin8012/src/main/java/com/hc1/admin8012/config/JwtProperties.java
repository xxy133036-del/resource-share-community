package com.hc1.admin8012.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** RS256 JWT 配置：Java 服务使用私钥签发、公钥验签。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String privateKeyFile;
    private String publicKeyFile;
    private String issuer = "弘创小明";
    private int expireMinutes = 60;

    @Getter
    private RSAPrivateKey signingKey;
    @Getter
    private RSAPublicKey verificationKey;

    @PostConstruct
    public void validate() {
        if (privateKeyFile == null || privateKeyFile.trim().isEmpty()
                || publicKeyFile == null || publicKeyFile.trim().isEmpty()) {
            throw new IllegalStateException("必须配置 jwt.private-key-file 和 jwt.public-key-file");
        }
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            byte[] privateBytes = readPem(privateKeyFile, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            byte[] publicBytes = readPem(publicKeyFile, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
            signingKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            verificationKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(publicBytes));
        } catch (Exception e) {
            throw new IllegalStateException("加载 JWT RSA 密钥失败，请检查 PKCS#8 私钥和 X.509 公钥文件", e);
        }
        if (signingKey.getModulus().bitLength() < 2048 || verificationKey.getModulus().bitLength() < 2048) {
            throw new IllegalStateException("JWT RSA 密钥长度不得小于 2048 位");
        }
        if (!signingKey.getModulus().equals(verificationKey.getModulus())) {
            throw new IllegalStateException("JWT 私钥与公钥不属于同一密钥对");
        }
        if (issuer == null || issuer.trim().isEmpty()) {
            throw new IllegalStateException("未配置 jwt.issuer");
        }
        if (expireMinutes <= 0) {
            throw new IllegalStateException("jwt.expire-minutes 必须为正数");
        }
    }

    private static byte[] readPem(String file, String begin, String end) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
        String encoded = pem.replace(begin, "").replace(end, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }
}
