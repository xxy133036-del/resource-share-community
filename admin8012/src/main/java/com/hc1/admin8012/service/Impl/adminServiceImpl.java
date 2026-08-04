package com.hc1.admin8012.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.bean.privilege;
import com.hc1.admin8012.mapper.adminMapper;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-27 10:46:29
 * @Version 1.0
 */
@Service
public class adminServiceImpl implements adminService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private adminMapper adminMapper;

    @Override
    public Page<admin> listAdmin(String keyWord, String startDate, String endDate, int pageNum, int pageSize) {
        Page<admin> page = new Page<>(pageNum, pageSize);
        page = adminMapper.getListAdmin(page,keyWord, startDate, endDate);
        List<admin> adminList = page.getRecords();

        if (adminList != null && !adminList.isEmpty()) {

            // 2.1 提取所有管理员 ID 并去重
            List<Integer> adminIds = adminList.stream()
                    .map(admin::getId)
                    .distinct()
                    .collect(Collectors.toList());

            // 2.2 执行 1 次 SQL 查询（调用刚才写的注解方法），得到 List<Map>
            List<Map<String, Object>> privilegeMaps = adminMapper.queryPrivilegesByAdminIds(adminIds);

            // 2.3 将 Map 列表分组：按 admin_id 分组，把 privilege_name 收集成 List<String>
            Map<Integer, List<String>> privilegeMap = new HashMap<>();
            for (Map<String, Object> map : privilegeMaps) {
                Integer adminId = (Integer) map.get("admin_id");
                String privilegeName = (String) map.get("privilege_name");

                // 如果该管理员还没有权限列表，就 new 一个 ArrayList
                privilegeMap.computeIfAbsent(adminId, k -> new ArrayList<>()).add(privilegeName);
            }

            // 2.4 遍历管理员，把权限塞进去
            for (admin admin : adminList) {
                List<String> perms = privilegeMap.getOrDefault(admin.getId(), new ArrayList<>());
                admin.setPrivileges(perms);
            }
        }
        return page;
    }

    @Override
    public List<String> queryPriByAdminId(int adminId) {
        return adminMapper.queryPriByAdminId(adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateById(admin admin) {
        adminMapper.updateById(admin);
    }

    @Override
    public List<String> queryAdminName() {
        return adminMapper.queryAdminName();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertAdmin(admin admin) {
        List<String> privileges = admin.getPrivileges();
        admin.setPassword(BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt(10)));
        adminMapper.insertAdmin(admin);
        for (String privilegeName : privileges) {
            adminMapper.insertAdminPrivilege(admin.getId(),
            adminMapper.queryPrivilegeIdByName(privilegeName));
        }
    }

    @Override
    public String updateAdminById(admin admin) {
        try {
            String adminNameRegex = "^[a-zA-Z0-9]+$";
            String passwordRegex = "^[a-zA-Z0-9]{6,}$";
            String roleNameRegex = "^.{0,12}$";
            if (admin.getAdminName() == null || admin.getPassword() == null || admin.getRoleName() == null) {
                return "信息不完整";
            } else if (!admin.getAdminName().matches(adminNameRegex) || !admin.getPassword().matches(passwordRegex) || !admin.getRoleName().matches(roleNameRegex)) {
                return "格式错误";
            } else {
                String hash = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt(10));
                admin.setPassword(hash);
                adminMapper.updateAdminById(admin);
                return "success";
            }
        } catch (DuplicateKeyException e) {
            return "账号已存在";
        }
    }




}
