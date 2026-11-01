package com.hc1.user8011.controller;

import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.service.RegisterService;
import com.hc1.user8011.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-04 17:00:16
 * @Version 1.0
 */
@RestController
public class RegisterController {
    @Autowired
    private RegisterService registerService;
    /** JWTUtil 已由静态工具类改为 Spring Bean，密钥从配置读取。 */
    @Autowired
    private JWTUtil jwtUtil;

    @PostMapping("/register")
    public CommonResult<?> register(@RequestBody User user) {
        // 先做 null 检查，避免 NPE
        if (user.getUsername() == null || user.getPassword() == null || user.getNickname() == null) {
            return CommonResult.error("信息不完整");
        }
        String usernameRegex = "^[a-zA-Z0-9]+$";
        String passwordRegex = "^[a-zA-Z0-9]{6,}$";
        String nicknameRegex = "^.{1,12}$";
        if (!user.getUsername().matches(usernameRegex) || !user.getPassword().matches(passwordRegex) || !user.getNickname().matches(nicknameRegex)) {
            return CommonResult.error("格式错误");
        }
        String result = registerService.register(user);
        if (result.equals("exist")) {
            return CommonResult.error("用户名已存在");
        }
        // 注册成功 封装用户信息 生成token
        try {
            User registeredUser = registerService.selectByUsername(user.getUsername());

            if (registeredUser == null || registeredUser.getId() == null) {
                return CommonResult.error("注册失败，请稍后重试");
            }

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("id", registeredUser.getId().toString());
            userInfo.put("username", registeredUser.getUsername());
            userInfo.put("nickname", registeredUser.getNickname());

            String token = jwtUtil.createToken(userInfo);
            return CommonResult.success(token);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("注册失败：" + e.getMessage());
        }
    }
}


