package com.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.SkuInfo;
import com.gmall.bean.SpuSaleAttr;
import com.gmall.service.ListService;
import com.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin
public class ItemController {
    @Reference
    ManageService manageService;
    @Reference
    ListService listService;
    @GetMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId, HttpServletRequest request){
        SkuInfo skuInfo = manageService.skuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        // 存储 spu，sku数据
        List<SpuSaleAttr> saleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("saleAttrList",saleAttrList);

        //得到属性组合与skuid的映射关系 ，用于页面根据属性组合进行跳转
        Map skuValueIdsMap = manageService.getSkuValueIdsMap(skuInfo.getSpuId());
        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        listService.incrHotScore(skuId);  //最终应该由异步方式调用

        return "item";
    }
}
