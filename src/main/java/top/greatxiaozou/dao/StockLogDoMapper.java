package top.greatxiaozou.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.greatxiaozou.dataobject.StockLogDo;
@Repository
@Mapper
public interface StockLogDoMapper {
    int deleteByPrimaryKey(String stockLogId);

    int insert(StockLogDo record);

    int insertSelective(StockLogDo record);

    StockLogDo selectByPrimaryKey(String stockLogId);

    int updateByPrimaryKeySelective(StockLogDo record);

    int updateByPrimaryKey(StockLogDo record);
}