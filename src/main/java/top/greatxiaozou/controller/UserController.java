package top.greatxiaozou.controller;

import com.alibaba.druid.util.StringUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.security.provider.MD5;
import top.greatxiaozou.controller.viewobject.UserVO;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.response.CommonReturnType;
import top.greatxiaozou.service.UserService;
import top.greatxiaozou.service.model.UserModel;
import top.greatxiaozou.utils.EncodeUtils;


import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = {"*"})
public class UserController extends BaseController{
    @Autowired
    UserService userService;
    @Autowired
    protected HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    //用户登录接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name="telphone")String telphone,
                                  @RequestParam(name = "password")String password) throws BusinessException, NoSuchAlgorithmException {

        //入参校验
        if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户名或密码为空");
        }

        //用户登录服务，用来校验用户登录是否合法
        UserModel userModel = userService.validateLogin(telphone, EncodeUtils.EncodeByMd5(password));

        //将登录凭证加入到用户登录成功的session内
//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);

        //修改为若用户登录验证成功后将对应的登录信息和登录凭证一同存入redis中


        //生产登录token--使用uuid
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-","");

        //建立token和用户登录态之间的联系
        redisTemplate.opsForValue().set(uuidToken,userModel);
        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);


        //下发token
        return CommonReturnType.create(uuidToken);
    }


    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone")String telphone,
                                     @RequestParam(name = "otpCode")String otpCode,
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Byte gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //验证手机号和对应的otpcode是否相符合
        String inSessionOtpCode =(String) this.httpServletRequest.getSession().getAttribute(telphone);
        //System.out.println(inSessionOtpCode);
        //使用工具的目的是封装了判空
        if (!StringUtils.equals(otpCode,inSessionOtpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
        }

        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(EncodeUtils.EncodeByMd5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);
    }



    //用户获取otp短信接口
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone")String telphone){
        //需要按照一定的规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);//[0,99999)

        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);

        //将OTP验证码同对应用户的手机号关联,使用httpsession的方式来绑定
        httpServletRequest.getSession().setAttribute(telphone,otpCode);
        //System.out.println(httpServletRequest.getSession().getAttribute(telphone));
        //将OTP验证码通过短信通道发送给用户（略
        System.out.println("telphone = "+telphone+"\n otpCode = "+otpCode);

        return CommonReturnType.create(null);
    }



    //获取用户测试的接口
    @ResponseBody
    @RequestMapping("/get")
    public CommonReturnType getUser(@RequestParam(name="id") Integer id) throws BusinessException {

        //调用service服务获取对应Id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //若获取的对应用户信息不存在
        if (userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域模型用户对象转换为可供UI使用的viewObject
        UserVO userVO = convertFromModel(userModel);

        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    //=======================================================//

    //将user模型转换未User视图
    private UserVO convertFromModel(UserModel userModel){
        if (userModel == null) return null;
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }
}
