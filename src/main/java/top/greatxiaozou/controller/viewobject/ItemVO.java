package top.greatxiaozou.controller.viewobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.DateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.DataTruncation;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemVO {
    //商品Id
    private Integer id;

    //商品名称
    private String title;

    //商品价格
    private BigDecimal price;

    //秒杀活动价格
    private BigDecimal promoPrice;

    //秒杀活动id
    private Integer promoId;

    //秒杀活动开始时间
    private String startDate;

    //商品库存
    private Integer stock;

    //商品描述
    private String description;

    //商品销量
    private Integer sales;

    //商品描述图片url
    private String imgUrl;

    //记录商品是否在秒杀活动中，以及对应的状态-
    // 0 表示没有秒杀活动，1表示秒杀活动待开始，2表示秒杀活动进行中
    private Integer promoStatus;
}
