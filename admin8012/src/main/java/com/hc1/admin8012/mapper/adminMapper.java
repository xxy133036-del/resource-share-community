package com.hc1.admin8012.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.admin;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface adminMapper extends BaseMapper<admin> {


    @Select("SELECT p.privilege_name " +
            "FROM sys_admin_privilege ap " +
            "LEFT JOIN sys_privilege p ON ap.privilege_id = p.id " +
            "WHERE ap.admin_id = #{adminId}")
    List<String> queryPriByAdminId(int adminId);

    @Select("<script>" +
            "SELECT a.* FROM sys_admin a WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.admin_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR a.role_name LIKE CONCAT('%',#{keyword},'%') " +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND a.create_time &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND a.create_time &lt;= CONCAT(#{endDate}, ' 23:59:59') " +
            "</if>" +
            "ORDER BY a.update_time DESC" +
            "</script>")
    Page<admin> getListAdmin(@Param("keyword") String keyword,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("pageNum") int pageNum,
                           @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT ap.admin_id, p.privilege_name " +
            "FROM sys_admin_privilege ap " +
            "LEFT JOIN sys_privilege p ON ap.privilege_id = p.id " +
            "WHERE ap.admin_id IN " +
            "<foreach collection='adminIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> queryPrivilegesByAdminIds(@Param("adminIds") List<Integer> adminIds);

    @Update("update sys_admin set admin_status = #{status} where admin_id = #{adminId}")
    int updateById(admin admin);

    @Select("select admin_name from sys_admin")
    List<String> queryAdminName();

    @Insert("insert into sys_admin(admin_name,role_name,password) values(#{adminName},#{roleName},#{password})")
    void insertAdmin(admin admin);

    @Insert("insert into sys_admin_privilege(admin_id,privilege_id) values(#{adminId},#{privilegeId})")
    void insertAdminPrivilege(@Param("adminId") int adminId, @Param("privilegeId") int privilegeId);

    @Select("select id from sys_privilege where privilege_name = #{privilegeName}")
    int queryPrivilegeIdByName(String privilegeName);

    @Update("update sys_admin set admin_name = #{adminName},role_name = #{roleName},password = #{password} where admin_id = #{adminId}")
    void updateAdminById(admin admin);
}
