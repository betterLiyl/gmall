package com.gmall.service;

import com.gmall.bean.BaseAttrInfo;
import com.gmall.bean.BaseCatalog1;
import com.gmall.bean.BaseCatalog2;
import com.gmall.bean.BaseCatalog3;

import java.util.List;

public interface ManageService {
    //查询一级分类
    public List<BaseCatalog1> getCatalog1();

    //查询二级分类，根据一级分类id
    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    //查询三级分类，根据二级分类id
    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类id查询平台属性
    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    // 新增平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);
    //选中准修改数据 ， 根据该attrId 去查找AttrInfo，该对象下 List<BaseAttrValue>
    BaseAttrInfo getAttrInfo(String attrId);
}
