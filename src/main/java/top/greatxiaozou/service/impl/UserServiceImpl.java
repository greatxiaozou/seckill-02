package top.greatxiaozou.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.greatxiaozou.dao.UserDoMapper;
import top.greatxiaozou.dao.UserPasswprdDoMapper;
import top.greatxiaozou.dataobject.UserDo;
import top.greatxiaozou.dataobject.UserPasswprdDo;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.service.UserService;
import top.greatxiaozou.service.model.UserModel;
import top.greatxiaozou.validator.ValidationResult;
import top.greatxiaozou.validator.ValidatorImpl;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDoMapper userDoMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private UserPasswprdDoMapper userPasswprdDoMapper;

    //根据id获取信息方法
    @Override
    public UserModel getUserById(Integer id) {
        UserDo userDo = userDoMapper.selectByPrimaryKey(id);

        if (userDo == null){
            return null;
        }

        //通过用户Id获取对应的用户加密密码信息
        UserPasswprdDo userPasswprdDo = userPasswprdDoMapper.selectByUserId(id);
        return convertFromDataObject(userDo,userPasswprdDo);
    }

    //登录方法
    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {
        //通过用户手机获取用户信息
        UserDo userDo = userDoMapper.selectByTelphone(telphone);
        if (userDo == null){
            throw new BusinessException(EmBusinessError.USER_LOFIN_FAIL);
        }

        UserPasswprdDo passwprdDo = userPasswprdDoMapper.selectByUserId(userDo.getId());

        UserModel userModel = convertFromDataObject(userDo,passwprdDo);

        //比对用户密码
        if (!StringUtils.equals(encrptPassword,userModel.getEncrptPassword())) {
            throw new BusinessException(EmBusinessError.USER_LOFIN_FAIL);
        }
        return userModel;
    }

    //注册方法
    @Override
    @Transactional  //表示事务操作
    public void register(UserModel userModel) throws BusinessException {
        if (userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }


//        if(StringUtils.isEmpty(userModel.getName())
//            || userModel.getGender() == null
//            || userModel.getAge() == null
//            || StringUtils.isEmpty(userModel.getTelphone())){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
//        }

        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }


        UserDo userDo = convertFromModel(userModel);
        //selective为不更新为空字段,防止空值覆盖
        try {
            userDoMapper.insertSelective(userDo);
        }catch (DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已注册");
        }
        userModel.setId(userDo.getId());

        UserPasswprdDo userPasswprdDo = convertPasswordFromModel(userModel);
        if (userPasswprdDo == null || userPasswprdDo.getEncrptPassword().equals("")){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"密码不能为空");
        }
        userPasswprdDoMapper.insertSelective(userPasswprdDo);

        return;

    }


    //将密码和用户信息组装成usermodel对象
    private UserModel convertFromDataObject(UserDo userDo, UserPasswprdDo passwprdDo){
        if (userDo == null){
            return null;
        }
        UserModel userModel = new UserModel();
        //使用bean工具的copy 方法将属性复制到model对象
        BeanUtils.copyProperties(userDo,userModel);
        if (passwprdDo != null){
            userModel.setEncrptPassword(passwprdDo.getEncrptPassword());
        }
        return userModel;
    }

    //将model对象转换为Do对象
    private UserDo convertFromModel(UserModel userModel){
        if (userModel == null){
            return null;
        }
        UserDo userDo = new UserDo();
        BeanUtils.copyProperties(userModel,userDo);
        return userDo;

    }

    //将密码从model中提取成password的DO
    private UserPasswprdDo convertPasswordFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserPasswprdDo passwprdDo = new UserPasswprdDo();
        passwprdDo.setEncrptPassword(userModel.getEncrptPassword());
        passwprdDo.setUserId(userModel.getId());
        return passwprdDo;
    }
}
