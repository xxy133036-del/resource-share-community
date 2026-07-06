package com.hc1.user8011.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.File;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    int insert(File file, int userid);

    void addDownloadCountBatch(String fileUrl, int count);

    void addCommentCountBatch(int id, int count);

    void addLikesCountBatch(int id, int count);

    File selectByFileUrl(String fileUrl);

    /** 分页查询文件列表（支持关键字搜索、状态筛选、排序），管理员调用时 status 传 null 查全部 */
    Page<File> getFileListWithAuthor(String keyword, Integer status, String sortType, int pageNum, int pageSize);

    /** 按用户ID查询该用户发布的资源 */
    Page<File> getMyFileList(int userId, int pageNum, int pageSize);

    /** 模糊搜索资源（标题/标签/作者），支持日期筛选 — 用户端调用，固定 status=2 */
    Page<File> searchFiles(String keyword, String startDate, String endDate, int pageNum, int pageSize);

    /** 查询文件详情（含作者信息） */
    File getFileDetail(String fileUrl);

    CommonResult<?> uploadFile(File fileEntity, MultipartFile file, Integer userid);

    /**
     * 文件下载（用户 + 管理员共用）
     * userId != null → 走积分结算；userId == null → 管理员模式，不走积分
     */
    ResponseEntity<byte[]> downloadFile(String fileUrl, Integer userId);

    void updateStatus(String fileUrl, int status);

    Page<File> getUserFilesByUserId(int userId, Integer pageNum, Integer pageSize);
}
