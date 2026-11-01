package com.hc1.user8011.util;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 跨模块一致性测试。
 *
 * <p>这是本次改造最容易被忽略、但故障时最致命的一点：
 * user8011 与 admin8012 必须使用完全相同的 JWT RSA 密钥文件配置与 jwt.issuer。
 * 只要有一个不一致，就会出现"在用户端登录后访问管理端报未登录"这类诡异问题。
 *
 * <p>这里直接读取两个模块的 application.yaml 做静态比对，把它变成 CI 能拦住的红线。
 *
 * @Author xwj
 * @Date 2026-10-06
 */
class JwtConfigConsistencyTest {

    private Map<String, Object> loadJwtSection(String moduleRelativePath) throws Exception {
        Path path = Paths.get(moduleRelativePath);
        assertNotNull(path, "找不到配置文件：" + moduleRelativePath);
        try (InputStream in = Files.newInputStream(path)) {
            Map<String, Object> root = new Yaml().load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> jwt = (Map<String, Object>) root.get("jwt");
            return jwt;
        }
    }

    @Test
    void bothModulesShareTheSameJwtKeyConfigurationAndIssuer() throws Exception {
        // 测试的工作目录是 user8011/，因此用相对路径回到仓库根
        Map<String, Object> user = loadJwtSection("../user8011/src/main/resources/application.yaml");
        Map<String, Object> admin = loadJwtSection("../admin8012/src/main/resources/application.yaml");

        assertNotNull(user, "user8011 的 application.yaml 缺少 jwt 配置段");
        assertNotNull(admin, "admin8012 的 application.yaml 缺少 jwt 配置段");

        assertEquals(user.get("private-key-file"), admin.get("private-key-file"),
                "user8011 与 admin8012 的 JWT 私钥配置不一致");
        assertEquals(user.get("public-key-file"), admin.get("public-key-file"),
                "user8011 与 admin8012 的 JWT 公钥配置不一致");
        assertEquals(user.get("issuer"), admin.get("issuer"),
                "user8011 与 admin8012 的 jwt.issuer 不一致");
    }

    @Test
    void keysAreExternallyConfiguredWithoutHardcodedDefaults() throws Exception {
        Map<String, Object> user = loadJwtSection("../user8011/src/main/resources/application.yaml");
        assertEquals(true, String.valueOf(user.get("private-key-file")).contains("${JWT_PRIVATE_KEY_FILE:"));
        assertEquals(true, String.valueOf(user.get("public-key-file")).contains("${JWT_PUBLIC_KEY_FILE:"));
    }
}
