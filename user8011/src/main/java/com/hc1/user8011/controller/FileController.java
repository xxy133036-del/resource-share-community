package com.hc1.user8011.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.mapper.FileMapper;
import com.hc1.user8011.service.FileService;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.AdminCallContext;
import com.hc1.user8011.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @Author xwj
 * @Description
 * @Date 2026-06-11 15:14:10
 * @Version 1.0
 */
@RestController
public class FileController {


    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;

    // 上传文件
    @PostMapping("/uploadFile")
    public CommonResult<?> uploadFile(@ModelAttribute File fileEntity,
                                @RequestParam("file") MultipartFile file
    ) {
        int userid = UserContext.getUserId();
        UserContext.removeUserId();
        return  fileService.uploadFile(fileEntity, file, userid);
    }

    /**
     * 文件下载（用户端 + 管理员端共用）
     * userId != null → 用户下载（走积分结算）；userId == null → 管理员下载（不走积分）
     */
    @GetMapping("/downloadFile/{fileUrl}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable("fileUrl") String fileUrl) {
        Integer userId = UserContext.getUserId();
        UserContext.removeUserId();
        return fileService.downloadFile(fileUrl, userId);
    }

    /**
     * 文件列表（用户端 + 管理员端共用）
     * 用户端：固定查 status=2（已审核），不传 keyword
     * 管理员端（X-Internal-Call）：可选 status（默认全部）和 keyword 搜索
     */
    @GetMapping("/fileList")
    public CommonResult<?> getFileList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "sortType", defaultValue = "latest") String sortType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Integer userId = UserContext.getUserId();

            // 管理员调用：status 由参数传入（默认 null=全部）；用户调用：固定 status=2
            if (!AdminCallContext.isAdminCall()) {
                status = 2; // 用户只能看到已审核文件
            }

            Page<File> page = fileService.getFileListWithAuthor(keyword, status, sortType, pageNum, pageSize);

            // 批量填充当前用户的点赞状态（仅用户端）
            if (!AdminCallContext.isAdminCall() && userId != null
                    && page.getRecords() != null && !page.getRecords().isEmpty()) {
                List<Integer> fileIds = page.getRecords().stream()
                        .map(File::getId)
                        .collect(Collectors.toList());
                Map<Integer, Boolean> likeStatusMap = userService.batchCheckLikes(userId, fileIds);
                for (File file : page.getRecords()) {
                    file.setLiked(likeStatusMap.getOrDefault(file.getId(), false));
                }
            }
            UserContext.removeUserId();
            return CommonResult.success(page);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("获取资源列表失败：" + e.getMessage());
        }
    }

    // 文件详情（用户端 + 管理员端共用）
    @GetMapping("/fileDetail")
    public CommonResult<?> getFileDetail(@RequestParam("fileUrl") String fileUrl) {
        try {
            File file = fileService.getFileDetail(fileUrl);
            if (file == null) {
                return CommonResult.error("文件不存在");
            }
            return CommonResult.success(file);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("获取文件详情失败：" + e.getMessage());
        }
    }

    @GetMapping("/myFiles")
    public CommonResult<?> getMyFiles(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "12") Integer pageSize) {
        try {
            Integer userId = UserContext.getUserId();
            Page<File> page = fileService.getMyFileList(userId, pageNum, pageSize);
            UserContext.removeUserId();
            return CommonResult.success(page);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("获取我的发布失败：" + e.getMessage());
        }
    }

    @GetMapping("/searchResources")
    public CommonResult<?> searchResources(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "12") Integer pageSize) {
        try {
            Integer userId = UserContext.getUserId();
            Page<File> page = fileService.searchFiles(keyword, startDate, endDate, pageNum, pageSize);
            // 批量填充当前用户的点赞状态
            if (userId != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
                List<Integer> fileIds = page.getRecords().stream()
                        .map(File::getId).collect(Collectors.toList());
                Map<Integer, Boolean> likeStatusMap = userService.batchCheckLikes(userId, fileIds);
                for (File file : page.getRecords()) {
                    file.setLiked(likeStatusMap.getOrDefault(file.getId(), false));
                }
            }
            UserContext.removeUserId();
            return CommonResult.success(page);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("搜索失败：" + e.getMessage());
        }
    }

    @RequestMapping("/updateStatus")
    public String updateStatus(@RequestParam("fileUrl") String fileUrl,@RequestParam("status") int status)
    {
        if(!AdminCallContext.isAdminCall())
            return "无权限";
        fileService.updateStatus(fileUrl, status);
        return "已审核";
    }

    @GetMapping("/getUserFilesByUserId")
    public CommonResult<?> getUserFilesByUserId(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "7") Integer pageSize,
                                                @RequestParam("userId") int userId) {
        try {
            Page<File> page = fileService.getUserFilesByUserId(userId, pageNum, pageSize);
            return CommonResult.success(page);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.error("获取该用户所发布的资源失败：" + e.getMessage());
        }
    }

}
