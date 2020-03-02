package com.gmall.service;

import com.gmall.bean.SkuLsInfo;
import com.gmall.bean.SkuLsParams;
import com.gmall.bean.SkuLsResult;

public interface ListService {

    public void saveSkuInfo(SkuLsInfo skuLsInfo);
    //全文搜索
    public SkuLsResult search(SkuLsParams skuLsParams);
    //增加商品热度评分 用来排序
    public void incrHotScore(String skuId);
}
