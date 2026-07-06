package com.hc1.user8011.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.Comment;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.File;

public interface CommentService {

    CommonResult<?> commentResources(Comment comment);
    CommonResult<?> commentComment(Comment comment);

    Page<Comment> getCommentListWithAuthor(Integer targetId, int pageNum, int pageSize);
}
