package com.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.gmall.bean.UserInfo;
import com.gmall.usermanage.mapper.UserInfoMapper;
import com.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    UserInfoMapper userInfoMapper;
    @Override
    public List<UserInfo> getUserInfoList() {
        return userInfoMapper.selectAll();
    }
}
