package com.hc1.admin8012.service;


import com.hc1.admin8012.bean.CommonResult;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserService {
   CommonResult<?> updateUserStatusById(int status,int userId);
   CommonResult<?> getUserById(int userId);
   CommonResult<?> getUserFilesByUserId(Integer pageNum,  Integer pageSize,int userId);
}