package top.greatxiaozou.service.impl;

import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.greatxiaozou.dao.PromoDoMapper;
import top.greatxiaozou.dataobject.PromoDo;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.service.ItemService;
import top.greatxiaozou.service.PromoService;
import top.greatxiaozou.service.UserService;
import top.greatxiaozou.service.model.ItemModel;
import top.greatxiaozou.service.model.PromoModel;
import top.greatxiaozou.service.model.UserModel;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDoMapper promoDoMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDo promoDo = promoDoMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromDataObject(promoDo);
        if(promoModel == null) return null;

        //判断当前时间是否秒杀活动即将开始或正在进行
//        DateTime now = new DateTime();
        if (promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if (promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }

        return promoModel;

    }

    @Override
    public void publishPromo(Integer promoId) {
        //通过活动Id获取活动
        PromoDo promoDo = promoDoMapper.selectByPrimaryKey(promoId);
        if (promoDo.getItemId()==null || promoDo.getItemId().intValue()==0){
            return;
        }

        ItemModel itemModel = itemService.getItemById(promoDo.getItemId());

        //将库存同步到Redis内部
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock());

        //将大闸的限制数字设置到redis内
        //5倍库存
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue()*5);


    }

    @Override
    public String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId) throws BusinessException {
        //库存是否售完
        Boolean isInvalid = redisTemplate.hasKey("promo_item_stock_invalid_" + itemId);
        if (isInvalid){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        PromoDo promoDo = promoDoMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertFromDataObject(promoDo);

        if(promoModel == null){
            return null;
        }

        //判断活动是否即将开始或者正在进行
        if (promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if (promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }

        //只有model的status字段值为2才允许生成令牌
        if (promoModel.getStatus()!=2){
            return null;
        }

        //用户和商品校验
        //1. 校验下单状态，下单商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品不存在");
            return null;
        }

        //2. 校验用户是否合法
        UserModel userModel = userService.getUserByIdInCache(userId);

        if (userModel == null){
            //throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
            return null;
        }

        //获取秒杀大闸的count数量
        Long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (result < 0){
            return null;
        }


        //生成token并存入redis内，并设置一个5分钟的有效期
        String token = UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,token);
        //秒杀令牌五分钟失效
        redisTemplate.expire("promo_token_"+promoId+"_userid_"+userId+"_itemId_"+itemId,5, TimeUnit.MINUTES);

        return token;
    }

    //==============convert方法==============//
    private PromoModel convertFromDataObject(PromoDo promoDo){
        if (promoDo == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDo,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDo.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDo.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDo.getEndDate()));

        return promoModel;
    }
}
