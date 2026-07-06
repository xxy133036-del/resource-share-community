package com.hc1.admin8012.controller;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.service.RegisterService;
import com.hc1.admin8012.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RegisterController {
    @Autowired
    private RegisterService registerService;

    @PostMapping("/register")
    public Object register(@RequestBody admin admin) {
        String result = registerService.register(admin);
        if (result.equals("exist")) {
            return CommonResult.error("管理员名称已存在");
        } else if (result.equals("格式错误")) {
            return CommonResult.error("格式错误");
        } else if (result.equals("信息不完整")) {
            return CommonResult.error("信息不完整");
        }
        // 注册成功 封装管理员信息 生成token
        try {
            admin registeredAdmin = registerService.selectByAdminName(admin.getAdminName());

            if (registeredAdmin == null || registeredAdmin.getId() == null) {
                return CommonResult.error("注册失败，请稍后重试");
            }

            Map<String, String> adminInfo = new HashMap<>();
            adminInfo.put("id", registeredAdmin.getId().toString());
            adminInfo.put("adminName", registeredAdmin.getAdminName());
            adminInfo.put("roleName", registeredAdmin.getRoleName());

            String token = JWTUtil.createToken(adminInfo);
            return CommonResult.success(token);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("注册失败：" + e.getMessage());
        }
    }
}
