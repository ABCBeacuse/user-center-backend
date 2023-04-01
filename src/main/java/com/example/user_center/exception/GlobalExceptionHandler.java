package com.example.user_center.exception;

import com.example.user_center.common.BaseResponse;
import com.example.user_center.common.ErrorCode;
import com.example.user_center.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BussinessException.class)
    public BaseResponse businessExceptionHandler(BussinessException e){
        log.error("businessException" + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runTimeExceptionHandler(RuntimeException e){
        log.error("runTimeException" , e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(),"");
    }
}
