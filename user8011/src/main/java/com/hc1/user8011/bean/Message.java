package com.hc1.user8011.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-22 11:37:15
 * @Version 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
@TableName("message_info")
public class Message {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;       // 接收者
    private Integer senderId;     // 发送者
    private String senderName;    // 发送者昵称
    private Integer type;         // 1=点赞 2=评论 3=系统公告
    private String content;       // 消息正文
    private Integer targetId;     // 关联资源ID（评论/点赞跳转用）
    private Integer commentId;    // 关联评论ID（评论消息定位用）
    private String createTime;
    private Integer status;       // 0=未读 1=已读
    private String title; // 公告使用
    private Integer isGlobal; // 0=个人消息 1=全局公告（所有用户可见）
}
