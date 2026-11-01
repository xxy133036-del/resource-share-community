package com.hc1.admin8012.util;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hc1.admin8012.config.JwtProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilTest {

    private static Path keyDirectory;
    private static KeyFiles pairA;
    private static KeyFiles pairB;

    @BeforeAll
    static void createKeyPairs() throws Exception {
        keyDirectory = Files.createTempDirectory("admin-jwt-rsa-tests");
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
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        Files.write(privateFile, privatePem.getBytes(StandardCharsets.UTF_8));
        Files.write(publicFile, publicPem.getBytes(StandardCharsets.UTF_8));
        return new KeyFiles(privateFile, publicFile);
    }

    private JwtProperties props(KeyFiles keys) {
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyFile(keys.privateFile.toString());
        properties.setPublicKeyFile(keys.publicFile.toString());
        properties.setIssuer("弘创小明");
        properties.setExpireMinutes(60);
        properties.validate();
        return properties;
    }

    private Map<String, String> claims() {
        Map<String, String> claims = new HashMap<>();
        claims.put("id", "8012");
        claims.put("adminName", "admin");
        claims.put("roleName", "ROLE_SUPER_ADMIN");
        return claims;
    }

    @Test
    void createAndVerifyRoundTripUsesRs256() {
        JWTUtil util = new JWTUtil(props(pairA));
        String token = util.createToken(claims());
        assertEquals("RS256", util.verifyToken(token).getAlgorithm());
        assertEquals(Integer.valueOf(8012), util.getAdminIdFromToken(token));
        assertEquals("弘创小明", util.verifyToken(token).getIssuer());
    }

    @Test
    void tokenSignedByOtherKeyPairIsRejected() {
        String token = new JWTUtil(props(pairA)).createToken(claims());
        assertThrows(JWTVerificationException.class, () -> new JWTUtil(props(pairB)).verifyToken(token));
    }

    @Test
    void missingIdClaimReturnsNull() {
        JWTUtil util = new JWTUtil(props(pairA));
        Map<String, String> noId = new HashMap<>();
        noId.put("adminName", "admin");
        assertNull(util.getAdminIdFromToken(util.createToken(noId)));
    }

    @Test
    void missingKeyFilesRejectedOnStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtProperties().validate());
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
