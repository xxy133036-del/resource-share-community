package com.hc1.admin8012.service;

import com.hc1.admin8012.bean.admin;

public interface LoginService {
    admin login(String adminName, String password);
}