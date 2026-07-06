package com.hc1.user8011.service;


import com.hc1.user8011.bean.User;


public interface RegisterService {

   String register(User user);
   User selectByUsername(String username);

}
