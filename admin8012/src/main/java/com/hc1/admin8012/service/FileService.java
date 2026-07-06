package com.hc1.admin8012.service;

import com.hc1.admin8012.bean.CommonResult;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author xwj
 * @Description 通过 OpenFeign 调用 user8011 模块已有接口（X-Internal-Call 头自动注入）
 *              映射关系一对一，不再需要 user8011 单独建 /admin/* 端点
 */
@FeignClient(name = "user8011", contextId = "fileFeignClient", path = "/user8011")
public interface FileService {

    /** → user8011 GET /fileList（管理员模式：status=null 查全部，支持 keyword 搜索） */
    @GetMapping("/fileList")
    CommonResult<?> getFileList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "sortType", defaultValue = "latest") String sortType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize);

    /** → user8011 GET /fileDetail（管理员模式：返回文件元数据 + 作者信息） */
    @GetMapping("/fileDetail")
    CommonResult<?> getFileDetail(@RequestParam("fileUrl") String fileUrl);

    /** → user8011 GET /downloadFile/{fileUrl}（管理员模式：返回字节流，不走积分逻辑） */
    @GetMapping("/downloadFile/{fileUrl}")
    Response downloadFile(@PathVariable("fileUrl") String fileUrl);

    @RequestMapping("/updateStatus")
    String updateStatus(@RequestParam("fileUrl") String fileUrl,@RequestParam("status") int status);

}
