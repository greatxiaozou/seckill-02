package top.greatxiaozou.dao;

import org.springframework.stereotype.Repository;
import top.greatxiaozou.dataobject.UserPasswprdDo;
@Repository
public interface UserPasswprdDoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserPasswprdDo record);

    int insertSelective(UserPasswprdDo record);

    UserPasswprdDo selectByPrimaryKey(Integer id);

    UserPasswprdDo selectByUserId(Integer userId);

    int updateByPrimaryKeySelective(UserPasswprdDo record);

    int updateByPrimaryKey(UserPasswprdDo record);


}