package com.hc1.admin8012.service.Impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.mapper.RegisterMapper;
import com.hc1.admin8012.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Autowired
    private RegisterMapper registerMapper;

    @Transactional
    @Override
    public String register(admin admin) {
        // 先做 null 检查，避免 NPE
        if (admin.getAdminName() == null || admin.getPassword() == null || admin.getRoleName() == null) {
            return "信息不完整";
        }
        String adminNameRegex = "^[a-zA-Z0-9]+$";
        String passwordRegex = "^[a-zA-Z0-9]{6,}$";
        String roleNameRegex = "^.{0,12}$";
        if (!admin.getAdminName().matches(adminNameRegex) || !admin.getPassword().matches(passwordRegex) || !admin.getRoleName().matches(roleNameRegex)) {
            return "格式错误";
        }
        LambdaQueryChainWrapper<admin> wrapper = new LambdaQueryChainWrapper<>(registerMapper);
        long count = wrapper.eq(com.hc1.admin8012.bean.admin::getAdminName, admin.getAdminName()).count();
        if (count > 0) {
            return "exist";
        }
        String hash = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt(10));
        admin.setPassword(hash);
        admin.setStatus(1);
        registerMapper.insert(admin);
        return "success";
    }

    @Override
    public admin selectByAdminName(String adminName) {
        return registerMapper.selectByAdminName(adminName);
    }

}