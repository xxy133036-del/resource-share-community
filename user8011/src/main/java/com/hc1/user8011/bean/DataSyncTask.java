package com.hc1.user8011.bean;

import com.hc1.user8011.service.FileService;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataSyncTask {

    private static final Logger log = LoggerFactory.getLogger(DataSyncTask.class);

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;

    // 每隔 3 分钟执行一次 (3 * 60 * 1000 = 180000 毫秒)
    @Scheduled(fixedDelay = 180000)
    public void syncRedisToDb() {
        log.info("开始执行定时任务：同步Redis下载量，积分，点赞量，评论量到数据库");
        syncFileDownloads();
        syncUserPoints();
        syncFileComments();
        syncFileLikes();
        log.info("定时任务同步完成");
    }
    /**
     * 同步文件的下载量
     */
    private void syncFileDownloads() {
        String cacheKey = "file:downloads:cache";
        String processingKey = "file:downloads:processing";

        // RedisUtil 判断是否有数据
        if (!redisUtil.hasKey(cacheKey)) {
            return;
        }

        try {
            // 1. 改名，防止在同步的几毫秒内有新数据进来导致丢失 (调用底层 template)
            redisTemplate.rename(cacheKey, processingKey);

            // 2. RedisUtil 取出这 3 分钟内所有的下载记录集合
            Map<Object, Object> entries = redisUtil.hmget(processingKey);

            if (entries == null || entries.isEmpty()) {
                return;
            }

            // 3. 循环批量更新数据库
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String fileUrl = String.valueOf(entry.getKey());
                int count = Integer.parseInt(String.valueOf(entry.getValue()));

                // 去数据库执行更新
                fileService.addDownloadCountBatch(fileUrl, count);
            }

            // 4. 同步成功，使用你的 RedisUtil 删除处理完的 Key
            redisUtil.del(processingKey);

        } catch (Exception e) {
            log.error("同步文件下载量失败", e);
        }
    }

    /**
     * 同步用户的增加积分
     */
    private void syncUserPoints() {
        String cacheKey = "user:points:cache";
        String processingKey = "user:points:processing";

        if (!redisUtil.hasKey(cacheKey)) {
            return;
        }

        try {
            // 1. 改名锁数据
            redisTemplate.rename(cacheKey, processingKey);

            // 2. 获取数据
            Map<Object, Object> entries = redisUtil.hmget(processingKey);

            if (entries == null || entries.isEmpty()) {
                return;
            }

            // 3. 循环更新数据库
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                Integer userId = Integer.parseInt(String.valueOf(entry.getKey()));
                int points = Integer.parseInt(String.valueOf(entry.getValue()));

                // 去数据库执行更新
                userService.addPointsBatch(userId, points);
            }

            // 4. 清除缓存
            redisUtil.del(processingKey);

        } catch (Exception e) {
            log.error("同步用户积分失败", e);
        }
    }
    /**
     * 同步文件的评论量
     */
    private void syncFileComments() {
        String cacheKey = "file:comments:cache";
        String processingKey = "file:comments:processing";

        // RedisUtil 判断是否有数据
        if (!redisUtil.hasKey(cacheKey)) {
            return;
        }

        try {
            // 1. 改名，防止在同步的几毫秒内有新数据进来导致丢失 (调用底层 template)
            redisTemplate.rename(cacheKey, processingKey);

            // 2. RedisUtil 取出这 3 分钟内所有的下载记录集合
            Map<Object, Object> entries = redisUtil.hmget(processingKey);

            if (entries == null || entries.isEmpty()) {
                return;
            }

            // 3. 循环批量更新数据库
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                int id = Integer.parseInt(String.valueOf(entry.getKey()));
                int count = Integer.parseInt(String.valueOf(entry.getValue()));

                // 去数据库执行更新
                fileService.addCommentCountBatch(id, count);
            }

            // 4. 同步成功，使用你的 RedisUtil 删除处理完的 Key
            redisUtil.del(processingKey);

        } catch (Exception e) {
            log.error("同步文件评论量失败", e);
        }
    }
    /**
     * 同步文件的点赞量
     */
    private void syncFileLikes() {
        String cacheKey = "file:likes:cache";
        String processingKey = "file:likes:processing";

        // RedisUtil 判断是否有数据
        if (!redisUtil.hasKey(cacheKey)) {
            return;
        }

        try {
            // 1. 改名，防止在同步的几毫秒内有新数据进来导致丢失 (调用底层 template)
            redisTemplate.rename(cacheKey, processingKey);

            // 2. RedisUtil 取出这 3 分钟内所有的下载记录集合
            Map<Object, Object> entries = redisUtil.hmget(processingKey);

            if (entries == null || entries.isEmpty()) {
                return;
            }

            // 3. 循环批量更新数据库
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                int id = Integer.parseInt(String.valueOf(entry.getKey()));
                int count = Integer.parseInt(String.valueOf(entry.getValue()));

                // 去数据库执行更新
                fileService.addLikesCountBatch(id, count);
            }

            // 4. 同步成功，使用你的 RedisUtil 删除处理完的 Key
            redisUtil.del(processingKey);

        } catch (Exception e) {
            log.error("同步文件点赞量失败", e);
        }
    }

}
