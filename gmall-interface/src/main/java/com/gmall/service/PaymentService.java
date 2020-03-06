package com.gmall.service;

import com.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void  savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);
    //退款
    boolean refund(String orderId);

    Map createNative(String s, String s1);

    void sendPaymentResult(PaymentInfo paymentInfo,String result);
}
