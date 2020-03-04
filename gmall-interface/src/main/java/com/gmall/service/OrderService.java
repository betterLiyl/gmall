package com.gmall.service;

import com.gmall.bean.OrderInfo;

public interface OrderService {
    void saveOrder(OrderInfo orderInfo);
    //跳往结算页面时生成token
    String genToken(String userId);
    //验证token
    boolean verifyToken(String userId,String token);

}
