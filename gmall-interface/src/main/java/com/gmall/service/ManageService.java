package com.gmall.service;

import com.gmall.bean.*;

import java.util.List;
import java.util.Map;

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
    // 查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();
    //保存spu
    public void saveSpuInfo(SpuInfo spuInfo);
    //  根据三级id获取spu
    List<SpuInfo> getSpuList(String catalog3Id);

    // 根据spuId获取spuImage中的所有图片列表
    List<SpuImage> getSpuImageList(String spuId);
    //  根据三级id获取销售属性
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);
    //保存sku
    void saveSkuInfo(SkuInfo skuInfo);

    //用于显示商品详情 查询sku
    SkuInfo skuInfo(String skuId);
    //用商品详情 显示当前商品的销售属性和值，并标红
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);
    //切换销售属性值时，得到对应sku
    public Map getSkuValueIdsMap(String spuId);
}
