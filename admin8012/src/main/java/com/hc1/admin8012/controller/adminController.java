package com.hc1.admin8012.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.bean.privilege;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.util.adminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @Author xwj
 * @Description
 * @Date 2026-06-27 10:34:09
 * @Version 1.0
 */
@RestController
public class adminController {

    @Autowired
    private adminService adminService;

    @RequestMapping("/listAdmin")
    public CommonResult<?> listAdmin(
            @RequestParam(value = "keyWord", required = false) String keyWord,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize) {
        try {

            Integer adminId = adminContext.getAdminId();
            List<String> pris = adminService.queryPriByAdminId(adminId);
            if(!pris.contains("管理员管理"))
            {
                return CommonResult.error("无权限");
            }
            Page<admin> page = adminService.listAdmin(keyWord, pageNum, pageSize);
            return CommonResult.success(page);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("搜索失败：" + e.getMessage());
        }
    }


    @RequestMapping("/freeze")
    public CommonResult<?> freeze(@RequestParam("id") Integer id) {
        try {
            Integer adminId = adminContext.getAdminId();
            List<String> pris = adminService.queryPriByAdminId(adminId);
            if (!pris.contains("管理员管理")) {
                return CommonResult.error("无权限");
            }
            admin admin = new admin();
            admin.setId(id);
            admin.setStatus(2);
            adminService.updateById(admin);
            return CommonResult.success("冻结成功");
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("冻结失败：" + e.getMessage());
        }
    }
    @RequestMapping("/thaw")
    public CommonResult<?> thaw(@RequestParam("id") Integer id) {
        try {
            Integer adminId = adminContext.getAdminId();
            List<String> pris = adminService.queryPriByAdminId(adminId);
            if (!pris.contains("管理员管理")) {
                return CommonResult.error("无权限");
            }
            admin admin = new admin();
            admin.setId(id);
            admin.setStatus(1);
            adminService.updateById(admin);
            return CommonResult.success("解冻成功");
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("解冻失败：" + e.getMessage());
        }
    }

    @RequestMapping("/insertAdmin")
    public CommonResult<?> insertAdmin(@RequestBody admin admin) {
        try {
            Integer adminId = adminContext.getAdminId();
            List<String> pris = adminService.queryPriByAdminId(adminId);
            if (!pris.contains("管理员管理")) {
                return CommonResult.error("无权限");
            }
            if(admin.getAdminName() == null || admin.getAdminName().isEmpty())
                return CommonResult.error("管理员名称不可为空");
            if(admin.getRoleName() == null || admin.getRoleName().isEmpty())
                return CommonResult.error("角色名称不可为空");
            List<String> adminName = adminService.queryAdminName();
            if(adminName.contains(admin.getAdminName()))
                return CommonResult.error("管理员名称已存在");
            adminService.insertAdmin(admin);
            return CommonResult.success("添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("添加失败：" + e.getMessage());
        }
    }

    @PostMapping("/updateUser")
    public CommonResult<?> updateUser(@RequestBody admin admin) {
        int id = adminContext.getAdminId();
        admin.setId(id);
        String result = adminService.updateAdminById(admin);
        if ("格式错误".equals(result)) {
            return CommonResult.error("格式错误");
        } else if ("名称已存在".equals(result)) {
            return CommonResult.error("账号已存在");
        } else if ("信息不完整".equals(result)) {
            return CommonResult.error("信息不完整");
        }
        adminContext.removeAdminId();
        return CommonResult.success("更新成功");
    }
}
