package com.hc1.admin8012.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.notice;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.service.noticeService;
import com.hc1.admin8012.util.RedisUtil;
import com.hc1.admin8012.util.adminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-27 10:34:19
 * @Version 1.0
 */
@RestController
public class noticeController {

    @Autowired
    private noticeService noticeService;
    @Autowired
    private adminService adminService;
    @Autowired
    private RedisUtil redisUtil;

    @RequestMapping("/insertNotice")
    public CommonResult<?> insertNotice(String title, String content) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        if(title == null || title.trim().isEmpty())
            return CommonResult.error("标题不能为空");
        if(content == null || content.trim().isEmpty())
            return CommonResult.error("内容不能为空");
        noticeService.insertNotice(title, content);
        // 通过 Redis 广播通知所有在线用户
        Map<String, Object> noticeMsg = new HashMap<>();
        noticeMsg.put("title", title);
        noticeMsg.put("content", content);
        noticeMsg.put("type", 3);
        redisUtil.publish("notice:broadcast", noticeMsg);
        adminContext.removeAdminId();
        return CommonResult.success("发布成功");
    }

    @RequestMapping("/queryNoticeByAddition")
    public CommonResult<?> queryNoticeByAddition(@RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "startDate", required = false) String startDate,
                                                 @RequestParam(value = "endDate", required = false) String endDate,
                                                 @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        adminContext.removeAdminId();
        return CommonResult.success(noticeService.queryNoticeByAddition(keyword, startDate, endDate, pageNum, pageSize));
    }

    @RequestMapping("/queryNotice")
    public CommonResult<?> queryNotice(@RequestParam("noticeId") int noticeId) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        CommonResult<?> result = noticeService.queryNotice(noticeId);
        adminContext.removeAdminId();
        return result;
    }
    @RequestMapping("/queryNoticeList")
    public CommonResult<?> queryNotice(
                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        Page<notice> noticeList = noticeService.queryNoticeList(pageNum, pageSize);
        adminContext.removeAdminId();
        return CommonResult.success(noticeList);
    }
    @RequestMapping("/deleteNotice")
    public CommonResult<?> deleteNotice(@RequestParam("noticeId") int noticeId) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        noticeService.deleteNotice(noticeId);
        adminContext.removeAdminId();
        return CommonResult.success("删除成功");
    }
    @RequestMapping("/updateNotice")
    public CommonResult<?> updateNotice(String title, String content, @RequestParam("noticeId") int noticeId) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if (!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        noticeService.updateNotice(title, content, noticeId);
        adminContext.removeAdminId();
        return CommonResult.success("修改成功");
    }
}
