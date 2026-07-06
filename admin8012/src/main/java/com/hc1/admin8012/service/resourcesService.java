package com.hc1.admin8012.service;

import com.hc1.admin8012.bean.CommonResult;
import feign.Response;

/**
 * @Author xwj
 * @Description 资源管理服务接口
 * @Date 2026-07-03
 */
public interface resourcesService {

    CommonResult<?> getResourcesList(Integer adminId, String keyword, Integer status,
                                   String sortType, int pageNum, int pageSize);

    Response downloadResource(Integer adminId, String fileUrl);

    CommonResult<?> getResourceDetail(Integer adminId, String fileUrl);

    String updateStatus(String fileUrl, int status);
}
