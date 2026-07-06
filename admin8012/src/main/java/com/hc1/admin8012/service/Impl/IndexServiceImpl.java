package com.hc1.admin8012.service.Impl;

import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.mapper.adminMapper;
import com.hc1.admin8012.service.IndexService;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-29 10:00:00
 * @Version 1.0
 */
@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private adminService adminService;
    @Autowired
    private adminMapper adminMapper;

    @Override
    public Map<String, Object> indexAdmin(Integer adminId) {
        HashMap<String, Object> adminAndPrivileges = new HashMap<>();
        String adminCacheKey = "admin:" + adminId;
        admin admin = (admin) redisUtil.get(adminCacheKey);
        if (admin == null) {
            admin = adminMapper.selectById(adminId);
            if (admin != null) {
                // 设置30分钟过期时间
                redisUtil.set(adminCacheKey, admin, 30 * 60);
            }
        }
        List<String> privileges = adminService.queryPriByAdminId(adminId);
        if (admin != null) {
            admin.setPrivileges(privileges);
        }
        adminAndPrivileges.put("admin", admin);
        adminAndPrivileges.put("privileges", privileges);
        return adminAndPrivileges;
    }
}