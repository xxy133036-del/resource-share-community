package com.hc1.admin8012.controller;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.service.UserService;
import com.hc1.admin8012.util.adminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hc1.admin8012.service.adminService;
import java.util.List;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-27 10:34:39
 * @Version 1.0
 */
@RestController
public class userController {
    @Autowired
    private UserService userService;
    @Autowired
    private adminService adminService;
    //修改用户状态 0:冻结  1:正常(解冻)
    @RequestMapping("/updateUserStatusById")
    public CommonResult<?> updateUserStatusById(int status, int userId){
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
        adminContext.removeAdminId();
       return userService.updateUserStatusById(status, userId);
    }

    @RequestMapping("/getUserById")
    public CommonResult<?> getUserById(int userId){
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
        adminContext.removeAdminId();
        return userService.getUserById(userId);
    }

    @RequestMapping("/getUserFilesByUserId")
    public CommonResult<?> getUserFilesByUserId(Integer pageNum, Integer pageSize, int userId){
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
        adminContext.removeAdminId();
        return userService.getUserFilesByUserId(pageNum, pageSize, userId);
    }
}
