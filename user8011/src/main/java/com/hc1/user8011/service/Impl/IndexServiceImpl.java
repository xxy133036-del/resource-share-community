package com.hc1.user8011.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.bean.User;
import com.hc1.user8011.service.FileService;
import com.hc1.user8011.service.IndexService;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-18 13:06:06
 * @Version 1.0
 */
@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;

    @Override
    public Map<String, Object> indexUser(Integer userId) {
        HashMap<String, Object> userAndCheckInStatus = new HashMap<>();
        String userCacheKey = "user:" + userId;
        User user = (User) redisUtil.get(userCacheKey);
        if (user == null) {
            user = userService.selectById(userId);
            // 设置30分钟过期时间
            redisUtil.set(userCacheKey, user, 30 * 60);
            user = (User) redisUtil.get(userCacheKey);
        }
        boolean hasCheckedIn = userService.checkTodayStatus(userId);
        userAndCheckInStatus.put("user", user);
        userAndCheckInStatus.put("hasCheckedIn", hasCheckedIn);
        return userAndCheckInStatus;
    }

    @Override
    public Page<File> indexFiles(String sortType, int pageNum, int pageSize, Integer userId) {
        // v2 缓存版本号：改过热度和排序公式后递增，自动淘汰旧缓存
        String filesCacheKey = "files:index:v2:" + sortType + ":" + pageNum + ":" + pageSize;
        // 2. 从 Redis 获取
        Page<File> page = (Page<File>) redisUtil.get(filesCacheKey);
        if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
            // 透传分页参数到 FileService
            page = fileService.getFileListWithAuthor(null, 2, sortType, pageNum, pageSize);
            // 4. 存入 Redis 缓存，并登记到缓存 key 索引 Set（用于 appendToIndexCache 快速定位，避免使用 KEYS 命令）
            if (page.getRecords() != null && !page.getRecords().isEmpty()) {
                // 设置 10 分钟过期时间
                redisUtil.set(filesCacheKey, page, 10 * 60);
                // 将缓存 key 注册到索引 Set，一并设置过期时间，防止僵尸 key 堆积
                redisUtil.sSet("cache:index:keys", filesCacheKey);
                redisUtil.expire("cache:index:keys", 10 * 60);
            }
        }

        // 缓存数据可能缺少计算字段，统一补齐
        fillHotScoreForCache(page.getRecords());

        // 批量填充当前用户的点赞状态（不受缓存影响）
        if (userId != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
            List<Integer> fileIds = page.getRecords().stream()
                    .map(File::getId)
                    .collect(Collectors.toList());
            Map<Integer, Boolean> likeStatusMap = userService.batchCheckLikes(userId, fileIds);
            for (File file : page.getRecords()) {
                file.setLiked(likeStatusMap.getOrDefault(file.getId(), false));
            }
        }

        return page;
    }

    /** 缓存数据可能没有计算字段，统一补算热度 */
    private void fillHotScoreForCache(List<File> files) {
        if (files == null) return;
        for (File f : files) {
            int d = f.getDownloadsCount() != null ? f.getDownloadsCount() : 0;
            int l = f.getLikesCount() != null ? f.getLikesCount() : 0;
            int c = f.getCommentsCount() != null ? f.getCommentsCount() : 0;
            f.setHotScore(d * 3 + l * 2 + c);
        }
    }
}
