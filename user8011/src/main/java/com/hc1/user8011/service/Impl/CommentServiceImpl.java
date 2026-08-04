package com.hc1.user8011.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.Comment;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.bean.Message;
import com.hc1.user8011.mapper.CommentMapper;
import com.hc1.user8011.mapper.FileMapper;
import com.hc1.user8011.service.CommentService;
import com.hc1.user8011.service.MessageService;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-19 14:11:29
 * @Version 1.0
 */
@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private MessageService messageService;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CommonResult<?> commentResources(Comment comment) {
        // 存到comment表
        comment.setParentId(0);
        commentMapper.insertComment(comment);
        // 资源 评论量 +1
        Integer fileId = comment.getTargetId();
        redisUtil.hincr("file:comments:cache", String.valueOf(fileId), 1);
        // 发送消息通知资源作者
        sendCommentNotification(comment);

        return CommonResult.success("评论成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CommonResult<?> commentComment(Comment comment) {
        // 存到comment表
        commentMapper.insertComment(comment);
        // 资源 评论量 +1
        Integer fileId = comment.getTargetId();
        redisUtil.hincr("file:comments:cache", String.valueOf(fileId), 1);
        // 发送消息通知资源作者
        sendCommentNotification(comment);

        return CommonResult.success("评论成功");
    }

    /**
     * 评论后通知资源作者（评论者本人不通知自己）
     */
    private void sendCommentNotification(Comment comment) {
        try {
            Integer commenterId = comment.getUserId();
            Integer resourceId = comment.getTargetId();

            // 查找资源
            File resource = fileMapper.selectById(resourceId);
            if (resource == null) return;
            Integer authorId = resource.getUserid();

            // 自己评论自己的资源，不发通知
            if (authorId.equals(commenterId)) return;

            // 查找评论者昵称
            User commenter = userService.selectById(commenterId);
            String commenterName = commenter != null ? commenter.getNickname() : "用户";

            // 查找资源名称
            String resourceName = resource.getFileName();
            if (resourceName == null || resourceName.isEmpty()) resourceName = "资源" + resourceId;

            // 构造消息（评论类型：无 title，仅 content；targetId 用于前端点击跳转）
            Message message = new Message();
            message.setUserId(authorId);
            message.setSenderId(commenterId);
            message.setSenderName(commenterName);
            message.setType(2);  // 评论消息
            message.setContent(commenterName + "评论了您的资源《" + resourceName + "》");
            message.setTargetId(resourceId);  // 点击跳转到该资源
            message.setCommentId(comment.getId());  // 定位到该评论

            messageService.sendMessage(message);
        } catch (Exception e) {
            // 通知失败不影响主流程
            log.error("评论通知发送失败", e);
        }
    }



    @Override
    public Page<Comment> getCommentListWithAuthor(Integer targetId, int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        if (targetId != null) {
            wrapper.eq("target_id", targetId);
        }
        wrapper.orderByDesc("create_time");
        commentMapper.selectPage(page, wrapper);
        List<Comment> commentList = page.getRecords();
        if (commentList == null || commentList.isEmpty()) {
            return page;
        }

        // 2. 提取出当前页所有文件的 userid 并去重（例如：[1001, 1002]）
        List<Integer> userIds = commentList.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量一次性查出这些用户的信息
        List<User> users = userService.listByIds(userIds);

        // 转成 Map 结构方便快速匹配：Key是userId, Value是User对象
        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. 把查出来的作者名字和头像，塞回 comment 对象的非数据库字段中
        for (Comment comment : commentList) {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setAuthorName(user.getNickname());
                comment.setAuthorAvatar(user.getAvatar());
            }
        }

        return page;
    }
}
