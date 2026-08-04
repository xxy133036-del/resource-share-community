package com.hc1.admin8012.controller;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.service.UserService;
import com.hc1.admin8012.util.adminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    @PutMapping("/updateUserStatusById")
    public CommonResult<?> updateUserStatusById(@RequestParam("status") int status, @RequestParam("userId") int userId){
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
       return userService.updateUserStatusById(status, userId);
    }

    @GetMapping("/getUserById")
    public CommonResult<?> getUserById(@RequestParam("userId") int userId){
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
        return userService.getUserById(userId);
    }

    @GetMapping("/getUserFilesByUserId")
    public CommonResult<?> getUserFilesByUserId(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize,
                                                @RequestParam("userId") int userId){
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
        return userService.getUserFilesByUserId(pageNum, pageSize, userId);
    }

    @GetMapping("/getUserListByAdditions")
    public CommonResult<?> getUserListByAdditions(@RequestParam(value = "keyword", required = false ) String keyword,
                                                  @RequestParam(value = "status",required = false ) Integer status,
                                                  @RequestParam(value = "startDate", required = false )String startDate,
                                                  @RequestParam(value = "endDate", required = false )String endDate,
                                                   @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if (!adminNames.contains("用户管理"))
            return CommonResult.error("无权限");
        return userService.getUserListByAdditions(keyword, status, startDate, endDate, pageNum, pageSize);
    }

}
