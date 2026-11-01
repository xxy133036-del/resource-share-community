package com.hc1.user8011.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AiAgentSignerTest {

    private static Path privateKeyFile;
    private static java.security.PublicKey publicKey;

    @BeforeAll
    static void createTestKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        publicKey = pair.getPublic();
        privateKeyFile = Files.createTempFile("ai-agent-test-private", ".pem");
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Files.write(privateKeyFile, pem.getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    static void removeTestKey() throws Exception {
        if (privateKeyFile != null) {
            Files.deleteIfExists(privateKeyFile);
        }
    }

    private String sign(String timestamp, String requestId, byte[] body,
                        String method, String path, String query) {
        return AiAgentSigner.sign(privateKeyFile.toString(), timestamp, requestId, body, method, path, query);
    }

    private boolean verifies(String signature, String timestamp, String requestId, byte[] body,
                             String method, String path, String query) throws Exception {
        String canonical = AiAgentSigner.canonicalRequest(method, path, query, body);
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest(canonical.getBytes(StandardCharsets.UTF_8))) {
            hex.append(String.format("%02x", value & 0xff));
        }
        byte[] payload = (timestamp + "." + requestId + "." + hex).getBytes(StandardCharsets.UTF_8);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(payload);
        return verifier.verify(Base64.getDecoder().decode(signature));
    }

    @Test
    void signatureIsBase64RsaAndVerifiesWithPublicKey() throws Exception {
        byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        String signature = sign("1700000000", "req-1", body, "POST", "/v1/chat/completions", "");
        assertTrue(verifies(signature, "1700000000", "req-1", body, "POST", "/v1/chat/completions", ""));
        assertEquals(344, signature.length(), "2048 位 RSA 签名的 Base64 长度应为 344 字符");
    }

    @Test
    void signatureChangesWithBodyAndCannotVerifyTamperedRequest() throws Exception {
        String signature = sign("1700000000", "req-1", "{\"a\":1}".getBytes(StandardCharsets.UTF_8),
                "POST", "/v1/chat/completions", "");
        assertFalse(verifies(signature, "1700000000", "req-1", "{\"a\":2}".getBytes(StandardCharsets.UTF_8),
                "POST", "/v1/chat/completions", ""));
    }

    @Test
    void canonicalQueryIsOrderInsensitiveAndEscapesDelimiters() {
        assertEquals(AiAgentSigner.canonicalQuery("userId=15&limit=10"),
                AiAgentSigner.canonicalQuery("limit=10&userId=15"));
        assertEquals("a=b%26c%3Dd", AiAgentSigner.canonicalQuery("a=b%26c%3Dd"));
        assertEquals("a=b&c=d", AiAgentSigner.canonicalQuery("a=b&c=d"));
        assertNotEquals(AiAgentSigner.canonicalQuery("a=b%26c%3Dd"),
                AiAgentSigner.canonicalQuery("a=b&c=d"));
    }

    @Test
    void canonicalQueryUsesStableUnicodeOrdering() {
        assertEquals("%EE%80%80=2&%F0%90%80%80=1",
                AiAgentSigner.canonicalQuery("𐀀=1&=2"));
    }

    @Test
    void getSignatureCoversQueryParametersAndPath() throws Exception {
        String signature = sign("1700000000", "req-get", new byte[0], "GET", "/v1/sessions", "userId=15&limit=10");
        assertTrue(verifies(signature, "1700000000", "req-get", new byte[0],
                "GET", "/v1/sessions", "userId=15&limit=10"));
        assertFalse(verifies(signature, "1700000000", "req-get", new byte[0],
                "GET", "/v1/sessions", "userId=1&limit=10"));
        assertFalse(verifies(signature, "1700000000", "req-get", new byte[0],
                "GET", "/v1/sessions/other", "userId=15&limit=10"));
    }
}
