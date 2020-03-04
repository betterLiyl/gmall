package com.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.annotation.LoginRequire;
import com.gmall.bean.CartInfo;
import com.gmall.cart.handler.CartCookieHandler;
import com.gmall.service.CartService;
import com.gmall.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    CartService cartService;


    @PostMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public  String  addCart(@RequestParam("skuId") String skuId, @RequestParam("num") int num, HttpServletRequest request, HttpServletResponse response){
        String userId =(String) request.getAttribute("userId");

        if(userId==null){
            //如果用户未登录  检查cookie用户是否有token 如果有token  用token 作为id 加购物车 如果没有生成一个新的token放入cookie
            userId = CookieUtil.getCookieValue(request, "user_tmp_id", false);
            if(userId==null){
                userId = UUID.randomUUID().toString();
                CookieUtil.setCookie(request,response,"user_tmp_id",userId,60*60*24*7,false);
            }

        }
        CartInfo cartInfo = cartService.addCart(userId, skuId, num);
        request.setAttribute("cartInfo",cartInfo);
        request.setAttribute("num",num);

        return "success";
    }


    @GetMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public  String  cartList(HttpServletRequest request){
        String userId =(String) request.getAttribute("userId");
        List<CartInfo> cartList=null;
        if(userId!=null){
            cartList =  cartService.cartList(  userId);
        }

        String userTmpId=CookieUtil.getCookieValue(request, "user_tmp_id", false);;
        List<CartInfo> cartTempList=null;
        if(userTmpId!=null){
            cartTempList =  cartService.cartList(  userTmpId);
            cartList=cartTempList;
        }
        if(userId!=null &&cartTempList!=null&&cartTempList.size()>0){
            cartList=  cartService.mergeCartList(userId,userTmpId);
        }

        request.setAttribute("cartList",cartList);

        return "cartList";

    }
    @Autowired
    CartCookieHandler cartCookieHandler;

    @PostMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId=(String) request.getAttribute("userId");
        if (userId!=null){
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @PostMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
        if (cookieHandlerCartList!=null && cookieHandlerCartList.size()>0){
            cartService.mergeToCartList(cookieHandlerCartList, userId);
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }
}
