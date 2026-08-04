package com.hc1.user8011.service.Impl;

import com.hc1.user8011.bean.User;
import com.hc1.user8011.mapper.LoginMapper;
import com.hc1.user8011.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-10 11:42:15
 * @Version 1.0
 */
@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private LoginMapper loginMapper;
    @Override
    public User login( String username,  String password) {
        User user = loginMapper.selectByUsername(username);
        if(user != null && BCrypt.checkpw(password, user.getPassword()))
        {
            return user;
        }

        return null;
    }
}
