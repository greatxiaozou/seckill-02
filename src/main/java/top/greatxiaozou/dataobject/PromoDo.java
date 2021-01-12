package top.greatxiaozou.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PromoDo {

    private Integer id;

    private String promoName;

    private Date startDate;

    private Date endDate;

    private Double promoItemPrice;

    private Integer itemId;

}