package com.hc1.admin8012.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author xwj
 * @Description 评论 DTO（admin8012 通过 Feign 调用 user8011 获取评论列表）
 * @Date 2026-07-07
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommentDTO {
    private Integer id;
    private Integer targetId;
    private Integer userId;
    private String content;
    private Integer parentId;
    private String createTime;
    private String authorName;   // 评论者昵称
    private String authorAvatar; // 评论者头像
}
