package com.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.OrderDetail;
import com.gmall.bean.OrderInfo;
import com.gmall.enums.ProcessStatus;
import com.gmall.order.mapper.OrderDetailMapper;
import com.gmall.order.mapper.OrderInfoMapper;
import com.gmall.service.OrderService;
import com.gmall.util.ActiveMQUtil;
import com.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

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
    public String saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return  orderInfo.getId();
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
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

    @Override
    public void updateOrderStatus(String orderId,ProcessStatus processStatus){
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Override
    public void sendOrderStatus(String orderId){
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);

            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);
            producer.send(textMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public String initWareOrder(String orderId){
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }
    // 设置初始化仓库信息方法
    public Map  initWareOrder (OrderInfo orderInfo){
        Map<String,Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        // 组合json
        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map detailMap = new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }
}
