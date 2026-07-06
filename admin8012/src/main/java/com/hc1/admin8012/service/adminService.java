package com.hc1.admin8012.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.admin;
import com.hc1.admin8012.bean.privilege;

import java.util.List;

public interface adminService {
    //分页查询管理员
    Page<admin> listAdmin(String keyWord,int pageNum,int pageSize);
    //根据 id 查询 该管理员 的职能
    List<String> queryPriByAdminId(int adminId);

    void updateById(admin admin);

    List<String> queryAdminName();

    void insertAdmin(admin admin);

    String updateAdminById(admin admin);
}
