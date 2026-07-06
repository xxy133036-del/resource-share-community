package com.hc1.user8011.controller;

import com.hc1.user8011.bean.Comment;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.service.CommentService;
import com.hc1.user8011.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping("/commentResources")
    public CommonResult<?> commentResources(@RequestBody Comment comment) {
        if (comment.getContent() == null) {
            return CommonResult.error("评论内容不能为空");
        }

            Integer userId = UserContext.getUserId();
            comment.setUserId(userId);
            UserContext.removeUserId();
        return commentService.commentResources(comment);
    }

    @RequestMapping("/commentComment")
    public CommonResult<?> commentComment(@RequestBody Comment comment) {
        if (comment.getContent() == null) {
            return CommonResult.error("评论内容不能为空");
        }
        Integer userId = UserContext.getUserId();
        comment.setUserId(userId);
        UserContext.removeUserId();
        return commentService.commentComment(comment);
    }

    @RequestMapping("/getCommentListWithAuthor")
    public CommonResult<?> getCommentListWithAuthor( @RequestParam(required = false) Integer targetId,
                                                     @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return CommonResult.success(commentService.getCommentListWithAuthor(targetId, pageNum, pageSize));
    }
}
