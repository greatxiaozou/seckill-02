package top.greatxiaozou.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

//用户下单的交易模型
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderModel implements Serializable {

    //订单id
    private String id;

    //下单的用户id
    private Integer userId;

    //下单的物品id
    private Integer itemId;

    //若非空，则表示是以秒杀商品方式下单
    private Integer promoId;

    //购买商品单价,若promoId非空，则表示m秒杀商品价格
    private BigDecimal price;

    //购买的数量
    private Integer amount;

    //购买的金额，若promoId非空，则表示m秒杀商品价格
    private BigDecimal orderPrice;
}
