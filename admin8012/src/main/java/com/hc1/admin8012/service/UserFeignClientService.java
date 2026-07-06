package com.hc1.admin8012.service;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.MessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user8011", contextId = "userFeignClient", path = "/user8011")
public interface UserFeignClientService {
    @RequestMapping("/updateUserStatus")
    public CommonResult<?> updateUserStatusById(@RequestParam("status") int status, @RequestParam("userId") int userId);
    @RequestMapping("/getUserById")
    public CommonResult<?> getUserById(@RequestParam("userId") int userId);
    @GetMapping("/getUserFilesByUserId")
    public CommonResult<?> getUserFilesByUserId(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize,
                                                @RequestParam("userId") int userId);
}
