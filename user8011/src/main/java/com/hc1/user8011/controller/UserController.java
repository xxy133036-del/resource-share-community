package com.hc1.user8011.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.AdminCallContext;
import com.hc1.user8011.util.RedisUtil;
import com.hc1.user8011.util.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-07 17:11:04
 * @Version 1.0
 */
@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private RedisUtil redisUtil;
    private static final String UPLOADHEAD_URL = "D:/java/file/head/";
    @Autowired
    private UserService userService;

    // 上传头像
    @PostMapping("/uploadHeadImg")
    public CommonResult<?> headimg(
            @RequestParam("headimg") MultipartFile headimg) {
        int id = UserContext.getUserId();
        try {

            if (headimg == null || headimg.isEmpty()) {
                return CommonResult.error("请选择要上传的头像");
            }

            // 1. 生成唯一文件名 (雪花ID/UUID + 原始文件名)
            String originalHeadname = headimg.getOriginalFilename();
            String uniqueHeadName = IdUtil.getSnowflakeNextIdStr() + "_" + originalHeadname;
            java.io.File destFile = new java.io.File(UPLOADHEAD_URL + uniqueHeadName);

            // 2. 将文件流写入本地磁盘
            headimg.transferTo(destFile);

            userService.updateUserAvatarById(id, uniqueHeadName);
            return CommonResult.success("成功上传头像！");

        } catch (Exception e) {
            log.error("上传头像失败, userId={}", id, e);
            return CommonResult.error("服务器存储异常：" + e.getMessage());
        }
    }

    @PostMapping("/updateUser")
    public CommonResult<?> updateUser(@RequestBody User user) {
        int id = UserContext.getUserId();
        user.setId(id);
        String result = userService.updateUserById(user);
        if ("格式错误".equals(result)) {
            return CommonResult.error("格式错误");
        } else if ("账号已存在".equals(result)) {
            return CommonResult.error("账号已存在");
        } else if ("信息不完整".equals(result)) {
            return CommonResult.error("信息不完整");
        }
        return CommonResult.success("更新成功");
    }

    @PostMapping("/dailyCheck")
    public CommonResult<?> dailyCheck() {
        int id = UserContext.getUserId();
        int streakDays = userService.doCheckIn(id);
        int points = 0;
        if (streakDays == -1) {
            return CommonResult.error("您今天已经签到过啦！");
        } else if (streakDays < 3 && streakDays >= 1) {
            userService.addPointsOneById(id);
            points = 1;
        } else if (streakDays >= 3) {
            userService.addPointsFiveById(id);
            points = 5;
        }
        return CommonResult.success("已连续签到 " + streakDays + " 天,获得 " + points + " 积分");
    }

    @PostMapping("/addLike")
    public CommonResult<?> addLike(@RequestParam("fileId") int fileId) {
        int userId = UserContext.getUserId();
        String result = userService.addLike(fileId, userId);
        if ("文件不存在".equals(result)) {
            return CommonResult.error("文件不存在");
        } else if ("已点赞".equals(result)) {
            return CommonResult.error("已点赞");
        }
        return CommonResult.success("点赞成功");
    }

    @PostMapping("/removeLike")
    public CommonResult<?> removeLike(@RequestParam("fileId") int fileId) {
        int userId = UserContext.getUserId();
        String result = userService.removeLike(fileId, userId);
        if ("未点赞".equals(result)) {
            return CommonResult.error("未点赞");
        }
        return CommonResult.success("取消点赞成功");
    }

    @GetMapping("/checkLike")
    public CommonResult<?> checkLike(@RequestParam("fileId") int fileId) {
        int userId = UserContext.getUserId();
        boolean liked = userService.checkLike(fileId, userId);
        return CommonResult.success(liked);
    }

    @PutMapping("/updateUserStatus")
    public CommonResult<?> updateUserStatusById(@RequestParam("status") int status, @RequestParam("userId") int userId) {
        if (!AdminCallContext.isAdminCall())
            return CommonResult.error("无权限");
       userService.updateUserStatusById(status, userId);
       return CommonResult.success("修改成功");
    }

    @GetMapping("/getUserById")
    public CommonResult<?> getUserById(@RequestParam("userId") int userId) {
        return CommonResult.success(userService.getById(userId));
    }

    @GetMapping("/getUserListByAdditions")
    public CommonResult<?> getUserListByAdditions(@RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "status", required = false) Integer status,
                                                  @RequestParam(value = "startDate", required = false) String startDate,
                                                  @RequestParam(value = "endDate", required = false) String endDate,
                                                  @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        if (!AdminCallContext.isAdminCall())
            return CommonResult.error("无权限");
        Page<User> userList = userService.getUserListByAdditions(keyword, status, startDate, endDate, pageNum, pageSize);
        return CommonResult.success(userList);
    }
}
