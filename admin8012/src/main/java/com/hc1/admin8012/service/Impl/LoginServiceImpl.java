package com.hc1.admin8012.service.Impl;

import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.mapper.LoginMapper;
import com.hc1.admin8012.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-29 10:00:00
 * @Version 1.0
 */
@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private LoginMapper loginMapper;

    @Override
    public admin login(String adminName, String password) {
        admin admin = loginMapper.selectByAdminName(adminName);
        if (admin != null && BCrypt.checkpw(password, admin.getPassword())) {
            return admin;
        }
        return null;
    }
}