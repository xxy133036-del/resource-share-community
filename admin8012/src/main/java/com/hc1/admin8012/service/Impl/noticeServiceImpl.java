package com.hc1.admin8012.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc1.admin8012.bean.CommonResult;
import com.hc1.admin8012.bean.notice;
import com.hc1.admin8012.mapper.noticeMapper;
import com.hc1.admin8012.service.noticeService;
import com.hc1.admin8012.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-27 10:46:29
 * @Version 1.0
 */
@Service
public class noticeServiceImpl implements noticeService {
    @Autowired
    private noticeMapper noticeMapper;
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public void insertNotice(String title, String content) {
        noticeMapper.insertNotice(title, content);
    }

    @Override
    public Page<notice> queryNoticeByAddition(String keyword, String startDate, String endDate, int pageNum, int pageSize) {
        Page<notice> page = new Page<>(pageNum, pageSize);
        noticeMapper.queryNoticeByAddition(page,keyword, startDate, endDate);
        return page;
    }

    @Override
    public CommonResult<?> queryNotice(int noticeId) {
       return CommonResult.success(noticeMapper.queryNotice(noticeId));
    }

    @Override
    public Page<notice> queryNoticeList(int pageNum, int pageSize) {
        Page<notice> page = new Page<>(pageNum, pageSize);
        QueryWrapper<notice> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        noticeMapper.selectPage(page, wrapper);
        return page;
    }

    @Override
    public void deleteNotice(int noticeId) {
        noticeMapper.deleteById(noticeId);
    }

    @Override
    public void updateNotice(String title, String content, int noticeId) {
        noticeMapper.updateNoticeById(title, content, noticeId);
    }

}
