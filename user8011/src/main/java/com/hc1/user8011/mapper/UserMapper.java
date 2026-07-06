package com.hc1.user8011.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from user_info where id = #{id}")
    User selectById(Integer id);

    @Update("update user_info set avatar = #{avatar} where id = #{id}")
    void updateUserAvatarById(@Param("id")int id, @Param("avatar") String avatar);

    @Update("update user_info set username = #{username},password = #{password},nickname = #{nickname} where id = #{id}")
    void updateUserById(User user);

    @Update("update user_info set points = points+1 where id = #{id}")
    void addPointsOneById(int id);

    @Update("update user_info set points = points+5 where id = #{id}")
    void addPointsFiveById(int id);

    @Update("update user_info set points = points-5 where id = #{id}")
    void reducePointsFiveById(int id);

    @Update("update user_info set points = points + #{points} where id = #{userid}")
    void addPointsBatch(@Param("userid") int userid,@Param("points") int points);

    @Update("update user_info set status = #{status} where id = #{userId}")
    void updateUserStatusById(int status, int userId);
}
