package top.greatxiaozou.service;

import org.springframework.stereotype.Service;
import top.greatxiaozou.dataobject.UserDo;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.service.model.UserModel;

@Service
public interface UserService {
    UserModel getUserById(Integer id);

    void register(UserModel userModel) throws BusinessException;

    /**
     *
     * @param telphone 用户注册手机
     * @param encrptPassword 用户加密后的密码
     * @throws BusinessException
     */
    UserModel validateLogin(String telphone,String encrptPassword) throws BusinessException;
}
