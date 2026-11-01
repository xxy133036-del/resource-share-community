package com.hc1.user8011.service.Impl;


import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.CommonResult;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.mapper.FileMapper;
import com.hc1.user8011.service.FileService;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-11 15:18:21
 * @Version 1.0
 */
@Service
public class FileServiceImpl  implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public int insert(File file, int userid) {
      return fileMapper.insert(file, userid);
    }

    @Override
    public void addDownloadCountBatch(String fileUrl, int count) {
        fileMapper.addDownloadCountBatch(fileUrl, count);
    }
    @Override
    public void addCommentCountBatch(int id, int count) {
        fileMapper.addCommentsCountBatch(id, count);
    }

    @Override
    public void addLikesCountBatch(int id, int count) {
        fileMapper.addLikesCountBatch(id, count);
    }

    @Override
    public File selectByFileUrl(String fileUrl) {
        return fileMapper.selectByFileUrl(fileUrl);
    }


    @Override
    public Page<File> getFileListWithAuthor(String keyword, Integer status,
                                             String sortType, int pageNum, int pageSize) {
        Page<File> page = new Page<>(pageNum, pageSize);
        // 统一使用 Mapper 动态 SQL：支持关键字搜索、状态筛选、排序
        fileMapper.searchFiles(page, keyword, null, null, status, sortType,null);

        List<File> fileList = page.getRecords();
        if (fileList == null || fileList.isEmpty()) {
            return page;
        }

        fillAuthorInfo(fileList);
        fillHotScore(fileList);
        return page;
    }

    @Override
    public Page<File> searchFiles(String keyword, String startDate, String endDate,
                                   int pageNum, int pageSize) {
        Page<File> page = new Page<>(pageNum, pageSize);
        // 用户端搜索：固定 status=2（仅已审核），按最新排序
        fileMapper.searchFiles(page, keyword, startDate, endDate, 2, "latest", null);

        List<File> fileList = page.getRecords();
        if (fileList == null || fileList.isEmpty()) return page;

        fillAuthorInfo(fileList);
        fillHotScore(fileList);
        return page;
    }

    @Override
    public Page<File> getMyFileList(int userId, int pageNum, int pageSize) {
        Page<File> page = new Page<>(pageNum, pageSize);
        // 查自己的文件：不限状态，按最新排序
        fileMapper.searchFiles(page, null, null, null, null, "latest", userId);

        List<File> fileList = page.getRecords();
            User currentUser = userService.selectById(userId);
            if (currentUser != null) {
                for (File file : fileList) {
                    file.setAuthorName(currentUser.getNickname());
                    file.setAuthorAvatar(currentUser.getAvatar());
                }
            }
            fillHotScore(fileList);
        return page;
    }

    @Override
    public File getFileDetail(String fileUrl) {
        File file = fileMapper.selectByFileUrl(fileUrl);
        if (file == null) {
            return null;
        }
        User author = userService.selectById(file.getUserid());
        if (author != null) {
            file.setAuthorName(author.getNickname());
            file.setAuthorAvatar(author.getAvatar());
        }
        fillHotScore(Collections.singletonList(file));
        return file;
    }

    /** 批量填充作者信息 */
    private void fillAuthorInfo(List<File> fileList) {
        List<Integer> userIds = fileList.stream()
                .map(File::getUserid)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (File file : fileList) {
            User user = userMap.get(file.getUserid());
            if (user != null) {
                file.setAuthorName(user.getNickname());
                file.setAuthorAvatar(user.getAvatar());
            }
        }
    }

    /** 给文件列表统一计算热度分 */
    private void fillHotScore(List<File> files) {
        if (files == null) return;
        for (File f : files) {
            int d = f.getDownloadsCount() != null ? f.getDownloadsCount() : 0;
            int l = f.getLikesCount() != null ? f.getLikesCount() : 0;
            int c = f.getCommentsCount() != null ? f.getCommentsCount() : 0;
            f.setHotScore(d * 3 + l * 2 + c);
        }
    }

    private static final String UPLOADFILE_URL = "D:/java/file/doc/";

    /** 单用户上传频率限制：窗口秒数 + 窗口内最大次数 */
    private static final long UPLOAD_RATE_WINDOW_SECONDS = 60;
    private static final long UPLOAD_RATE_MAX_COUNT = 20;

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md",
            "zip", "rar", "7z", "png", "jpg", "jpeg", "gif"));

    @Override
    public CommonResult<?> uploadFile(File fileEntity, MultipartFile file,Integer userid) {
        try {
            // 0. 空文件校验
            if (file == null || file.isEmpty()) {
                return CommonResult.error("请选择要上传的文件");
            }

            // 1. 单用户上传频率限制（Redis 固定窗口计数，防同一账号并发刷上传）
            String rateKey = "upload:rate:" + userid;
            long uploadCount = redisUtil.incr(rateKey, 1);
            if (uploadCount == 1) {
                redisUtil.expire(rateKey, UPLOAD_RATE_WINDOW_SECONDS);
            }
            if (uploadCount > UPLOAD_RATE_MAX_COUNT) {
                redisUtil.decr(rateKey, 1); // 回滚本次计数，避免占用配额
                return CommonResult.error("上传过于频繁，请稍后再试");
            }

            // 2. 生成唯一文件名 (雪花ID + 原始文件名)
            String originalFilename = file.getOriginalFilename();
            String uniqueFileName = IdUtil.getSnowflakeNextIdStr() + "_" + originalFilename;
            java.io.File destFile = new java.io.File(UPLOADFILE_URL + uniqueFileName);

            // 3. 流式写入：先读文件头做魔数校验，再边算 MD5 边落盘（不把整个文件读进内存）
            String fileHash;
            try (InputStream in = file.getInputStream()) {
                byte[] header = new byte[8];
                int headerRead = in.read(header);
                if (headerRead <= 0) {
                    return CommonResult.error("文件内容为空");
                }
                if (!isAllowedFileType(header, headerRead, originalFilename)) {
                    return CommonResult.error("不支持的文件类型，或文件内容与扩展名不符");
                }
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.update(header, 0, headerRead);
                try (OutputStream out = new FileOutputStream(destFile)) {
                    out.write(header, 0, headerRead);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        md5.update(buf, 0, n);
                        out.write(buf, 0, n);
                    }
                }
                fileHash = HexUtil.encodeHexStr(md5.digest());
            }

            // 4. 秒传去重：相同 MD5 的文件已存在则复用，不重复占用磁盘
            File existing = fileMapper.selectByFileHash(fileHash);
            if (existing != null) {
                destFile.delete();
                return CommonResult.success("文件已存在，秒传成功");
            }

            fileEntity.setFileUrl(uniqueFileName);
            fileEntity.setFileHash(fileHash);
            insert(fileEntity, userid);
            userService.addPointsFiveById(userid);

            // 上传成功后，追加新资源到首页缓存（避免清缓存重查）
            appendToIndexCache(fileEntity, userid);

            return CommonResult.success("成功上传资源！ 请等待审核");

        } catch (Exception e) {
            log.error("上传文件失败, userId={}", userid, e);
            return CommonResult.error("服务器存储异常：" + e.getMessage());
        }
    }

    /** 扩展名白名单 + 魔数校验（防把可执行文件伪装成图片/文档） */
    private boolean isAllowedFileType(byte[] header, int len, String filename) {
        String ext = "";
        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return false;
        }
        switch (ext) {
            case "pdf":
                return startsWith(header, len, 0x25, 0x50, 0x44, 0x46);
            case "png":
                return startsWith(header, len, 0x89, 0x50, 0x4E, 0x47);
            case "jpg":
            case "jpeg":
                return startsWith(header, len, 0xFF, 0xD8, 0xFF);
            case "gif":
                return startsWith(header, len, 0x47, 0x49, 0x46, 0x38);
            case "zip":
            case "docx":
            case "xlsx":
            case "pptx":
                return startsWith(header, len, 0x50, 0x4B);
            default:
                // txt/md/doc/xls/ppt/rar/7z 等不严格校验魔数，仅按扩展名放行
                return true;
        }
    }

    private boolean startsWith(byte[] data, int len, int... expected) {
        if (len < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ResponseEntity<byte[]> downloadFile(String fileUrl, Integer userId) {
        java.io.File file = new java.io.File(UPLOADFILE_URL + fileUrl);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 1. 读文件字节
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            log.error("读取文件失败, fileUrl={}", fileUrl, e);
            return ResponseEntity.status(500).build();
        }

        // 2. 积分逻辑（仅用户下载）
        if (userId != null) {
            redisUtil.hincr("file:downloads:cache", fileUrl, 1);

            String ownerKey = "file:owner:" + fileUrl;
            Object cachedUserIdObj = redisUtil.get(ownerKey);
            Integer fileOwnerId = null;
            if (cachedUserIdObj != null) {
                fileOwnerId = Integer.parseInt(cachedUserIdObj.toString());
            } else {
                File resource = selectByFileUrl(fileUrl);
                if (resource != null) {
                    fileOwnerId = resource.getUserid();
                    redisUtil.set(ownerKey, fileOwnerId, 24 * 60 * 60);
                }
            }
            if (fileOwnerId != null) {
                redisUtil.hincr("user:points:cache", String.valueOf(fileOwnerId), 1);
            }

            int affected = userService.reducePointsFiveById(userId);
            if (affected == 0) {
                return ResponseEntity.status(402).build(); // 402 Payment Required — 积分不足
            }
        }

        // 3. 构建响应（用户 + 管理员共用）
        String realFileName = fileUrl.contains("_")
                ? fileUrl.substring(fileUrl.indexOf("_") + 1) : fileUrl;
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(realFileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (Exception e) {
            encodedFileName = "download";
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Override
    public void updateStatus(String fileUrl, int status) {
        fileMapper.updateStatus(fileUrl, status);
    }

    @Override
    public Page<File> getUserFilesByUserId(int userId, Integer pageNum, Integer pageSize) {
        Page<File> page = new Page<>(pageNum, pageSize);
        fileMapper.searchFiles(page, null, null, null, null, "latest", userId);
        return page;
    }

    /**
     * 上传新资源后，直接追加到首页缓存，避免清空缓存重新查库
     * 使用 Redis Set 索引定位首页缓存 key（替代 KEYS 命令，避免阻塞 Redis）
     */
    private void appendToIndexCache(File newFile, Integer userId) {
        try {
            // 只有审核通过(status=2)的资源才追加到首页缓存
            if (newFile.getStatus() == null || newFile.getStatus() != 2) return;
            // 补充作者信息
            User author = userService.selectById(userId);
            if (author != null) {
                newFile.setAuthorName(author.getNickname());
                newFile.setAuthorAvatar(author.getAvatar());
            }

            // 从 Redis Set 索引中获取所有首页缓存 key（替代 KEYS 命令，生产安全）
            Set<Object> keySet = redisUtil.sGet("cache:index:keys");
            if (keySet == null || keySet.isEmpty()) return;

            for (Object keyObj : keySet) {
                String key = String.valueOf(keyObj);

                // 热门排序缓存直接删，新资源热度为0不会排前面，下次请求重建即可
                if (key.contains(":hottest:")) {
                    redisUtil.del(key);
                    redisUtil.setRemove("cache:index:keys", key); // 同步清理索引
                    continue;
                }

                // 最新排序：只处理 pageNum=1 的缓存，格式 files:index:latest:1:12
                if (!key.contains(":latest:")) continue;
                String[] parts = key.split(":");
                if (parts.length < 5 || !"1".equals(parts[parts.length - 2])) continue;

                try {
                    Page<File> cached = (Page<File>) redisUtil.get(key);
                    if (cached == null || cached.getRecords() == null) continue;

                    int pageSize = Math.max(cached.getRecords().size(), 12);

                    // 追加到列表头部
                    List<File> newRecords = new ArrayList<>();
                    newRecords.add(newFile);
                    newRecords.addAll(cached.getRecords());
                    // 保持分页大小
                    if (newRecords.size() > pageSize) {
                        newRecords = newRecords.subList(0, pageSize);
                    }
                    cached.setRecords(newRecords);
                    cached.setTotal(cached.getTotal() + 1);

                    // 重置过期时间（10分钟）
                    redisUtil.set(key, cached, 10 * 60);
                } catch (Exception e) {
                    // 单个key失败不影响其他，直接删掉让下次重建
                    redisUtil.del(key);
                    redisUtil.setRemove("cache:index:keys", key);
                }
            }
        } catch (Exception e) {
            // 缓存追加失败不影响上传结果
            log.error("追加首页缓存失败, fileUrl={}", newFile.getFileUrl(), e);
        }
    }


}
