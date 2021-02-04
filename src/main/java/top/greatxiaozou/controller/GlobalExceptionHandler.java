package top.greatxiaozou.controller;

import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;
import top.greatxiaozou.error.BusinessException;
import top.greatxiaozou.error.EmBusinessError;
import top.greatxiaozou.response.CommonReturnType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonReturnType doError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,Exception ex){
//        ex.printStackTrace();
        Map<String,Object> responseData = new HashMap<>();
        //根据切面捕获的异常类型来进行不同的异常处理
        if(ex instanceof BusinessException){
            //如果异常为自定义的业务异常
            BusinessException businessException = (BusinessException) ex;
            responseData.put("errCode",businessException.getErrCode());
            responseData.put("errMsg",businessException.getErrMsg());
        }else if (ex instanceof ServletRequestBindingException){
            //如果异常为请求绑定异常
            responseData.put("errCode", EmBusinessError.UNKONW_ERROR.getErrCode());
            responseData.put("errMsg","url绑定路由问题");
        }else if (ex instanceof NoHandlerFoundException){
            responseData.put("errCode",EmBusinessError.UNKONW_ERROR.getErrCode());


            responseData.put("errMsg","未找到对应路径，请检查url是否正确");
        }
        return CommonReturnType.create(responseData,"fail");
    }
}
