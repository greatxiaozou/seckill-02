package top.greatxiaozou.controller;
import io.netty.util.internal.StringUtil;
import org.apache.ibatis.annotations.Param;
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
import top.greatxiaozou.service.PromoService;
import top.greatxiaozou.service.model.OrderModel;
import top.greatxiaozou.service.model.UserModel;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

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

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
    }


    @RequestMapping(value = "/generatetoken",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam("itemId")Integer itemId,
                                          @RequestParam("promoId")Integer promoId) throws BusinessException {

        //获取用户登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];

        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }

        //获取秒杀访问令牌
        String killToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if(killToken == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌生成错误");
        }

        return CommonReturnType.create(killToken);
    }




    //用户下单接口
    @ResponseBody
    @RequestMapping(method = {RequestMethod.POST},value = "/createorder",consumes = {CONTENT_TYPE_FORMED})
    public CommonReturnType createOrder(@RequestParam("itemId")Integer itemId,
                                        @RequestParam("amount")Integer amount,
                                        @RequestParam(value = "promoId",required = false)Integer promoId,
                                        @RequestParam(value = "promoToken",required = false)String promoToken) throws BusinessException {


        //获取用户登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];

        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }

        //验证秒杀令牌是否正确
        if (promoId != null){
            String promoTokenInRedis = (String)redisTemplate.opsForValue().get("promo_token_"+promoId+"_userid_"+userModel.getId()+"_itemid_"+itemId);
            if (promoTokenInRedis == null){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
            boolean isEquls = com.alibaba.druid.util.StringUtils.equals(promoToken, promoTokenInRedis);
            if (!isEquls){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
        }

        //UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        //OrderModel orderModel = orderService.createOrder(userModel.getId(), promoId,itemId, amount);

        //同步调用线程池的submit方法
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                //加入流水之后，再完成对应的下单事务型消息机制
                boolean result = producer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId);
                if (!result) {
                    throw new BusinessException(EmBusinessError.UNKONW_ERROR, "下单失败");
                }

                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKONW_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKONW_ERROR);
        }


        return CommonReturnType.create(null);
    }
}
