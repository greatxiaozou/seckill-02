package top.greatxiaozou.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.greatxiaozou.dao.OrderDoMapper;
import top.greatxiaozou.dao.SequenceDoMapper;
import top.greatxiaozou.dao.StockLogDoMapper;
import top.greatxiaozou.dataobject.OrderDo;
import top.greatxiaozou.dataobject.SequenceDo;
import top.greatxiaozou.dataobject.StockLogDo;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.mq.MqProducer;
import top.greatxiaozou.service.ItemService;
import top.greatxiaozou.service.OrderService;
import top.greatxiaozou.service.UserService;
import top.greatxiaozou.service.model.ItemModel;
import top.greatxiaozou.service.model.OrderModel;
import top.greatxiaozou.service.model.UserModel;
import top.greatxiaozou.utils.OrderUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDoMapper orderDoMapper;

    @Autowired
    private SequenceDoMapper sequenceDoMapper;

    @Autowired
    private StockLogDoMapper stockLogDoMapper;



    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount,String stockLogId) throws BusinessException{
        //1. 校验下单状态，下单商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);

        //验证数量
        if (amount<=0 || amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }

//        //校验活动信息
//        if (promoId !=null){
//            //校验对应活动是否存在这个适用商品
//            if (promoId.intValue()!=itemModel.getPromoModel().getId()){
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//            }else if (itemModel.getPromoModel().getStatus().intValue()==2){ //校验活动是否进行
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
//            }
//        }

        //2. 落单减库存
        boolean b = itemService.decreaseStock(itemId, amount);
        if (!b){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }


        //3. 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setUserId(userId);
        orderModel.setPromoId(promoId);
        if (promoId!=null){
            orderModel.setPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else {
            orderModel.setPrice(itemModel.getPrice());
        }
        //通过计算得出订单的总金额
        orderModel.setOrderPrice(orderModel.getPrice().multiply(new BigDecimal(amount)));

        //生成交易流水号、订单号
        orderModel.setId(generateOrderNo());
        OrderDo orderDo = convertOrderDoFromOrderModel(orderModel);

        //入库
        orderDoMapper.insertSelective(orderDo);

        //销量增加
        itemService.increaseSales(itemId,amount);

        //设置库存流水状态为成功
        StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDo == null){
            //一般不会到这一步
            throw new BusinessException(EmBusinessError.UNKONW_ERROR,"库存流水获取失败");
        }
        //将流水状态设置为成功
        stockLogDo.setStatus(2);
        stockLogDoMapper.updateByPrimaryKeySelective(stockLogDo);


        //springboot提供的一个方法，在最近的一个事务(Transaction注解方法）被commit之后才会执行
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                //异步更新库存
//                boolean myResult = itemService.asyncDecreaseStock(itemId, amount);
//                if (!myResult){
//                    //如果异步消息发送失败，则回滚
//                    itemService.increaseSales(itemId,amount);
//                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
//                }
//            }
//        });


        //返回前端
        return orderModel;
    }

    //=================convert方法=====================//
    private OrderDo convertOrderDoFromOrderModel(OrderModel orderModel){
        if (orderModel==null) return null;
        OrderDo orderDo = new OrderDo();

        BeanUtils.copyProperties(orderModel,orderDo);
        orderDo.setItemPrice(orderModel.getPrice().doubleValue());
        orderDo.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDo;
    }

    //======================生成订单号方法===================//
    //生成订单号
    //将事务隔离
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public  String generateOrderNo(){
        //订单号有16位
        StringBuilder stringBuilder = new StringBuilder();
        //前8位为年月日时间信息
        LocalDateTime now = LocalDateTime.now();
        String timeInfo = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(timeInfo);
        // 中间六位为自增序列

        SequenceDo sequenceDo = sequenceDoMapper.getSequenceByName("order_info");

        Integer sequence = sequenceDo.getCurrentValue();
        sequenceDo.setCurrentValue(sequence+sequenceDo.getStep());
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDo);

        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6-sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);
        //最后两位为分库分表位
        stringBuilder.append("00");

        return stringBuilder.toString();
    }


}
