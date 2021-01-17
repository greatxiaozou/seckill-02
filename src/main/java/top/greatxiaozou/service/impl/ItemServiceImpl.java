package top.greatxiaozou.service.impl;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.greatxiaozou.dao.ItemDoMapper;
import top.greatxiaozou.dao.ItemStockDoMapper;
import top.greatxiaozou.dao.StockLogDoMapper;
import top.greatxiaozou.dataobject.ItemDo;
import top.greatxiaozou.dataobject.ItemStockDo;
import top.greatxiaozou.dataobject.StockLogDo;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.mq.MqProducer;
import top.greatxiaozou.service.ItemService;
import top.greatxiaozou.service.PromoService;
import top.greatxiaozou.service.model.ItemModel;
import top.greatxiaozou.service.model.PromoModel;
import top.greatxiaozou.validator.ValidationResult;
import top.greatxiaozou.validator.ValidatorImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDoMapper itemDoMapper;

    @Autowired
    private ItemStockDoMapper itemStockDoMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private StockLogDoMapper stockLogDoMapper;


    //创建商品
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validate(itemModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }

        //转化--->dataobject
        ItemDo itemDo = convertItemDoFromModel(itemModel);

        itemDoMapper.insertSelective(itemDo);

        itemModel.setId(itemDo.getId());
        ItemStockDo stockDo = convertItemStockDoFromModel(itemModel);

        itemStockDoMapper.insertSelective(stockDo);

        //返回写入完成的对象
        return this.getItemById(itemModel.getId());
    }

    //展示所有商品
    @Override
    public List<ItemModel> listItem() {
        List<ItemDo> itemDos = itemDoMapper.listItem();
        List<ItemModel> list = itemDos.stream().map(itemDo -> {
            ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());
            ItemModel itemModel = convertItemModelFromDo(itemDo, itemStockDo);

            return itemModel;
        }).collect(Collectors.toList());
        return list;
    }

    //根据ID获取商品信息
    @Override
    public ItemModel getItemById(Integer id) {
        ItemDo itemDo = itemDoMapper.selectByPrimaryKey(id);
//        System.out.println(itemDo);

        if (itemDo == null){
            return null;
        }
        //获得库存数量
        ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());

        //dataObject --->model
        ItemModel itemModel = convertItemModelFromDo(itemDo,itemStockDo);

        //获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        //System.out.println(promoModel);

        //将活动聚合到itemModel中
        if (promoModel != null && promoModel.getStatus().intValue() != 3){
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }


    //缓存方法
    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel)redisTemplate.opsForValue().get("item_validate_" + id);

        if(itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_"+id,itemModel);
            redisTemplate.expire("item_validate_"+id,10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    //减库存的方法
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        //int affectedRow = itemStockDoMapper.decreaseStock(itemId, amount);

        //减库存缓存化
        Long res = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue() * -1);
        if (res >= 0){
            //更新库存成功
            return true;
        }else {
            //更新库存失败,缓存回滚
            increaseStock(itemId,amount);
            return false;
        }
                
    }
    //销量增加
    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDoMapper.increaseSales(itemId,amount);
    }

    //异步更新库存的方法
    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult = mqProducer.asyncReduceStock(itemId, amount);
        return mqResult;
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) throws BusinessException {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    //初始化流水并插入
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDo stockLogDo = new StockLogDo();
        stockLogDo.setItemId(itemId);
        stockLogDo.setAmount(amount);
        stockLogDo.setStockLogId(UUID.randomUUID().toString().replace("-",""));
        stockLogDo.setStatus(1);

        //
        stockLogDoMapper.insertSelective(stockLogDo);

        return stockLogDo.getStockLogId();

    }





    //========================convert方法======================//
    //将model对象转化为itemdo对象
    public ItemDo convertItemDoFromModel(ItemModel itemModel){
        if (itemModel==null) return null;

        ItemDo itemDo = new ItemDo();
        BeanUtils.copyProperties(itemModel,itemDo);
        itemDo.setPrice(itemModel.getPrice().doubleValue());

        return itemDo;
    }

    //将itemModel转化为ItemStockDo对象
    public ItemStockDo convertItemStockDoFromModel(ItemModel itemModel){
        if (itemModel == null ) return null;

        ItemStockDo itemStockDo= new ItemStockDo();
        itemStockDo.setItemId(itemModel.getId());
        itemStockDo.setStock(itemModel.getStock());

        return itemStockDo;
    }

    //将do对象转换为itemModel对象
    public ItemModel convertItemModelFromDo(ItemDo itemDo,ItemStockDo itemStockDo){
        if (itemDo == null || itemStockDo == null) return null;

        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDo,itemModel);
        itemModel.setPrice(new BigDecimal(itemDo.getPrice()));
        itemModel.setStock(itemStockDo.getStock());
        return itemModel;
    }

    //==========生成订单号方法===========//


}
