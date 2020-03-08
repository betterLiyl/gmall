package com.gmall.payment.consumer;

import com.gmall.bean.PaymentInfo;
import com.gmall.enums.PaymentStatus;
import com.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Autowired
    PaymentService paymentService;

    //接收延迟队列的消费端
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeCheckResult(MapMessage mapMessage) throws JMSException {
        int delaySec = mapMessage.getInt("delaySec");
        String outTradeNo = mapMessage.getString("outTradeNo");
        int checkCount = mapMessage.getInt("checkCount");

        PaymentInfo paymentInfo=new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentStatus paymentStatus = paymentService.checkAlipayPayment(paymentInfo);
        if(paymentStatus==PaymentStatus.UNPAID&&checkCount>0){
            System.out.println("checkCount = " + checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }

    }


}

