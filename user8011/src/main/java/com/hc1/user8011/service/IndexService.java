package com.hc1.user8011.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.bean.User;

import java.util.Map;

public interface IndexService {
    Map<String,Object> indexUser(Integer userId);
    Page<File> indexFiles(String sortType, int pageNum, int pageSize, Integer userId);

}
