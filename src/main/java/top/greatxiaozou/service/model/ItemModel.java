package top.greatxiaozou.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemModel implements Serializable {
    //商品Id
    private Integer id;

    //商品名称
    @NotBlank(message = "商品名称不能为空")
    private String title;

    //商品价格
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0,message = "商品价格必须大于0")
    private BigDecimal price;

    //商品库存
    @NotNull(message = "库存不能不填")
    private Integer stock;

    //商品描述
    @NotBlank(message = "商品描述不能为空")
    private String description;

    //商品销量
    private Integer sales;

    //商品描述图片url
    @NotBlank(message = "图片描述不能为空")
    private String imgUrl;

    //使用聚合模型，如果promoModel不为空，则表示其还具有未结束的秒杀活动
    private PromoModel promoModel;

}
