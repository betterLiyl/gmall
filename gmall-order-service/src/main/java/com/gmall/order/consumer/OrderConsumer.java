package com.gmall.order.consumer;

import com.gmall.enums.ProcessStatus;
import com.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {
    @Autowired
    OrderService orderService;

    /**
     * 消费来至支付模块的消息，并更新订单状态
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public  void  consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println("result = " + result);
        System.out.println("orderId = " + orderId);
        if ("success".equals(result)){
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 通知减库存
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }
    //消费减库存结果
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String  status = mapMessage.getString("status");
        if("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(  orderId , ProcessStatus.WAITING_DELEVER);
        }else{
            orderService.updateOrderStatus(  orderId , ProcessStatus.STOCK_EXCEPTION);
        }
    }


}
