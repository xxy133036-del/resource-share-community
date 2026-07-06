package com.hc1.user8011.controller;

import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.Message;
import com.hc1.user8011.service.MessageService;
import com.hc1.user8011.util.RedisUtil;
import com.hc1.user8011.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author xwj
 * @Description 消息控制器：聚合摘要 / 分类详情 / 标记已读 / 发送消息
 * @Date 2026-06-22 11:35:23
 * @Version 1.0
 */
@RestController
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private RedisUtil redisUtil;

    // ① 聚合摘要 — 返回 3 种类型的总数+预览+未读数
    @GetMapping("/messageSummary")
    public CommonResult<?> messageSummary() {
        int userId = UserContext.getUserId();
        UserContext.removeUserId();
        return CommonResult.success(messageService.getSummary(userId));
    }

    // ② 某类型详情（分页）
    @GetMapping("/myMessages")
    public CommonResult<?> myMessages(
            @RequestParam int type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        int userId = UserContext.getUserId();
        UserContext.removeUserId();
        return CommonResult.success(messageService.getByType(userId, type, pageNum, pageSize));
    }

    // ③ 标记单条已读
    @PutMapping("/markMessageRead/{id}")
    public CommonResult<?> markRead(@PathVariable int id) {
        messageService.markRead(id);
        return CommonResult.success("ok");
    }

    // ④ 标记某类型全部已读
    @PutMapping("/markMessageReadByType")
    public CommonResult<?> markReadByType(@RequestParam int type) {
        messageService.markReadByType(UserContext.getUserId(), type);
        return CommonResult.success("ok");
    }

    // ⑤ 发送消息（点赞/评论/管理员后台调用）
    @PostMapping("/sendMessage")
    public CommonResult<?> sendMessage(@RequestBody Message message) {
        messageService.sendMessage(message);
        return CommonResult.success("发送成功");
    }

    // ⑥ 获取最新公告（由 Redis 监听器缓存，无需查 message_info 表）
    @GetMapping("/latestNotice")
    public CommonResult<?> latestNotice() {
        Object notice = redisUtil.get("notice:latest");
        if (notice == null) {
            return CommonResult.success(null);
        }
        return CommonResult.success(notice);
    }
}
