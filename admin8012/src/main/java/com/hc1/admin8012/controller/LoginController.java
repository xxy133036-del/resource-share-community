package com.hc1.admin8012.controller;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.service.LoginService;
import com.hc1.admin8012.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-29 10:00:00
 * @Version 1.0
 */
@RestController
public class LoginController {
    @Autowired
    private LoginService loginService;
    /** JWTUtil 已由静态工具类改为 Spring Bean，密钥从配置读取。 */
    @Autowired
    private JWTUtil jwtUtil;

    @PostMapping("/login")
    public CommonResult<?> login(@RequestBody admin admin) {
        admin loginAdmin = loginService.login(admin.getAdminName(), admin.getPassword());
        if (loginAdmin != null) {
            Map<String, String> adminInfo = new HashMap<>();
            adminInfo.put("id", loginAdmin.getId().toString());
            adminInfo.put("adminName", loginAdmin.getAdminName());
            adminInfo.put("roleName", loginAdmin.getRoleName());
            String token = jwtUtil.createToken(adminInfo);
            return CommonResult.success(token);
        }
        return CommonResult.error("账号或密码错误");
    }

}
