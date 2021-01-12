package top.greatxiaozou.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.greatxiaozou.dataobject.SequenceDo;

@Repository
@Mapper
public interface SequenceDoMapper {
    int deleteByPrimaryKey(String name);

    int insert(SequenceDo record);

    int insertSelective(SequenceDo record);

    SequenceDo selectByPrimaryKey(String name);

    SequenceDo getSequenceByName(String name);

    int updateByPrimaryKeySelective(SequenceDo record);

    int updateByPrimaryKey(SequenceDo record);

}