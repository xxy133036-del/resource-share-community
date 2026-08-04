package com.hc1.user8011.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.Message;
import com.hc1.user8011.bean.MessageSummary;
import com.hc1.user8011.mapper.MessageMapper;
import com.hc1.user8011.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Author xwj
 * @Description 消息服务：发送 / 聚合摘要 / 分类详情 / 标记已读
 * @Date 2026-06-22 11:36:00
 * @Version 1.0
 */
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    private static final Map<Integer, String> TYPE_NAMES = new HashMap<>();
    static {
        TYPE_NAMES.put(1, "点赞消息");
        TYPE_NAMES.put(2, "评论消息");
        TYPE_NAMES.put(3, "系统公告");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sendMessage(Message message) {
        message.setStatus(0);  // 默认未读
        messageMapper.insertMessage(message);
    }

    @Override
    public List<MessageSummary> getSummary(int userId) {
        // 1. 查每种 type 的总数和第一条发送者
        List<Map<String, Object>> rows = messageMapper.selectSummary(userId);
        // 2. 查每种 type 的未读数
        List<Map<String, Object>> unreadRows = messageMapper.countUnreadByType(userId);

        // 3. 未读数做成 Map<type, count>
        Map<Integer, Integer> unreadMap = new HashMap<>();
        for (Map<String, Object> row : unreadRows) {
            Object typeObj = row.get("type");
            Object countObj = row.get("COUNT(*)");
            if (typeObj != null && countObj != null) {
                unreadMap.put(((Number) typeObj).intValue(), ((Number) countObj).intValue());
            }
        }

        // 4. 组装结果：确保 1/2/3 三种类型都存在
        Map<Integer, MessageSummary> summaryMap = new LinkedHashMap<>();
        for (int type : new int[]{1, 2, 3}) {
            summaryMap.put(type, new MessageSummary(
                type,
                TYPE_NAMES.get(type),
                0,
                null,
                unreadMap.getOrDefault(type, 0)
            ));
        }

        // 5. 用实际数据覆盖默认值
        for (Map<String, Object> row : rows) {
            Object typeObj = row.get("type");
            Object totalObj = row.get("total");
            Object firstObj = row.get("firstSender");
            if (typeObj == null) continue;

            int type = ((Number) typeObj).intValue();
            int total = totalObj != null ? ((Number) totalObj).intValue() : 0;
            String firstSender = firstObj != null ? (String) firstObj : null;

            MessageSummary s = summaryMap.get(type);
            if (s != null) {
                s.setTotal(total);
                s.setFirstSender(firstSender);
            }
        }

        return new ArrayList<>(summaryMap.values());
    }

    @Override
    public Page<Message> getByType(int userId, int type, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Message> records = messageMapper.selectByType(userId, type, offset, pageSize);
        int total = messageMapper.countByType(userId, type);

        Page<Message> page = new Page<>(pageNum, pageSize);
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    public void markRead(int messageId) {
        messageMapper.markRead(messageId);
    }

    @Override
    public void markReadByType(int userId, int type) {
        messageMapper.markReadByType(userId, type);
    }
}