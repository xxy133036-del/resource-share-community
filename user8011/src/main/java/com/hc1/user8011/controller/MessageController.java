package com.hc1.user8011.controller;

import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.Message;
import com.hc1.user8011.mapper.MessageMapper;
import com.hc1.user8011.service.MessageService;
import com.hc1.user8011.util.RedisUtil;
import com.hc1.user8011.util.UserContext;
import java.util.List;
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
    @Autowired
    private MessageMapper messageMapper;

    // ① 聚合摘要 — 返回 3 种类型的总数+预览+未读数
    @GetMapping("/messageSummary")
    public CommonResult<?> messageSummary() {
        int userId = UserContext.getUserId();
        return CommonResult.success(messageService.getSummary(userId));
    }

    // ② 某类型详情（分页）
    @GetMapping("/myMessages")
    public CommonResult<?> myMessages(
            @RequestParam int type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        int userId = UserContext.getUserId();
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

    // ⑦ 获取公告列表（直接从 message_info 表查 is_global=1 的记录）
    @GetMapping("/queryNoticeList")
    public CommonResult<?> queryNoticeList() {
        List<Message> list = messageMapper.selectGlobalNotices();
        return CommonResult.success(list != null ? list : new java.util.ArrayList<>());
    }

    // ⑧ 管理员广播公告：写入一条全局消息（isGlobal=1），所有用户可见
    @PostMapping("/broadcastNotice")
    public CommonResult<?> broadcastNotice(@RequestParam String title, @RequestParam String content) {
        Message notice = new Message();
        notice.setType(3);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setSenderName("系统管理员");
        notice.setIsGlobal(1);
        notice.setStatus(0);
        messageService.sendMessage(notice);
        return CommonResult.success("公告已广播");
    }
}
