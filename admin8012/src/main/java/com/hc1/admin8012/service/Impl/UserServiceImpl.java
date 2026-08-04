package com.hc1.admin8012.service.Impl;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.service.UserFeignClientService;
import com.hc1.admin8012.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-27 10:46:29
 * @Version 1.0
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserFeignClientService UserFeignClientService;
    @Override
    public CommonResult<?> updateUserStatusById(int status, int userId) {
        return UserFeignClientService.updateUserStatusById(status, userId);
    }

    @Override
    public CommonResult<?> getUserById(int userId) {
        return UserFeignClientService.getUserById(userId);
    }

    @Override
    public CommonResult<?> getUserFilesByUserId(Integer pageNum, Integer pageSize, int userId) {
        return UserFeignClientService.getUserFilesByUserId(pageNum, pageSize, userId);
    }

   /* @Override
    public CommonResult<?> getUserList(Integer pageNum, Integer pageSize) {
        return UserFeignClientService.getUserList(pageNum, pageSize);
    }*/

    @Override
    public CommonResult<?> getUserListByAdditions(String keyword, Integer status, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        return UserFeignClientService.getUserListByAdditions(keyword, status, startDate, endDate, pageNum, pageSize);
    }
}
