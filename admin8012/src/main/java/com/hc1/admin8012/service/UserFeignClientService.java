package com.hc1.admin8012.service;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.MessageDTO;
import com.hc1.admin8012.service.Impl.UserServiceImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user8011", contextId = "userFeignClient", path = "/user8011")
public interface UserFeignClientService {
    @PutMapping("/updateUserStatus")
    public CommonResult<?> updateUserStatusById(@RequestParam("status") int status, @RequestParam("userId") int userId);
    @GetMapping("/getUserById")
    public CommonResult<?> getUserById(@RequestParam("userId") int userId);
    @GetMapping("/getUserFilesByUserId")
    public CommonResult<?> getUserFilesByUserId(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize,
                                                @RequestParam("userId") int userId);
/*    @RequestMapping("/getUserList")
    public CommonResult<?> getUserList(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize);*/
    @GetMapping("/getUserListByAdditions")
    public CommonResult<?> getUserListByAdditions(@RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "status", required = false) Integer status,
                                                   @RequestParam(value = "startDate", required = false) String startDate,
                                                   @RequestParam(value = "endDate", required = false) String endDate,
                                                   @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize);

    /** → user8011 POST /broadcastNotice（管理员发公告后插入一条全局消息） */
    @PostMapping("/broadcastNotice")
    CommonResult<?> broadcastNotice(@RequestParam("title") String title, @RequestParam("content") String content);
}
