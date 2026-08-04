package com.hc1.user8011.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hc1.user8011.bean.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT type, COUNT(*) AS total, " +
            " (SELECT m2.sender_name FROM message_info m2 " +
            "  WHERE (m2.user_id = #{userId} OR m2.is_global = 1) AND m2.type = m1.type " +
            "  ORDER BY m2.create_time DESC LIMIT 1) AS firstSender " +
            "FROM message_info m1 " +
            "WHERE (m1.user_id = #{userId} OR m1.is_global = 1) " +
            "GROUP BY m1.type")
    List<Map<String, Object>> selectSummary(@Param("userId") int userId);

    @Select("SELECT * FROM message_info " +
            "WHERE (user_id = #{userId} OR is_global = 1) AND type = #{type} " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Message> selectByType(@Param("userId") int userId,
                               @Param("type") int type,
                               @Param("offset") int offset,
                               @Param("size") int size);

    @Select("SELECT COUNT(*) FROM message_info " +
            "WHERE (user_id = #{userId} OR is_global = 1) AND type = #{type}")
    int countByType(@Param("userId") int userId, @Param("type") int type);

    @Select("SELECT type, COUNT(*) FROM message_info " +
            "WHERE (user_id = #{userId} OR is_global = 1) AND status = 0 GROUP BY type")
    List<Map<String, Object>> countUnreadByType(@Param("userId") int userId);

    @Update("UPDATE message_info SET status = 1 WHERE id = #{id}")
    void markRead(@Param("id") int id);

    @Update("UPDATE message_info SET status = 1 " +
            "WHERE user_id = #{userId} AND type = #{type}")
    void markReadByType(@Param("userId") int userId, @Param("type") int type);

    @Insert("INSERT INTO message_info (user_id, sender_id, sender_name, type, title, content, target_id, comment_id, status, is_global) " +
            "VALUES (#{userId}, #{senderId}, #{senderName}, #{type}, #{title}, #{content}, #{targetId}, #{commentId}, 0, #{isGlobal})")
    void insertMessage(Message message);

    @Select("SELECT * FROM message_info WHERE is_global = 1 ORDER BY create_time DESC LIMIT 10")
    List<Message> selectGlobalNotices();






}
