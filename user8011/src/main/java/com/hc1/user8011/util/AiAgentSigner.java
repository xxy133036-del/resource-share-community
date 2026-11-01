package com.hc1.user8011.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Java -> Python 内部请求的 RSA-SHA256 签名器。 */
public final class AiAgentSigner {

    private static final ConcurrentMap<String, PrivateKey> PRIVATE_KEYS = new ConcurrentHashMap<>();

    private AiAgentSigner() {
    }

    public static boolean isConfigured(String privateKeyFile) {
        return privateKeyFile != null && !privateKeyFile.trim().isEmpty();
    }

    /** 请求签名覆盖 method、path、query、body、时间戳和请求 ID。 */
    public static String sign(String privateKeyFile, String timestamp, String requestId, byte[] body,
                              String method, String path, String query) {
        if (!isConfigured(privateKeyFile)) {
            throw new IllegalStateException("未配置 Java->Python RSA 私钥文件");
        }
        String canonical = canonicalRequest(method, path, query, body);
        String payload = timestamp + "." + requestId + "." + sha256Hex(canonical.getBytes(StandardCharsets.UTF_8));
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(loadPrivateKey(privateKeyFile));
            signer.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("生成 Java->Python RSA 请求签名失败", e);
        }
    }

    public static String canonicalRequest(String method, String path, String query, byte[] body) {
        String m = (method == null || method.isEmpty()) ? "POST" : method.toUpperCase();
        return m + "\n"
                + (path == null ? "" : path) + "\n"
                + canonicalQuery(query) + "\n"
                + sha256Hex(body == null ? new byte[0] : body);
    }

    /** 按编码后的键和值排序，避免 Unicode 顺序和分隔符歧义。 */
    public static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<String[]> pairs = new ArrayList<>();
        for (String segment : rawQuery.split("&")) {
            if (segment.isEmpty()) {
                continue;
            }
            int idx = segment.indexOf('=');
            String key = idx < 0 ? segment : segment.substring(0, idx);
            String value = idx < 0 ? "" : segment.substring(idx + 1);
            pairs.add(new String[]{urlDecode(key), urlDecode(value)});
        }
        pairs.sort((a, b) -> {
            int byKey = urlEncode(a[0]).compareTo(urlEncode(b[0]));
            return byKey != 0 ? byKey : urlEncode(a[1]).compareTo(urlEncode(b[1]));
        });
        StringBuilder result = new StringBuilder();
        for (String[] pair : pairs) {
            if (result.length() > 0) {
                result.append('&');
            }
            result.append(urlEncode(pair[0])).append('=').append(urlEncode(pair[1]));
        }
        return result.toString();
    }

    private static PrivateKey loadPrivateKey(String file) throws Exception {
        String absolutePath = Paths.get(file).toAbsolutePath().normalize().toString();
        PrivateKey cached = PRIVATE_KEYS.get(absolutePath);
        if (cached != null) {
            return cached;
        }
        String pem = new String(Files.readAllBytes(Paths.get(absolutePath)), StandardCharsets.UTF_8);
        String encoded = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        PrivateKey loaded = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        if (!(loaded instanceof RSAPrivateKey)
                || ((RSAPrivateKey) loaded).getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("服务间签名私钥必须是至少 2048 位的 RSA 私钥");
        }
        PrivateKey previous = PRIVATE_KEYS.putIfAbsent(absolutePath, loaded);
        return previous == null ? loaded : previous;
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(Character.forDigit((value >> 4) & 0xF, 16));
                result.append(Character.forDigit(value & 0xF, 16));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算请求摘要失败", e);
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("%7E", "~")
                    .replace("*", "%2A");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
