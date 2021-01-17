package top.greatxiaozou.service;

import org.springframework.stereotype.Service;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.service.model.OrderModel;


public interface OrderService {
    //创建订单
    //通过前端url上传过来的秒杀轰动id，在下单接口内校验对应id是否属于对应商品且活动已开始
    OrderModel createOrder(Integer userId,Integer itemId
            ,Integer promoId,Integer amount
            ,String stockLogId) throws BusinessException;


}
