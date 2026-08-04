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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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


    @Transactional(rollbackFor = Exception.class)
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteNotice(int noticeId) {
        noticeMapper.deleteById(noticeId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateNotice(String title, String content, int noticeId) {
        noticeMapper.updateNoticeById(title, content, noticeId);
    }

    @Override
    public void refreshNoticeListCache() {
        // 查询最新 10 条公告
        Page<notice> page = new Page<>(1, 10);
        QueryWrapper<notice> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        noticeMapper.selectPage(page, wrapper);

        // 转为简单 Map 列表（避免跨模块反序列化问题）
        List<Map<String, Object>> list = page.getRecords().stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("createTime", n.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        // 写入 Redis 列表，供 user8011 读取
        redisUtil.del("notice:list");
        if (!list.isEmpty()) {
            redisUtil.lSet("notice:list", (List<Object>) (List<?>) list, 3600);
        }
    }

}
