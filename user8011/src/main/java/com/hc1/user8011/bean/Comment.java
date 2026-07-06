package com.hc1.user8011.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-20 13:21:59
 * @Version 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer targetId;
    private Integer userId;
    private String content;
    private Integer parentId;
    private String createTime;
    @TableField(exist = false)
    private String authorName;   // 评论者昵称
    @TableField(exist = false)
    private String authorAvatar; // 评论者头像
}
