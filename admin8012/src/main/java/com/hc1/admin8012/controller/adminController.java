package com.hc1.admin8012.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.bean.privilege;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.util.adminContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(adminController.class);

    @Autowired
    private adminService adminService;

    @GetMapping("/listAdmin")
    public CommonResult<?> listAdmin(
            @RequestParam(value = "keyWord", required = false) String keyWord,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize) {
        try {

            Integer adminId = adminContext.getAdminId();
            List<String> pris = adminService.queryPriByAdminId(adminId);
            if(!pris.contains("管理员管理"))
            {
                return CommonResult.error("无权限");
            }
            Page<admin> page = adminService.listAdmin(keyWord,startDate,endDate, pageNum, pageSize);
            return CommonResult.success(page);
        } catch (Exception e) {
            log.error("搜索管理员列表失败", e);
            return CommonResult.error("搜索失败：" + e.getMessage());
        }
    }


    @PutMapping("/freeze")
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
            log.error("冻结管理员失败, id={}", id, e);
            return CommonResult.error("冻结失败：" + e.getMessage());
        }
    }
    @PutMapping("/thaw")
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
            log.error("解冻管理员失败, id={}", id, e);
            return CommonResult.error("解冻失败：" + e.getMessage());
        }
    }

    @PostMapping("/insertAdmin")
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
            log.error("添加管理员失败", e);
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
        return CommonResult.success("更新成功");
    }
}
