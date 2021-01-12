package top.greatxiaozou.dao;

import top.greatxiaozou.dataobject.UserPasswprdDo;

public interface UserPasswprdDoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserPasswprdDo record);

    int insertSelective(UserPasswprdDo record);

    UserPasswprdDo selectByPrimaryKey(Integer id);

    UserPasswprdDo selectByUserId(Integer userId);

    int updateByPrimaryKeySelective(UserPasswprdDo record);

    int updateByPrimaryKey(UserPasswprdDo record);


}