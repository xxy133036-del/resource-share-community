package com.hc1.user8011.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author xwj
 * @Description 消息聚合摘要：每种类型合并为一行，显示总数+预览+未读数
 * @Date 2026-06-22 20:46:05
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MessageSummary {
    private int type;           // 1=点赞 2=评论 3=系统公告
    private String typeName;    // "点赞消息" / "评论消息" / "系统公告"
    private int total;          // 该类型消息总数
    private String firstSender; // 最新一条的发送者昵称（如"user001"）
    private int unreadCount;    // 该类型未读数
}