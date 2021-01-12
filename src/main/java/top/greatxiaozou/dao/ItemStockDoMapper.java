package top.greatxiaozou.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.greatxiaozou.dataobject.ItemDo;
import top.greatxiaozou.dataobject.ItemStockDo;

import java.util.List;

@Mapper
@Repository
public interface ItemStockDoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ItemStockDo record);

    int insertSelective(ItemStockDo record);

    ItemStockDo selectByPrimaryKey(Integer id);

    ItemStockDo selectByItemId(Integer itemId);

    int decreaseStock(@Param("itemId") Integer itemId,@Param("amount")Integer amount);

    int updateByPrimaryKeySelective(ItemStockDo record);

    int updateByPrimaryKey(ItemStockDo record);
}