package com.gmall.service;

import com.gmall.bean.OrderInfo;
import com.gmall.enums.ProcessStatus;

public interface OrderService {
    String saveOrder(OrderInfo orderInfo);
    OrderInfo getOrderInfo(String orderId);
    //跳往结算页面时生成token
    String genToken(String userId);
    //验证token
    boolean verifyToken(String userId,String token);

    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);
}
