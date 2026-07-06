package com.hc1.user8011.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigInteger;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-11 15:14:49
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@TableName("file_info")
public class File {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String fileName;
    private String description;
    private String fileUrl;
    @TableField("user_id")
    private Integer userid;
    private String createTime;
    private String updateTime;
    private Integer status; //0：审核中（默认） 1：审核未通过 2：审核通过
    private Integer likesCount;
    private Integer commentsCount;
    private Integer downloadsCount;
    private String tags;           // 标签，逗号分隔存储
    @TableField(exist = false)
    private String authorName;   // 作者昵称
    @TableField(exist = false)
    private String authorAvatar; // 作者头像
    @TableField(exist = false)
    private Boolean liked;       // 当前用户是否已点赞
    @TableField(exist = false)
    private Integer hotScore;    // 热度分 = 下载*3 + 点赞*2 + 评论*1

}
