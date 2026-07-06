package com.hc1.admin8012.bean;

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
 * @Date 2026-06-29 16:12:08
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@TableName("sys_notice")
public class notice {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String content;
    private String createTime;
    private String updateTime;
}
