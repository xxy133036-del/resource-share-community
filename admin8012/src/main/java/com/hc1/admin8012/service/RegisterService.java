package com.hc1.admin8012.service;

import com.hc1.admin8012.bean.admin;

public interface RegisterService {

    String register(admin admin);

    admin selectByAdminName(String adminName);

}