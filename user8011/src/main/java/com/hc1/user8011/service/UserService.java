package com.hc1.user8011.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hc1.user8011.bean.File;
import com.hc1.user8011.bean.User;

public interface UserService extends IService<User> {
    User selectById(Integer id);
    String updateUserById(User user);
    void updateUserAvatarById(Integer id,String avatar);
    void addPointsOneById(Integer id);
    void addPointsFiveById(Integer id);
    void reducePointsFiveById(Integer id);
    //检查用户今天的签到状态
    boolean checkTodayStatus(Integer id);
    //用户点击签到（返回连续签到天数，-1表示今日已签）
    int doCheckIn(Integer id);
    void addPointsBatch(Integer userid, Integer points);
    String addLike(int fileId,int userId);
    String removeLike(int fileId,int userId);
    /** 批量查询用户对多个文件的点赞状态 */
    java.util.Map<Integer, Boolean> batchCheckLikes(int userId, java.util.List<Integer> fileIds);
    /** 查询用户对单个文件的点赞状态 */
    boolean checkLike(int fileId, int userId);

    void updateUserStatusById(int status, int userId);
}
