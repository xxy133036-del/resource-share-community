package com.hc1.user8011.service.Impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;

import com.hc1.user8011.bean.User;
import com.hc1.user8011.mapper.RegisterMapper;
import com.hc1.user8011.service.RegisterService;
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
    public String register(User user) {
        LambdaQueryChainWrapper<User> wrapper = new LambdaQueryChainWrapper<>(registerMapper);
        long count = wrapper.eq(User::getUsername, user.getUsername()).count();
        if (count > 0) {
            return "exist";
        }
        String hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
        user.setPassword(hash);
        registerMapper.insert(user);
        return "success";
    }

    @Override
    public User selectByUsername(String username) {
        return registerMapper.selectByUsername(username);
    }

}
