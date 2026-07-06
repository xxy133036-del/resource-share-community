package com.hc1.user8011.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hc1.user8011.bean.Comment;
import com.hc1.user8011.bean.File;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Insert("insert into comment (target_id,user_id, content, parent_id) values (#{targetId}, #{userId}, #{content}, #{parentId})")
    void insertComment(Comment comment);

    @Select("select * from comment where target_id = #{resourceId}")
    List<Comment> selectAllComment(int resourceId);
}
