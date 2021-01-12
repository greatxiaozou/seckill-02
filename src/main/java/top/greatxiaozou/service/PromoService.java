package top.greatxiaozou.service;

import org.springframework.stereotype.Service;
import top.greatxiaozou.service.model.PromoModel;

@Service
public interface PromoService {
    //根据itemid获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);
}
