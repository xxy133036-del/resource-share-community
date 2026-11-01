package com.hc1.user8011.util;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.hc1.user8011.config.JwtProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 装配测试：验证 RS256 密钥路径和 JWT 配置来自属性源。 */
class JwtConfigWiringTest {

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfig {
    }

    private static Path keyDirectory;
    private static KeyFiles pairA;
    private static KeyFiles pairB;

    @BeforeAll
    static void createKeyPairs() throws Exception {
        keyDirectory = Files.createTempDirectory("jwt-wiring-rsa-tests");
        pairA = createPair("a");
        pairB = createPair("b");
    }

    @AfterAll
    static void removeKeyPairs() throws Exception {
        if (keyDirectory != null) {
            Files.deleteIfExists(pairA.privateFile);
            Files.deleteIfExists(pairA.publicFile);
            Files.deleteIfExists(pairB.privateFile);
            Files.deleteIfExists(pairB.publicFile);
            Files.deleteIfExists(keyDirectory);
        }
    }

    private static KeyFiles createPair(String name) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Path privateFile = keyDirectory.resolve(name + "-private.pem");
        Path publicFile = keyDirectory.resolve(name + "-public.pem");
        Files.write(privateFile, pem("PRIVATE KEY", pair.getPrivate().getEncoded()).getBytes(StandardCharsets.UTF_8));
        Files.write(publicFile, pem("PUBLIC KEY", pair.getPublic().getEncoded()).getBytes(StandardCharsets.UTF_8));
        return new KeyFiles(privateFile, publicFile);
    }

    private static String pem(String type, byte[] key) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key)
                + "\n-----END " + type + "-----\n";
    }

    private ApplicationContextRunner runner(KeyFiles keys) {
        return new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class, JWTUtil.class)
                .withPropertyValues(
                        "jwt.private-key-file=" + keys.privateFile,
                        "jwt.public-key-file=" + keys.publicFile
                );
    }

    @Test
    void keyPathsAndIssuerComeFromPropertySource() {
        runner(pairA).withPropertyValues("jwt.issuer=wiring-issuer", "jwt.expire-minutes=15").run(ctx -> {
            JwtProperties properties = ctx.getBean(JwtProperties.class);
            assertThat(properties.getPrivateKeyFile()).isEqualTo(pairA.privateFile.toString());
            assertThat(properties.getPublicKeyFile()).isEqualTo(pairA.publicFile.toString());
            assertThat(properties.getIssuer()).isEqualTo("wiring-issuer");
            assertThat(properties.getExpireMinutes()).isEqualTo(15);
        });
    }

    @Test
    void jwtUtilIsWiredAndUsesInjectedRsaKeys() {
        runner(pairA).withPropertyValues("jwt.issuer=wiring-issuer").run(ctx -> {
            JWTUtil util = ctx.getBean(JWTUtil.class);
            String token = util.createToken(Collections.singletonMap("id", "42"));
            assertThat(util.verifyToken(token).getAlgorithm()).isEqualTo("RS256");
            assertThat(util.verifyToken(token).getIssuer()).isEqualTo("wiring-issuer");
            assertThat(util.getUserIdFromToken(token)).isEqualTo(42);
        });
    }

    @Test
    void tokenFromOneKeyPairCannotBeVerifiedByAnother() {
        AtomicReference<String> tokenRef = new AtomicReference<>();
        runner(pairA).run(ctx -> tokenRef.set(ctx.getBean(JWTUtil.class)
                .createToken(Collections.singletonMap("id", "1"))));

        assertThatThrownBy(() -> runner(pairB).run(ctx -> ctx.getBean(JWTUtil.class).verifyToken(tokenRef.get())))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void mismatchedKeyPairFailsContextStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class, JWTUtil.class)
                .withPropertyValues(
                        "jwt.private-key-file=" + pairA.privateFile,
                        "jwt.public-key-file=" + pairB.publicFile
                )
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void missingKeysFailContextStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class, JWTUtil.class)
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    private static final class KeyFiles {
        private final Path privateFile;
        private final Path publicFile;

        private KeyFiles(Path privateFile, Path publicFile) {
            this.privateFile = privateFile;
            this.publicFile = publicFile;
        }
    }
}
