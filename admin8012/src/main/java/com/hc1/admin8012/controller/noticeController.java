package com.hc1.admin8012.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.notice;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.service.noticeService;
import com.hc1.admin8012.service.UserFeignClientService;
import com.hc1.admin8012.util.RedisUtil;
import com.hc1.admin8012.util.adminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    @Autowired
    private UserFeignClientService userFeignClientService;

    @PostMapping("/insertNotice")
    public CommonResult<?> insertNotice(@RequestBody notice notice) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        if(notice.getTitle() == null || notice.getTitle().trim().isEmpty())
            return CommonResult.error("标题不能为空");
        if(notice.getContent() == null || notice.getContent().trim().isEmpty())
            return CommonResult.error("内容不能为空");
        noticeService.insertNotice(notice.getTitle(), notice.getContent());
        noticeService.refreshNoticeListCache();
        // 通过 Redis 广播通知所有在线用户
        Map<String, Object> noticeMsg = new HashMap<>();
        noticeMsg.put("title", notice.getTitle());
        noticeMsg.put("content", notice.getContent());
        noticeMsg.put("type", 3);
        redisUtil.publish("notice:broadcast", noticeMsg);
        // 同步写入一条全局消息（isGlobal=1），所有用户在 message_info 表也能查到
        userFeignClientService.broadcastNotice(notice.getTitle(), notice.getContent());
        return CommonResult.success("发布成功");
    }

    @GetMapping("/queryNoticeByAddition")
    public CommonResult<?> queryNoticeByAddition(@RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "startDate", required = false) String startDate,
                                                 @RequestParam(value = "endDate", required = false) String endDate,
                                                 @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        return CommonResult.success(noticeService.queryNoticeByAddition(keyword, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/queryNotice")
    public CommonResult<?> queryNotice(@RequestParam("noticeId") int noticeId) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        return noticeService.queryNotice(noticeId);
    }
    @GetMapping("/queryNoticeList")
    public CommonResult<?> queryNoticeList(
                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        Page<notice> noticeList = noticeService.queryNoticeList(pageNum, pageSize);
        return CommonResult.success(noticeList);
    }
    @DeleteMapping("/deleteNotice")
    public CommonResult<?> deleteNotice(@RequestParam("noticeId") int noticeId) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        noticeService.deleteNotice(noticeId);
        noticeService.refreshNoticeListCache();
        return CommonResult.success("删除成功");
    }
    @PutMapping("/updateNotice")
    public CommonResult<?> updateNotice(@RequestParam("title") String title, @RequestParam("content") String content, @RequestParam("noticeId") int noticeId) {
        int adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if (!adminNames.contains("公告管理"))
            return CommonResult.error("无权限");
        noticeService.updateNotice(title, content, noticeId);
        noticeService.refreshNoticeListCache();
        return CommonResult.success("修改成功");
    }
}
