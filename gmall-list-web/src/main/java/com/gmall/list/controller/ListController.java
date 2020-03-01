package com.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.SkuLsParams;
import com.gmall.bean.SkuLsResult;
import com.gmall.service.ListService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListController {
    @Reference
    ListService listService;
    @PostMapping("list.html")
    public  String getList(@RequestBody SkuLsParams skuLsParams){
        SkuLsResult search = listService.search(skuLsParams);
        return JSON.toJSONString(search);
    }
}
