package com.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.annotation.LoginRequire;
import com.gmall.bean.*;
import com.gmall.enums.OrderStatus;
import com.gmall.enums.ProcessStatus;
import com.gmall.service.CartService;
import com.gmall.service.ManageService;
import com.gmall.service.OrderService;
import com.gmall.service.UserInfoService;
import com.gmall.util.HttpClientUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.DateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Controller
public class OrderController {

    @Reference
    UserInfoService userInfoService;
    @Reference
    ManageService manageService;
    @Reference
    CartService cartService;
    @Reference
    OrderService orderService;
    @GetMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        String userId =(String)request.getAttribute("userId");
        //  用户地址  列表
        List<UserAddress> userAddressList = userInfoService.getUserAddressList(userId);

        request.setAttribute("userAddressList",userAddressList);

        //  用户需要结账的商品清单
        List<CartInfo> checkedCartList = cartService.getCartCheckedList(userId);
        BigDecimal totalAmount = new BigDecimal("0");
        for (CartInfo cartInfo : checkedCartList) {
            BigDecimal cartInfoAmount = cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
            totalAmount= totalAmount.add(cartInfoAmount);
        }
        String token = orderService.genToken(userId);
        request.setAttribute("tradeNo",token);

        request.setAttribute("checkedCartList",checkedCartList);

        request.setAttribute("totalAmount",totalAmount);

        return  "trade";
    }


    @RequestMapping(value = "submitOrder",method = RequestMethod.POST)
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request){
        // 检查tradeCode
        String userId = (String) request.getAttribute("userId");
        String tradeNo = request.getParameter("tradeNo");
        boolean isEnableToken = orderService.verifyToken(userId, tradeNo);
        if(!isEnableToken){
            request.setAttribute("errMsg","页面已失效，请重新结算！");
            return "tradeFail";
        }
        // 初始化参数
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setCreateTime(new Date());
        orderInfo.setExpireTime(DateUtils.addMinutes(new Date(),15));
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            SkuInfo skuInfo = manageService.skuInfo(orderDetail.getSkuId());
            orderDetail.setImgUrl(skuInfo.getSkuDefaultImg());
            orderDetail.setSkuName(skuInfo.getSkuName());

            //验价
            if(!orderDetail.getOrderPrice().equals(skuInfo.getPrice())){
                request.setAttribute("errMsg","价格发生改动，请重新下单！");
                return "tradeFail";
            }
        }
        //验库存 使用多线程
        List<OrderDetail> errList= Collections.synchronizedList(new ArrayList<>());
        Stream<CompletableFuture<String>> completableFutureStream = orderDetailList.stream().map(orderDetail ->
                CompletableFuture.supplyAsync(() -> checkSkuNum(orderDetail)).whenComplete((hasStock, ex) -> {
                    if (hasStock.equals("0")) {
                        errList.add(orderDetail);
                    }
                })
        );
        CompletableFuture[] completableFutures = completableFutureStream.toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(completableFutures).join();

        if(errList.size()>0){
            StringBuffer errStingbuffer=new StringBuffer();
            for (OrderDetail orderDetail : errList) {
                errStingbuffer.append("商品："+orderDetail.getSkuName()+"库存暂时不足！");
            }
            request.setAttribute("errMsg",errStingbuffer.toString());
            return  "tradeFail";
        }
        // 保存
        String orderId = orderService.saveOrder(orderInfo);
        //删除购物车
        //cartService.
        // 重定向
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    //调用库存模块
    public String checkSkuNum(OrderDetail orderDetail){
        String hasStock = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + orderDetail.getSkuId() + "&num=" + orderDetail.getSkuNum());
        return  hasStock;
    }
}
