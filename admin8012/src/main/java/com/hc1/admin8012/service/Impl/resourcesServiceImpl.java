package com.hc1.admin8012.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hc1.admin8012.bean.CommentDTO;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.FileDTO;
import com.hc1.admin8012.service.FileService;
import com.hc1.admin8012.service.adminService;
import com.hc1.admin8012.service.resourcesService;
import feign.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * @Author xwj
 * @Description 资源管理服务实现
 * @Date 2026-07-03
 */
@Service
public class resourcesServiceImpl implements resourcesService {

    @Autowired
    private FileService fileService;


    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public CommonResult getResourcesList(Integer adminId, String keyword, Integer status,
                                          String sortType, int pageNum, int pageSize) {

        //  通过 Feign 调用 user8011 的 /fileList（X-Internal-Call 头自动注入，管理员模式）
        CommonResult result;
        try {
            result = fileService.getFileList(keyword, status, sortType, pageNum, pageSize);
        } catch (Exception e) {
            return CommonResult.error("调用资源服务失败：" + e.getMessage());
        }

        // 3. Feign 返回的 data 是 LinkedHashMap，转换为 Page<FileDTO>
        if (result.getCode() == 200 && result.getData() != null) {
            try {
                JavaType pageType = objectMapper.getTypeFactory()
                        .constructParametricType(Page.class, FileDTO.class);
                Page<FileDTO> page = objectMapper.convertValue(result.getData(), pageType);
                result.setData(page);
            } catch (Exception e) {
                return CommonResult.error("数据解析失败：" + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public Response downloadResource(Integer adminId, String fileUrl) {

        // 通过 Feign 调用 user8011 的 /admin/downloadFile/{fileUrl}
        return fileService.downloadFile(fileUrl);
    }

    @Override
    public CommonResult getResourceDetail(Integer adminId, String fileUrl) {

        // 通过 Feign 调用 user8011 的 /fileDetail
        CommonResult result;
        try {
            result = fileService.getFileDetail(fileUrl);
        } catch (Exception e) {
            return CommonResult.error("获取文件详情失败：" + e.getMessage());
        }
        //  转换 data 为 FileDTO
        if (result.getCode() == 200 && result.getData() != null) {
            try {
                FileDTO fileDTO = objectMapper.convertValue(result.getData(), FileDTO.class);
                result.setData(fileDTO);
            } catch (Exception e) {
                return CommonResult.error("数据解析失败：" + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public CommonResult getCommentList(Integer adminId, Integer targetId, int pageNum, int pageSize) {

        // 通过 Feign 调用 user8011 的 /getCommentListWithAuthor
        CommonResult result;
        try {
            result = fileService.getCommentList(targetId, pageNum, pageSize);
        } catch (Exception e) {
            return CommonResult.error("获取评论列表失败：" + e.getMessage());
        }
        // 转换 data 为 Page<CommentDTO>
        if (result.getCode() == 200 && result.getData() != null) {
            try {
                JavaType pageType = objectMapper.getTypeFactory()
                        .constructParametricType(Page.class, CommentDTO.class);
                Page<CommentDTO> page = objectMapper.convertValue(result.getData(), pageType);
                result.setData(page);
            } catch (Exception e) {
                return CommonResult.error("数据解析失败：" + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public CommonResult<?> updateStatus(String fileUrl, int status) {
        return fileService.updateStatus(fileUrl, status);
    }


}
