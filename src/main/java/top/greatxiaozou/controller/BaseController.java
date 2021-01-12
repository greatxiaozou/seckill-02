package top.greatxiaozou.controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.response.CommonReturnType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class BaseController {
    public static final String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

//
////    //定义exceptionhandler解决未被controller层吸收的exception
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.OK)
//    @ResponseBody
//    public CommonReturnType handlerExcpetion(HttpServletRequest request, Exception e){
//        Map<String,Object> map = new HashMap<>();
//        if (e instanceof BusinessException){
//            BusinessException be = (BusinessException)e;
//            //将异常数据放入map
//            map.put("errCode",be.getErrCode());
//            map.put("errMsg",be.getErrMsg());
//        }else {
//            map.put("errCode", EmBusinessError.UNKONW_ERROR.getErrCode());
//            map.put("errCode",EmBusinessError.UNKONW_ERROR.getErrMsg());
//        }
//        //将map放入通用返回对象
//        return CommonReturnType.create(map,"fail");
//    }
}
