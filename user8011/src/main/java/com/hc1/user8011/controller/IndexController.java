package com.hc1.user8011.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.service.IndexService;
import com.hc1.user8011.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * @Author xwj
 * @Description
 * @Date 2026-06-18 13:05:46
 * @Version 1.0
 */
@RestController
public class IndexController {
    @Autowired
    private IndexService indexService;
    @GetMapping("/home")
    public CommonResult<?> home(@RequestParam(required = false) String storage,
                                @RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "12") int pageSize) {
        Integer userId = UserContext.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.error(401, "用户未登录或登录已失效，请重新登录");
        }
        Map<String,Object> userAndCheckInStatus = indexService.indexUser(userId);
        Map<String,Object> result = new HashMap<>();
        result.put("userAndCheckInStatus", userAndCheckInStatus);
        // storage 为空时不查询文件列表（如 Profile/MainLayout 仅需用户信息）
        if (storage != null && !storage.isEmpty()) {
            result.put("files", indexService.indexFiles(storage, pageNum, pageSize, userId));
        } else {
            result.put("files", null);
        }
        UserContext.removeUserId();
        return CommonResult.success(result);
    }
}
