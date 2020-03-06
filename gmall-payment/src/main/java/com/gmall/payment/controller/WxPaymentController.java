package com.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.wxpay.sdk.WXPayUtil;
import com.gmall.payment.util.StreamUtil;
import com.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WxPaymentController {
    @Reference
    PaymentService paymentService;


    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @PostMapping("wx/submit")
    @ResponseBody
    public Map createNative(String orderId){
        //  // 做一个判断：支付日志中的订单支付状态 如果是已支付，则不生成二维码直接重定向到消息提示页面！
// 调用服务层数据
// 第一个参数是订单Id ，第二个参数是多少钱，单位是分
        Map map = paymentService.createNative(orderId +"", "1");
        System.out.println(map.get("code_url"));
// data = map
        return map;
    }
    @PostMapping("/wx/callback/notify")
    public String notify(HttpServletRequest request, HttpServletResponse response ) throws Exception {
        //  0 获得值
        ServletInputStream inputStream = request.getInputStream();
        String xmlString = StreamUtil.inputStream2String(inputStream,"utf-8");

        // 1 验签
        if( WXPayUtil.isSignatureValid(xmlString,partnerkey )){
            //2 判断状态
            Map<String, String> paramMap = WXPayUtil.xmlToMap(xmlString);
            String result_code = paramMap.get("result_code");
            if(result_code!=null&&result_code.equals("SUCCESS")){
                // 3 更新支付状态  包发送 消息给订单

                //  4  准备返回值 xml
                HashMap<String, String> returnMap = new HashMap<>();
                returnMap.put("return_code","SUCCESS");
                returnMap.put("return_msg","OK");

                String returnXml = WXPayUtil.mapToXml(returnMap);
                response.setContentType("text/xml");
                System.out.println("交易编号："+paramMap.get("out_trade_no")+"支付成功！");
                return  returnXml;

            }else{
                System.out.println(paramMap.get("return_code")+"---"+paramMap.get("return_msg"));
            }
        }
        return  null;
    }
}
