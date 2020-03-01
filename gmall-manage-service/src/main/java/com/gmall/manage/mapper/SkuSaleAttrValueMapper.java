package com.gmall.manage.mapper;

import com.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    List<Map> getSaleAttrValuesBySpu(String spuId);
}
