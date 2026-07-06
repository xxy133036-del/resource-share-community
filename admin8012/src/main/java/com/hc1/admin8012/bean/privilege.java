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
 * @Date 2026-06-26 16:21:02
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@TableName("sys_privilege")
public class privilege {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String roleName;
    private String createTime;
    private String updateTime;

}
