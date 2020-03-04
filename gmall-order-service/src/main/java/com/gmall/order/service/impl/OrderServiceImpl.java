package com.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.gmall.bean.OrderDetail;
import com.gmall.bean.OrderInfo;
import com.gmall.order.mapper.OrderDetailMapper;
import com.gmall.order.mapper.OrderInfoMapper;
import com.gmall.service.OrderService;
import com.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    RedisUtil redisUtil;
    //保存订单
    @Override
    @Transactional
    public void saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
    }

    @Override
    public String genToken(String userId) {
        // token type:string key:
        String token = UUID.randomUUID().toString();
        String tokenKey = "user:"+userId+":trade_ code";
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(tokenKey,10*60,token);
        jedis.close();
        return token;
    }

    @Override
    public boolean verifyToken(String userId, String token) {
        String tokenKey = "user:"+userId+":trade_ code";
        Jedis jedis = redisUtil.getJedis();
        String tokenExists = jedis.get(tokenKey);
        jedis.watch(tokenKey);
        Transaction transaction = jedis.multi();
        if(tokenExists != null && tokenExists.equals(token)){
            transaction.del(tokenKey);
        }
        List<Object> list = transaction.exec();
        if(list!=null && list.size()>0 && (Long)list.get(0)==1L){
            return true;
        }else {
            return false;
        }
    }

}
