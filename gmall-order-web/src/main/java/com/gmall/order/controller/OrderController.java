package com.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.UserInfo;
import com.gmall.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    @Reference
    UserInfoService userInfoService;

    @RequestMapping("trade")
    public List<UserInfo> trade() {
        List<UserInfo> userInfoList = userInfoService.getUserInfoList();
        return userInfoList;
    }
}
