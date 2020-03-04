package com.gmall.service;

import com.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    CartInfo addCart(String userId, String skuId, Integer num);

    List<CartInfo> cartList(String userId);

    // 缓存中没有数据，则从 数据库中加载
     List<CartInfo> loadCartCache(String userId);
//    // 查询购物车集合列表
//    public List<CartInfo> getCartList(String userId);
    //合并购物车
    List<CartInfo> mergeCartList(String userIdDest, String userIdOrig);
    //另一种合并购物车
    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);
    void  checkCart(String skuId,String isChecked,String userId);

    public  List<CartInfo> getCartCheckedList(String userId);
}
