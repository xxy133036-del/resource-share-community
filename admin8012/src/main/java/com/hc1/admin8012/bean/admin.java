package com.hc1.admin8012.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-26 16:17:47
 * @Version 1.0
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_admin")
public class admin {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String adminName;
    private String roleName;
    //默认密码为123456
    private String password;
    private String createTime;
    private String updateTime;
    private Integer status;
    @TableField(exist = false)
    private List<String> privileges;
}
