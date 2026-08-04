package com.hc1.admin8012.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.notice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface noticeMapper extends BaseMapper<notice> {
    @Insert("insert into sys_notice (title,content) values (#{title},#{content})")
    void insertNotice(String title, String content);

    @Select("select * from sys_notice where id = #{noticeId}")
    notice queryNotice(int noticeId);

    @Update("update sys_notice set title = #{title}, content = #{content},update_time = now() where id = #{noticeId}")
    void updateNoticeById(String title, String content, int noticeId);

    @Select("<script>" +
            "SELECT n.* FROM sys_notice n " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (n.title LIKE CONCAT('%',#{keyword},'%') " +
            "OR n.content LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND n.create_time &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND n.create_time &lt;= CONCAT(#{endDate}, ' 23:59:59') " +
            "</if>" +
            "</where>" +
            "ORDER BY n.update_time DESC" +
            "</script>")
    IPage<notice> queryNoticeByAddition(IPage<notice> page,String keyword, String startDate, String endDate);
}
