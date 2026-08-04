package com.hc1.admin8012.controller;

import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.service.FileService;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.service.resourcesService;
import com.hc1.admin8012.util.adminContext;
import feign.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * @Author xwj
 * @Description 资源管理控制器（管理员端）
 * @Date 2026-06-27 10:34:30
 * @Version 1.0
 */
@RestController
public class resourcesController {

    @Autowired
    private resourcesService resourcesService;
    @Autowired
    private adminService adminService;
    /**
     * 分页查询资源列表（管理员端）
     */
    @GetMapping("/resourcesList")
    public CommonResult<?> resourcesList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "sortType", defaultValue = "latest") String sortType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("资源管理"))
            return CommonResult.error("无权限");
        return resourcesService.getResourcesList(adminId, keyword, status, sortType, pageNum, pageSize);
    }

    /**
     * 管理员下载文件（用于审核资源内容）
     * 通过 Feign 调用 user8011 获取文件流，转发给客户端
     */
    @GetMapping("/downloadResource/{fileUrl}")
    public void downloadResource(@PathVariable("fileUrl") String fileUrl,
                                  HttpServletResponse response) throws IOException {
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if (!adminNames.contains("资源管理")) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"code\":500,\"message\":\"无权限\"}");
            return;
        }
        // 通过 Feign 调用 user8011 下载接口
        Response feignResponse = resourcesService.downloadResource(adminId, fileUrl);
        if (feignResponse == null || feignResponse.body() == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"code\":404,\"message\":\"文件不存在\"}");
            return;
        }

        // 透传 Content-Type
        String contentType = feignResponse.headers()
                .getOrDefault("Content-Type", Collections.singletonList("application/octet-stream"))
                .stream().findFirst().orElse("application/octet-stream");
        response.setContentType(contentType);

        // 透传 Content-Disposition
        feignResponse.headers().getOrDefault("Content-Disposition", Collections.emptyList())
                .forEach(disposition -> response.setHeader("Content-Disposition", disposition));

        // 将 Feign 响应的字节流转发给客户端
        try (Response r = feignResponse;
             InputStream in = r.body().asInputStream()) {
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }

    /**
     * 管理员查看文件详情
     */
    @GetMapping("/resourceDetail")
    public CommonResult<?> resourceDetail(@RequestParam("fileUrl") String fileUrl) {
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("资源管理"))
            return CommonResult.error("无权限");
        return resourcesService.getResourceDetail(adminId, fileUrl);
    }

    /**
     * 管理员查看资源评论（通过 Feign 调用 user8011）
     */
    @GetMapping("/resourceComments")
    public CommonResult<?> resourceComments(
            @RequestParam("targetId") Integer targetId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if(!adminNames.contains("资源管理"))
            return CommonResult.error("无权限");
        return resourcesService.getCommentList(adminId, targetId, pageNum, pageSize);
    }

    // 管理员审核文件 0待审核 1 审核未通过 2 审核通过
    @PutMapping("/updateStatus")
    public CommonResult<?> updateStatus(@RequestParam("fileUrl") String fileUrl, @RequestParam("status") int status) {
        Integer adminId = adminContext.getAdminId();
        List<String> adminNames = adminService.queryPriByAdminId(adminId);
        if (!adminNames.contains("资源管理"))
            return CommonResult.error("无权限");
        return resourcesService.updateStatus(fileUrl, status);
    }
}
