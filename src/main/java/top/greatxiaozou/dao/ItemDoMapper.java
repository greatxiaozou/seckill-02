package top.greatxiaozou.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.greatxiaozou.dataobject.ItemDo;

import java.util.List;

@Repository
@Mapper
public interface ItemDoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ItemDo record);

    int insertSelective(ItemDo record);

    ItemDo selectByPrimaryKey(Integer id);

    int increaseSales(@Param("id")Integer id, @Param("amount")Integer amount);

    List<ItemDo> listItem();

    int updateByPrimaryKeySelective(ItemDo record);

    int updateByPrimaryKey(ItemDo record);
}