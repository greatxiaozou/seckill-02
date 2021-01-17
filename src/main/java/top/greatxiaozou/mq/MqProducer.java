package top.greatxiaozou.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.greatxiaozou.dao.StockLogDoMapper;
import top.greatxiaozou.dataobject.StockLogDo;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.service.OrderService;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息中间件的生产者端，发送消息。
 */
@Component
public class MqProducer {
    @Autowired
    private OrderService orderService;

    //默认的producer
    private DefaultMQProducer producer;

    //事务型的producer
    private TransactionMQProducer transactionMQProducer;

    @Autowired
    private StockLogDoMapper stockLogDoMapper;


    //将配置文件中的值注入进变量
    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @PostConstruct
    public void init() throws MQClientException {
        //做mq producer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        //事务型producer的初始化
        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object args) {
                //获取参数
                Map argsMap = (Map) args;
                Integer itemId = (Integer) argsMap.get("itemId");
                Integer userId = (Integer) argsMap.get("userId");
                Integer promoId = (Integer) argsMap.get("promoId");
                Integer amount = (Integer) argsMap.get("amount");
                String stockLogId = (String) argsMap.get("stockLogId");

                //真正要做的事--创建订单
                try {
                    orderService.createOrder(userId,itemId,promoId,amount,stockLogId);
                } catch (BusinessException e) {
                    //抛出异常，说明任务需要回滚
                    e.printStackTrace();
                    //设置对应的stockLog为回滚状态
                    StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
                    stockLogDo.setStatus(3);
                    stockLogDoMapper.updateByPrimaryKeySelective(stockLogDo);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                //成功则提交
                return LocalTransactionState.COMMIT_MESSAGE;

            }

            //该方法会被定期回调来作为检测手段
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                //根据是否扣减库存成功来判断要返回commit还是comeback还是unknow
                String jsonString = new String(messageExt.getBody());
                Map map = JSON.parseObject(jsonString,Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId =(String) map.get("stockLogId");

                StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
                if (stockLogDo == null){
                    return LocalTransactionState.UNKNOW;
                }
                if (stockLogDo.getStatus().intValue() == 2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                if (stockLogDo.getStatus().intValue() == 1){
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;

            }
        });

    }

    //事务型同步库存扣减消息
    public boolean transactionAsyncReduceStock(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId){
        Map<String,Object> bodyMap = new HashMap<>();
        //bodyMap参数放入,给消费者端使用的
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        bodyMap.put("stockLogId",stockLogId);


        Map<String,Object> argsMap = new HashMap<>();
        //argsMap参数放入，给准备状态消息的回调函数使用的
        argsMap.put("itemId",itemId);
        argsMap.put("amount",amount);
        argsMap.put("userId",userId);
        argsMap.put("promoId",promoId);
        argsMap.put("stockLogId",stockLogId);

        Message message = new Message(topicName,"increase",
                JSON.toJSON(bodyMap).toString().getBytes(StandardCharsets.UTF_8));

        TransactionSendResult sendResult=null;
        //发送消息
        try {
            //会投递一个parpared消息，投递成功但不能被消费
            //prepared方法投递成功后会执行listener的excute....方法
            sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);

        } catch (MQClientException e){
            e.printStackTrace();
            return false;
        }
        if (LocalTransactionState.COMMIT_MESSAGE == sendResult.getLocalTransactionState()){
            return true;
        }else{
           return false;
        }
    }


    //同步库存扣减消息
    public boolean asyncReduceStock(Integer itemId,Integer amount) {
        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        Message message = new Message(topicName,"increase",
                JSON.toJSON(bodyMap).toString().getBytes(StandardCharsets.UTF_8));

        //发送消息
        try {
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
