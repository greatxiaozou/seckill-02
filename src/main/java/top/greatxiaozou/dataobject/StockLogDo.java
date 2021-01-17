package top.greatxiaozou.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockLogDo {
    private String stockLogId;

    private Integer itemId;

    private Integer amount;

    private Integer status;


}