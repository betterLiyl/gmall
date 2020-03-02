package com.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.gmall.bean.*;
import com.gmall.manage.mapper.*;
import com.gmall.service.ManageService;
import com.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ManageServiceImpl implements ManageService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;


    @Autowired
    SpuImageMapper spuImageMapper;
    @Autowired
    SpuInfoMapper spuInfoMapper;
    @Autowired
    SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    BaseCatalog3Mapper baseCatalog3Mapper;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        List<BaseCatalog2> catalog2List = baseCatalog2Mapper.select(baseCatalog2);
        return catalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        List<BaseCatalog3> catalog3List = baseCatalog3Mapper.select(baseCatalog3);
        return catalog3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
//        Example example = new Example(BaseAttrInfo.class);
//        example.createCriteria().andEqualTo("catalog3Id", catalog3Id);
//        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectByExample(example);
        List<BaseAttrInfo> baseAttrInfoListByCatalog3Id = baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
        return baseAttrInfoListByCatalog3Id;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        String attrValueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.getBaseAttrInfoListByValueIds(attrValueIds);
        return baseAttrInfoList;

    }
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //如果有主键就进行更新，如果没有就插入
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        } else {

            baseAttrInfo.setId(null);
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //把原属性值全部清空
        BaseAttrValue baseAttrValue4Del = new BaseAttrValue();
        baseAttrValue4Del.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue4Del);

        //重新插入属性值
        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                //防止主键被赋上一个空字符串

                attrValue.setId(null);
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        // 创建属性对象
        BaseAttrInfo attrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // 创建属性值对象
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        // 根据attrId字段查询对象
        baseAttrValue.setAttrId(attrInfo.getId());
        List<BaseAttrValue> attrValueList = baseAttrValueMapper.select(baseAttrValue);
        // 给属性对象中的属性值集合赋值
        attrInfo.setAttrValueList(attrValueList);
        // 将属性对象返回
        return attrInfo;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        // 什么情况下是保存，什么情况下是更新 spuInfo
        if (spuInfo.getId() == null || spuInfo.getId().length() == 0) {
            //保存数据
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        } else {
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }

        //  spuImage 图片列表 先删除，在新增
        //  delete from spuImage where spuId =?
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);

        // 保存数据，先获取数据
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            // 循环遍历
            for (SpuImage image : spuImageList) {
                image.setId(null);
                image.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(image);
            }
        }
        // 销售属性 删除，插入
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        // 销售属性值 删除，插入
        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        // 获取数据
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            // 循环遍历
            for (SpuSaleAttr saleAttr : spuSaleAttrList) {
                saleAttr.setId(null);
                saleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(saleAttr);

                // 添加销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    // 循环遍历
                    for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                        saleAttrValue.setId(null);
                        saleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(saleAttrValue);
                    }
                }

            }
        }
    }

    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> select = spuInfoMapper.select(spuInfo);
        return select;
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        // sku_info
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            // 设置id 为自增
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }

        //        sku_img,
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        // insert
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage image : skuImageList) {
                /* "" 区别 null*/
                if (image.getId()!=null && image.getId().length()==0){
                    image.setId(null);
                }
                // skuId 必须赋值
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }
//        sku_attr_value,
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        // 插入数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue attrValue : skuAttrValueList) {
                if (attrValue.getId()!=null && attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                // skuId
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(attrValue);
            }
        }
//        sku_sale_attr_value,
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);
//      插入数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                if (saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                    saleAttrValue.setId(null);
                }
                // skuId
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }

    /**
     * 通过直接查数据库获取sku详情
     * @param skuId
     * @return
     */
    public SkuInfo skuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        if(skuId==null){
            return null;
        }
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        //销售属性
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> select = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        skuInfo.setSkuSaleAttrValueList(select);

        //平台属性
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValues);

        return skuInfo;
    }

    public static final String SKUKEY_PREFIX="sku:";
    public static final String  SKUKEY_INFO_SUFFIX = ":info";
    public static final String SKUKEY_LOCK_SUFFIX = ":lock";

    /**
     * 使用缓存优化sku详情
     * 使用redisson分布式锁解决缓存穿透问题
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo skuInfo(String skuId) {
        SkuInfo skuInfoResult=null;
        //1  先查redis  没有再查数据库
        Jedis jedis = redisUtil.getJedis();
        int SKU_EXPIRE_SEC=100;
        // redis结构 ： 1 type  string  2 key   sku:101:info  3 value  skuInfoJson
        String skuKey=SKUKEY_PREFIX+skuId+SKUKEY_INFO_SUFFIX;
        String skuInfoJson = jedis.get(skuKey);
        if(skuInfoJson!=null){
            if(!"EMPTY".equals(skuInfoJson)){
                System.out.println(Thread.currentThread()+"命中缓存！！");
                skuInfoResult = JSON.parseObject(skuInfoJson, SkuInfo.class);
            }
        }else{
            Config config = new Config();
            config.useSingleServer().setAddress("redis://47.103.93.216:6379");
            RedissonClient redissonClient = Redisson.create(config);
            String lockKey=SKUKEY_PREFIX+skuId+SKUKEY_LOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            // lock.lock(10,TimeUnit.SECONDS);
            boolean locked=false ;
            try {
                locked = lock.tryLock(10, 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(locked) {
                System.out.println(Thread.currentThread() + "得到锁！！");
                // 如果得到锁后能够在缓存中查询 ，那么直接使用缓存数据 不用在查询数据库
                System.out.println(Thread.currentThread()+"再次查询缓存！！");
                String skuInfoJsonResult = jedis.get(skuKey);
                if (skuInfoJsonResult != null) {
                    if (!"EMPTY".equals(skuInfoJsonResult)) {
                        System.out.println(Thread.currentThread() + "命中缓存！！");
                        skuInfoResult = JSON.parseObject(skuInfoJsonResult, SkuInfo.class);
                    }

                } else {
                    skuInfoResult = skuInfoDB(skuId);

                    System.out.println(Thread.currentThread() + "写入缓存！！");

                    if (skuInfoResult != null) {
                        skuInfoJsonResult = JSON.toJSONString(skuInfoResult);
                    } else {
                        skuInfoJsonResult = "EMPTY";
                    }
                    jedis.setex(skuKey, SKU_EXPIRE_SEC, skuInfoJsonResult);
                }
                lock.unlock();
            }

        }
        return skuInfoResult;
    }

    /**
     * 使用redis原生解决缓存穿透问题，也可实现分布式锁
     * @param skuId
     * @return
     */
    public SkuInfo getSkuInfo_redis(String skuId) {

        SkuInfo skuInfoResult=null;
        //1  先查redis  没有再查数据库
        Jedis jedis = redisUtil.getJedis();
        int SKU_EXPIRE_SEC=100;
        // redis结构 ： 1 type  string  2 key   sku:101:info  3 value  skuInfoJson
        String skuKey=SKUKEY_PREFIX+skuId+SKUKEY_INFO_SUFFIX;
        String skuInfoJson = jedis.get(skuKey);
        if(skuInfoJson!=null){
            if(!"EMPTY".equals(skuInfoJson)){
                System.out.println(Thread.currentThread()+"命中缓存！！");
                skuInfoResult = JSON.parseObject(skuInfoJson, SkuInfo.class);
            }

        }else{
            System.out.println(Thread.currentThread()+"未命中！！");
            //setnx     1  查锁   exists 2 抢锁  set
            //定义一下 锁的结构   type  string     key  sku:101:lock      value  locked
            String lockKey=SKUKEY_PREFIX+skuId+SKUKEY_LOCK_SUFFIX;
//            Long locked = jedis.setnx(lockKey, "locked");
//            jedis.expire(lockKey,10);
            String token= UUID.randomUUID().toString();
            String locked = jedis.set(lockKey, token, "NX", "EX", 100);
            if("OK".equals(locked)){
                System.out.println(Thread.currentThread()+"得到锁！！");

                skuInfoResult = skuInfoDB(skuId);

                System.out.println(Thread.currentThread()+"写入缓存！！");
                String skuInfoJsonResult=null;
                if(skuInfoResult!=null){
                    skuInfoJsonResult = JSON.toJSONString(skuInfoResult);
                }else{
                    skuInfoJsonResult="EMPTY";
                }
                jedis.setex(skuKey,SKU_EXPIRE_SEC,skuInfoJsonResult);
                System.out.println(Thread.currentThread()+"释放锁！！"+lockKey);
                if(jedis.exists(lockKey)&&token.equals(jedis.get(lockKey))){   // 不完美 ，可以用lua解决
                    jedis.del(lockKey);
                }

            }else{
                System.out.println(Thread.currentThread()+"为得到锁，开始自旋等待！！");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                skuInfo(  skuId);
            }

        }

        jedis.close();
        return   skuInfoResult;

    }
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public Map getSkuValueIdsMap(String spuId) {
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        Map skuValueIdsMap =new HashMap();

        for (Map  map : mapList) {
            String skuId = map.get("sku_id") +"";
            String valueIds =(String ) map.get("value_ids");
            skuValueIdsMap.put(valueIds,skuId);

        }
        return skuValueIdsMap;
    }
}
