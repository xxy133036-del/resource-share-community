package com.hc1.admin8012.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hc1.admin8012.bean.admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginMapper extends BaseMapper<admin> {
    @Select("select * from sys_admin where admin_name = #{adminName}")
    admin selectByAdminName(String adminName);
}