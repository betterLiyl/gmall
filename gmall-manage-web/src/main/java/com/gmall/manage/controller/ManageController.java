package com.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gmall.bean.*;
import com.gmall.service.ListService;
import com.gmall.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

    @Reference
    ManageService manageService;
    @Reference
    ListService listService;
    @PostMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> catalog1 = manageService.getCatalog1();
        return catalog1;
    }

    @PostMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        List<BaseCatalog2> catalog2 = manageService.getCatalog2(catalog1Id);
        return catalog2;
    }

    @PostMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        List<BaseCatalog3> catalog3 = manageService.getCatalog3(catalog2Id);
        return catalog3;
    }
    //根据三级分类id获取平台属性
    @GetMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){

        return manageService.getAttrList(catalog3Id);
    }

    /**
     * 新增平台属性的保存方法
     * @param baseAttrInfo
     */
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        // 调用服务层做保存方法
        manageService.saveAttrInfo(baseAttrInfo);
    }

    @RequestMapping(value = "getAttrValueList",method = RequestMethod.POST)
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
        return attrInfo.getAttrValueList();
    }

    /**
     * 获取基本销售属性
     * @return
     */
    @PostMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return   manageService.getBaseSaleAttrList();
    }
    // 保存
    @PostMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return "success";
    }
    //  根据三级id获取spu
    @GetMapping("spuList")
    public List<SpuInfo> getSpuList(String catalog3Id){
        List<SpuInfo> spuList = manageService.getSpuList(catalog3Id);
        return spuList;
    }

    /**
     * 根据spuId获取图片
     * @param spuId
     * @return
     */
    @GetMapping("spuImageList")
    public List<SpuImage> spuImageList(String spuId){
        return   manageService.getSpuImageList(spuId);
    }

    @GetMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getspuSaleAttrList(String spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return  spuSaleAttrList;
    }

    /**
     * 通过skuId将skuInfo存入es中
     * @param skuId
     */
    @PostMapping("onSale")
    public void onSale(@RequestParam("skuId")String skuId){
        SkuInfo skuInfo = manageService.skuInfo(skuId);
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // 属性拷贝
        try {
            BeanUtils.copyProperties(skuLsInfo,skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        listService.saveSkuInfo(skuLsInfo);
    }

}
