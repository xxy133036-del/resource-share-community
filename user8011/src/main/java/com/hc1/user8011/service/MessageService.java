package com.hc1.user8011.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.user8011.bean.Message;
import com.hc1.user8011.bean.MessageSummary;

import java.util.List;

public interface MessageService  {

    void sendMessage(Message message);

    List<MessageSummary> getSummary(int userId);
    Page<Message> getByType(int userId, int type, int pageNum, int pageSize);

    void markRead(int messageId);
    void markReadByType(int userId, int type);


}
