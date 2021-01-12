package top.greatxiaozou.dao;

import org.springframework.stereotype.Repository;
import top.greatxiaozou.dataobject.PromoDo;

@Repository
public interface PromoDoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PromoDo record);

    int insertSelective(PromoDo record);

    PromoDo selectByPrimaryKey(Integer id);

    PromoDo selectByItemId(Integer itemId);

    int updateByPrimaryKeySelective(PromoDo record);

    int updateByPrimaryKey(PromoDo record);
}