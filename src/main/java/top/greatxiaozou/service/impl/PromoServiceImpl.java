package top.greatxiaozou.service.impl;

import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.greatxiaozou.dao.PromoDoMapper;
import top.greatxiaozou.dataobject.PromoDo;
import top.greatxiaozou.service.ItemService;
import top.greatxiaozou.service.PromoService;
import top.greatxiaozou.service.model.ItemModel;
import top.greatxiaozou.service.model.PromoModel;

import java.math.BigDecimal;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDoMapper promoDoMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

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
