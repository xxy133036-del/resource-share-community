package com.hc1.user8011.controller;

import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.service.LoginService;
import com.hc1.user8011.util.JWTUtil;
import com.hc1.user8011.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-04 17:00:27
 * @Version 1.0
 */
@RestController
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/login")
    public CommonResult<?> login(@RequestBody User user) {
        User loginUser = loginService.login(user.getUsername(), user.getPassword());
        if (loginUser != null) {
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("id", loginUser.getId().toString());
            userInfo.put("username", loginUser.getUsername());
            userInfo.put("nickname", loginUser.getNickname());
            String token = JWTUtil.createToken(userInfo);
            return CommonResult.success(token);
        }
        return CommonResult.error("账号或密码错误");
    }

}
