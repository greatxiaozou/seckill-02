package top.greatxiaozou.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.mq.MqProducer;
import top.greatxiaozou.response.CommonReturnType;
import top.greatxiaozou.service.ItemService;
import top.greatxiaozou.service.OrderService;
import top.greatxiaozou.service.model.OrderModel;
import top.greatxiaozou.service.model.UserModel;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin(origins = {"*"},allowCredentials = "true")
@Controller("order")
@RequestMapping("/order")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer producer;

    @Autowired
    private ItemService itemService;


    //用户下单接口
    @ResponseBody
    @RequestMapping(method = {RequestMethod.POST},value = "/createorder",consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam("itemId")Integer itemId,
                                        @RequestParam("amount")Integer amount,
                                        @RequestParam(value = "promoId",required = false)Integer promoId) throws BusinessException {
        //获取用户登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];

        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }


        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }


        //UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        //OrderModel orderModel = orderService.createOrder(userModel.getId(), promoId,itemId, amount);

        //加入库存流水init状态
        String stockLogId = itemService.initStockLog(itemId, amount);


        //加入流水之后，再完成对应的下单事务型消息机制
        boolean result = producer.transactionAsyncReduceStock(userModel.getId(),itemId,promoId,amount,stockLogId);
        if(!result){
            throw new BusinessException(EmBusinessError.UNKONW_ERROR,"下单失败");
        }

        return CommonReturnType.create(null);
    }
}
