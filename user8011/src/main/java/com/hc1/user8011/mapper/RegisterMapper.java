package com.hc1.user8011.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hc1.user8011.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegisterMapper extends BaseMapper<User> {
    @Select("select * from user_info where username = #{username}")
    User selectByUsername(String username);
}
