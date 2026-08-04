package com.hc1.admin8012.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.notice;

import java.util.List;

public interface noticeService {

    void insertNotice(String title, String content);

    Page<notice> queryNoticeByAddition(String keyword, String startDate, String endDate, int pageNum, int pageSize);

    CommonResult<?> queryNotice(int noticeId);

    Page<notice> queryNoticeList(int pageNum, int pageSize);

    void deleteNotice(int noticeId);

    void updateNotice(String title,String content,int noticeId);

    void refreshNoticeListCache();
}
