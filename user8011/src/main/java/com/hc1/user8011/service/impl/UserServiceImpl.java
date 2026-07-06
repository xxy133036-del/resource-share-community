package com.hc1.user8011.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hc1.user8011.bean.*;
import com.hc1.user8011.mapper.FileMapper;
import com.hc1.user8011.mapper.UserMapper;
import com.hc1.user8011.service.MessageService;
import com.hc1.user8011.service.UserService;
import com.hc1.user8011.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-11 13:24:45
 * @Version 1.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public User selectById(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    @CacheEvict(value = "userInfo", key = "#user.id")
    @Override
    public String updateUserById(User user) {
        try {
            String usernameRegex = "^[a-zA-Z0-9]+$";
            String passwordRegex = "^[a-zA-Z0-9]{6,}$";
            String nicknameRegex = "^.{0,12}$";
            if (user.getUsername() == null || user.getPassword() == null || user.getNickname() == null) {
                return "信息不完整";
            } else if (!user.getUsername().matches(usernameRegex) || !user.getPassword().matches(passwordRegex) || !user.getNickname().matches(nicknameRegex)) {
                return "格式错误";
            } else {
                String hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
                user.setPassword(hash);
                userMapper.updateUserById(user);
                return "success";
            }
        } catch (DuplicateKeyException e) {
            return "账号已存在";
        }
    }

    @CacheEvict(value = "userInfo", key = "#id")
    @Override
    public void updateUserAvatarById(Integer id, String avatar) {
        userMapper.updateUserAvatarById(id, avatar);
    }

    @Override
    public void addPointsFiveById(Integer id) {
        userMapper.addPointsFiveById(id);
        // 清除 IndexServiceImpl.indexUser 的 Redis 用户缓存，让前端立刻看到最新积分
        redisUtil.del("user:" + id);
    }

    @Override
    public void reducePointsFiveById(Integer id) {
        userMapper.reducePointsFiveById(id);
        redisUtil.del("user:" + id);
    }

    @Override
    public void addPointsOneById(Integer id) {
        userMapper.addPointsOneById(id);
        redisUtil.del("user:" + id);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    // 统一时区为 亚洲/上海
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    // Redis Key 前缀
    private static final String BITMAP_KEY_PREFIX = "user:checkin:map:";
    private static final String STREAK_KEY_PREFIX = "user:checkin:streak:";
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private MessageService messageService;
    /**
     * 1. 检查今天是否已签到
     */
    @Override
    public boolean checkTodayStatus(Integer id) {
        // 获取上海时区的当前日期
        LocalDate today = LocalDate.now(SHANGHAI_ZONE);
        String mapKey = buildBitmapKey(id, today);
        long offset = today.getDayOfMonth() - 1;

        Boolean isSigned = stringRedisTemplate.opsForValue().getBit(mapKey, offset);
        return Boolean.TRUE.equals(isSigned);
    }

    /**
     * 2. 执行签到并计算连续签到天数
     */
    @CacheEvict(value = "userInfo", key = "#id")
    @Override
    public int doCheckIn(Integer id) {
        LocalDate today = LocalDate.now(SHANGHAI_ZONE);
        String mapKey = buildBitmapKey(id, today);
        String streakKey = STREAK_KEY_PREFIX + id;
        long offset = today.getDayOfMonth() - 1;

        // 【步骤A】检查今天是否已经签到过

        if (checkTodayStatus(id)) {
            return -1; // -1 代表今日已签到，无需重复签到
        }

        // 【步骤B】标记今日已签到，并设置 60 天过期时间
        stringRedisTemplate.opsForValue().setBit(mapKey, offset, true);
        stringRedisTemplate.expire(mapKey, 60, TimeUnit.DAYS); // 满足 60 天过期需求

        // 【步骤C】计算连续签到天数
        int streakDays = 1; // 默认今天签到就是 1 天
        LocalDate yesterday = today.minusDays(1);
        String yesterdayMapKey = buildBitmapKey(id, yesterday);
        long yesterdayOffset = yesterday.getDayOfMonth() - 1;

        // 检查昨天是否签到过
        Boolean signedYesterday = stringRedisTemplate.opsForValue().getBit(yesterdayMapKey, yesterdayOffset);

        if (Boolean.TRUE.equals(signedYesterday)) {
            // 如果昨天签到过，连续签到天数 +1
            Long newStreak = stringRedisTemplate.opsForValue().increment(streakKey);
            streakDays = newStreak != null ? newStreak.intValue() : 1;
        } else {
            // 如果昨天没签到，说明连续签到中断了，重置为 1
            stringRedisTemplate.opsForValue().set(streakKey, "1");
        }
        // 连续签到计数器也设置 60 天过期，防止僵尸数据
        stringRedisTemplate.expire(streakKey, 60, TimeUnit.DAYS);

        // 同步 Redis 缓存：签到状态 + 清除用户缓存让 /home 下次查 DB 拿最新积分
        redisUtil.set("hasCheckedIn:" + id, true, 24 * 60 * 60);
        redisUtil.del("user:" + id);

        return streakDays;
    }

    @Override
    public void addPointsBatch(Integer userid, Integer points) {
        userMapper.addPointsBatch(userid, points);
        redisUtil.del("user:" + userid);
    }


    // 构建 Redis Key: user:checkin:map:用户ID:yyyyMM
    private String buildBitmapKey(Integer id, LocalDate date) {
        String suffix = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        return BITMAP_KEY_PREFIX + id + ":" + suffix;
    }


    @Override
    public String addLike(int fileId, int userId) {
        // 原子防重复点赞：setIfAbsent 返回 true 表示首次点赞
        String likeKey = "like:user:" + userId + ":file:" + fileId;
        if (!redisUtil.setIfAbsent(likeKey, "1", 100 * 24 * 60 * 60)) {
            return "已点赞";
        }
        // 查找资源
        File resource = fileMapper.selectById(fileId);
        if (resource == null) {
            // 回滚防重标记
            redisUtil.del(likeKey);
            return "文件不存在";
        }
        // 资源点赞量 +1
        redisUtil.hincr("file:likes:cache", String.valueOf(fileId), 1);
        // 自己点赞自己的资源不发通知
        if (resource.getUserid().equals(userId)) return "点赞成功";
        return sendLikeNotification(resource, userId);
    }

    @Override
    public String removeLike(int fileId, int userId) {
        String likeKey = "like:user:" + userId + ":file:" + fileId;
        if (redisUtil.get(likeKey) == null) {
            return "未点赞";
        }
        redisUtil.del(likeKey);
        redisUtil.hincr("file:likes:cache", String.valueOf(fileId), -1);
        return "取消点赞成功";
    }

    /**
     * 批量检查点赞状态，使用 Redis pipeline 合并所有 EXISTS 命令为 1 次网络往返
     * 优化前：N 个 fileId 产生 N 次 Redis 请求
     * 优化后：N 个 fileId 仅 1 次 Redis pipeline 请求
     */
    @Override
    public Map<Integer, Boolean> batchCheckLikes(int userId, List<Integer> fileIds) {
        Map<Integer, Boolean> result = new HashMap<>();
        if (fileIds == null || fileIds.isEmpty()) return result;

        // 通过 pipeline 批量发送 EXISTS 命令，减少网络往返
        List<Object> results = redisTemplate.executePipelined(
            (RedisCallback<Object>) connection -> {
                for (Integer fileId : fileIds) {
                    String likeKey = "like:user:" + userId + ":file:" + fileId;
                    connection.exists(likeKey.getBytes(StandardCharsets.UTF_8));
                }
                return null; // pipeline 模式下返回值由 executePipelined 统一收集
            }
        );

        // 结果与 fileIds 顺序一一对应
        for (int i = 0; i < fileIds.size(); i++) {
            Boolean exists = (Boolean) results.get(i);
            result.put(fileIds.get(i), Boolean.TRUE.equals(exists));
        }
        return result;
    }

    @Override
    public boolean checkLike(int fileId, int userId) {
        String likeKey = "like:user:" + userId + ":file:" + fileId;
        return redisUtil.hasKey(likeKey);
    }

    @Override
    public void updateUserStatusById(int status, int userId) {
        userMapper.updateUserStatusById(status, userId);
    }

    /**
     * 点赞后通知资源作者（点赞者本人不通知自己）
     */
    private String sendLikeNotification(File resource, int userId) {
        try {
            int fileId = resource.getId();
            Integer authorId = resource.getUserid();

            // 查找点赞者昵称
            User liker = userMapper.selectById(userId);
            String likerName = liker != null ? liker.getNickname() : "用户";

            // 资源名称
            String resourceName = resource.getFileName();
            if (resourceName == null || resourceName.isEmpty()) resourceName = "资源" + fileId;

            // 构造消息（点赞类型：无 title，仅 content；targetId 用于前端跳转）
            Message message = new Message();
            message.setUserId(authorId);
            message.setSenderId(userId);
            message.setSenderName(likerName);
            message.setType(1);
            message.setContent(likerName + "点赞了您的资源《" + resourceName + "》");
            message.setTargetId(fileId);

            messageService.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "点赞成功";
    }

}



