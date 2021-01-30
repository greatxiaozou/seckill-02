package top.greatxiaozou.controller;
import com.google.common.util.concurrent.RateLimiter;
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
import top.greatxiaozou.utils.CodeUtil;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@SuppressWarnings("all")
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

    private RateLimiter orderCreateRateLimiter;

    //初始化线程池
    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(250);
    }

    //获取验证码接口
    @RequestMapping(value = "/generateverifycode",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，不能生成验证码");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，不能生成验证码");
        }


        Map<String, Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_"+userModel.getId(),map.get("code"));
        redisTemplate.expire("verify_code_"+userModel.getId(),5,TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"),"jpeg",response.getOutputStream());

    }





    //生成秒杀令牌接口
    @RequestMapping(value = "/generatetoken",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam("itemId")Integer itemId,
                                          @RequestParam("promoId")Integer promoId,
                                          @RequestParam("verifyCode")String verifyCode) throws BusinessException {

        //获取用户登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];

        if (StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录，无法下单");
        }

        //通过verifycode验证码验证有效性
        String redisVerifyCode = (String)redisTemplate.opsForValue().get("verify_code_" + userModel.getId());
        if (redisVerifyCode.isEmpty()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法");
        }
        if (!redisVerifyCode.equalsIgnoreCase(verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法，验证码错误");
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

        if (!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EmBusinessError.RATELIMIT);
        }


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
