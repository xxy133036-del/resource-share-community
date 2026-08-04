package com.hc1.user8011.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.File;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<File> {

    @Insert("insert into file_info (file_name,description,file_url,user_id,tags) values (#{file.fileName},#{file.description},#{file.fileUrl},#{userid},#{file.tags})")
    int insert(@Param("file") File file,@Param("userid") int userid);

    @Update("update file_info set downloads_count = downloads_count + #{count} where file_url = #{fileUrl}")
    void addDownloadCountBatch(@Param("fileUrl") String fileUrl,@Param("count") int count);

    @Select("select * from file_info where file_url = #{fileUrl}")
    File selectByFileUrl(String fileUrl);

    @Update("update file_info set comments_count = comments_count + #{count} where id = #{id}")
    void addCommentsCountBatch(@Param("id") int id,@Param("count") int count);

    @Update("update file_info set likes_count = likes_count + #{count} where id = #{id}")
    void addLikesCountBatch(@Param("id") int id,@Param("count") int count);

    /** 模糊搜索：标题、标签、作者昵称，支持日期范围、状态筛选、排序 */
    @Select("<script>" +
            "SELECT f.* FROM file_info f " +
            "LEFT JOIN user_info u ON f.user_id = u.id " +
            "<where>" +
            "<if test='userId != null'>" +
            "AND f.user_id = #{userId}" +
            "</if>" +
            "<if test='status != null'>" +
            "AND f.status = #{status} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (f.file_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR f.tags LIKE CONCAT('%',#{keyword},'%') " +
            "OR u.nickname LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND f.create_time &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND f.create_time &lt;= CONCAT(#{endDate}, ' 23:59:59') " +
            "</if>" +
            "</where>" +
            "<choose>" +
            "<when test='sortType == \"hottest\"'>" +
            "ORDER BY (f.downloads_count * 3 + f.likes_count * 2 + f.comments_count) DESC" +
            "</when>" +
            "<otherwise>" +
            "ORDER BY f.update_time DESC" +
            "</otherwise>" +
            "</choose>" +
            "</script>")
    Page<File> searchFiles(IPage<?> page,
                          @Param("keyword") String keyword,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate,
                          @Param("status") Integer status,
                          @Param("sortType") String sortType,
                           @Param("userId") Integer userId);

    @Update("update file_info set status = #{status} where file_url = #{fileUrl}")
    void updateStatus(@Param("fileUrl") String fileUrl,@Param("status") int status);
}
