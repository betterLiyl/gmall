package com.gmall.manage.mapper;

import com.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {

    // 根据三级分类id查询属性表
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);

    List<BaseAttrInfo> getBaseAttrInfoListByValueIds(@Param("valueIds") String valueIds);
}
