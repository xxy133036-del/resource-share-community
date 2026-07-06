package com.hc1.admin8012.controller;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.service.IndexService;
import com.hc1.admin8012.util.adminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping("/home")
    public CommonResult<?> home() {
        Integer adminId = adminContext.getAdminId();
        if (adminId == null || adminId <= 0) {
            return CommonResult.error(401, "管理员未登录或登录已失效，请重新登录");
        }
        Map<String, Object> adminAndPrivileges = indexService.indexAdmin(adminId);
        Map<String, Object> result = new HashMap<>();
        result.put("adminAndPrivileges", adminAndPrivileges);
        adminContext.removeAdminId();
        return CommonResult.success(result);
    }
}
