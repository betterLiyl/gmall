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
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.DateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserInfoService userInfoService;
    @Reference
    ManageService manageService;
    @Reference
    private CartService cartService;
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
        }
        // 保存
         orderService.saveOrder(orderInfo);
        // 重定向
        return "redirect://payment.gmall.com/index";
    }
}
