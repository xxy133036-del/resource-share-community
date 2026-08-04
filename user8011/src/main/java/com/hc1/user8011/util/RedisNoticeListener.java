package com.hc1.user8011.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @Author xwj
 * @Description 订阅 notice:broadcast 频道，接收公告广播并缓存最新公告
 * @Date 2026-07-03
 * @Version 1.0
 */
@Component
public class RedisNoticeListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisNoticeListener.class);

    private static final String LATEST_NOTICE_KEY = "notice:latest";

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> noticeMap = mapper.readValue(json, Map.class);
            // 以 Map 对象存入 Redis，读取时直接返回对象
            redisUtil.set(LATEST_NOTICE_KEY, noticeMap);
        } catch (Exception e) {
            log.error("处理公告广播消息失败", e);
        }
    }
}
