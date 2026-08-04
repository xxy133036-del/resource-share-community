package com.hc1.user8011.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hc1.user8011.bean.Comment;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.service.CommentService;
import com.hc1.user8011.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


/**
 * @Author xwj
 * @Description
 * @Date 2026-06-19 14:10:53
 * @Version 1.0
 */
@RestController
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/commentResources")
    @SentinelResource(value = "commentResources", blockHandler = "commentBlockHandler")
    public CommonResult<?> commentResources(@RequestBody Comment comment) {
        if (comment.getContent() == null) {
            return CommonResult.error("评论内容不能为空");
        }

        Integer userId = UserContext.getUserId();
        comment.setUserId(userId);
        return commentService.commentResources(comment);
    }

    @PostMapping("/commentComment")
    @SentinelResource(value = "commentComment", blockHandler = "commentBlockHandler")
    public CommonResult<?> commentComment(@RequestBody Comment comment) {
        if (comment.getContent() == null) {
            return CommonResult.error("评论内容不能为空");
        }
        Integer userId = UserContext.getUserId();
        comment.setUserId(userId);
        return commentService.commentComment(comment);
    }

    @GetMapping("/getCommentListWithAuthor")
    public CommonResult<?> getCommentListWithAuthor(@RequestParam(required = false) Integer targetId,
                                                    @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                    @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return CommonResult.success(commentService.getCommentListWithAuthor(targetId, pageNum, pageSize));
    }

    public CommonResult<?> commentBlockHandler(Comment comment, BlockException e) {
        return CommonResult.error("系统繁忙 已降级/限流 "+ e.getClass().getCanonicalName());
    }
}
