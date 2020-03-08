package com.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.github.wxpay.sdk.WXPayUtil;
import com.gmall.bean.PaymentInfo;
import com.gmall.enums.PaymentStatus;
import com.gmall.payment.mapper.PaymentInfoMapper;
import com.gmall.payment.util.HttpClient;
import com.gmall.service.PaymentService;
import com.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    AlipayClient alipayClient;
    @Autowired
    ActiveMQUtil activeMQUtil;
    public  void  savePaymentInfo(PaymentInfo paymentInfo){
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return   paymentInfoMapper.selectOne(paymentInfo);
    }


    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    @Override
    public boolean refund(String orderId) {

        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        PaymentInfo paymentInfo = getPaymentInfoByOrderId(orderId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("refund_amount", paymentInfo.getTotalAmount());

        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }



    private PaymentInfo getPaymentInfoByOrderId(String orderId) {
        return paymentInfoMapper.selectByPrimaryKey(orderId);
    }

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Override
    public Map createNative(String orderId, String total_fee) {
        //1.创建参数
        Map<String,String> param=new HashMap();//创建参数
        param.put("appid", appid);//公众号
        param.put("mch_id", partner);//商户号
        param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        param.put("body", "尚硅谷");//商品描述
        param.put("out_trade_no", orderId);//商户订单号
        param.put("total_fee",total_fee);//总金额（分）
        param.put("spbill_create_ip", "127.0.0.1");//IP
        param.put("notify_url", "http://order.gmall.com/trade");//回调地址(随便写)
        param.put("trade_type", "NATIVE");//交易类型
        try {
            //2.生成要发送的xml
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println(xmlParam);
            HttpClient client=new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            client.setHttps(true);
            client.setXmlParam(xmlParam);
            client.post();
            //3.获得结果
            String result = client.getContent();
            System.out.println(result);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            Map<String, String> map=new HashMap<>();
            map.put("code_url", resultMap.get("code_url"));//支付地址
            map.put("total_fee", total_fee);//总金额
            map.put("out_trade_no",orderId);//订单号
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    //   添加发送方法
    public void sendPaymentResult(PaymentInfo paymentInfo,String result){
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(paymentResultQueue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId",paymentInfo.getOrderId());
            mapMessage.setString("result",result);
            producer.send(mapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //主动检查支付状态
    public PaymentStatus checkAlipayPayment(PaymentInfo paymentInfo){

        System.out.println("开始主动检查支付状态，paymentInfo.toString() = " + paymentInfo.toString());
        //先检查当前数据库是否已经变为“已支付状态”
        if(paymentInfo.getId()==null){
            System.out.println("outTradeNo:"+paymentInfo.getOutTradeNo()  );
            paymentInfo = getPaymentInfo(paymentInfo);
        }
        if (paymentInfo.getPaymentStatus()== PaymentStatus.PAID){
            System.out.println("该单据已支付:"+paymentInfo.getOutTradeNo());
            return PaymentStatus.PAID;
        }

        //如果不是已支付，继续去查询alipay的接口
        System.out.println("%% % % 查询alipay的接口"  );
        AlipayTradeQueryRequest alipayTradeQueryRequest=new AlipayTradeQueryRequest();
        alipayTradeQueryRequest.setBizContent("{\"out_trade_no\":\""+paymentInfo.getOutTradeNo()+"\"}");
        AlipayTradeQueryResponse response=null;
        try {
            response = alipayClient.execute(alipayTradeQueryRequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }


        if(response.isSuccess()){
            String tradeStatus = response.getTradeStatus();

            if ("TRADE_SUCCESS".equals(tradeStatus)){
                System.out.println("支付完成  ======================  "    );
                //如果结果是支付成功 ,则更新支付状态
                PaymentInfo paymentInfo4Upt=new PaymentInfo();
                paymentInfo4Upt.setPaymentStatus(PaymentStatus.PAID);
                paymentInfo4Upt.setCallbackTime(new Date());
                paymentInfo4Upt.setCallbackContent(response.getBody());
                paymentInfo4Upt.setId(paymentInfo.getId());
                paymentInfoMapper.updateByPrimaryKeySelective(paymentInfo4Upt);

                // 然后发送通知给订单
                sendPaymentResult(paymentInfo,"success");
                return PaymentStatus.PAID;
            }else{
                System.out.println("支付尚未完成 ？？？？？？？？？？ "    );
                return PaymentStatus.UNPAID;
            }
        }else{
            System.out.println("支付尚未完成 ？？？？？？？？？？ "    );
            return    PaymentStatus.UNPAID;
        }


    }

    //延迟发送支付结果
    public void sendDelayPaymentResult(String outTradeNo,int delaySec,int checkCount){
        //发送支付结果
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(paymentResultQueue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            MapMessage mapMessage= new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("delaySec",delaySec);
            mapMessage.setInt("checkCount",checkCount);
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            producer.send(mapMessage);

            session.commit();
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void closePayment(String orderId){
        Example example=new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        PaymentInfo paymentInfo=new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);

    }



}
