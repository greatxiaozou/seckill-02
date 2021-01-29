package top.greatxiaozou.service;

import org.springframework.stereotype.Service;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.service.model.PromoModel;


public interface PromoService {
    //根据itemid获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    //活动发布(生成缓存）
    void publishPromo(Integer promoId);

    //生成秒杀用的令牌
    String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId) throws BusinessException;
}
