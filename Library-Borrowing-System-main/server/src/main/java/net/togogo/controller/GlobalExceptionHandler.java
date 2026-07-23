package net.togogo.controller;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import net.togogo.common.Result;
import net.togogo.common.BusinessException;
import net.togogo.common.ResultCode;


@RestControllerAdvice
public class GlobalExceptionHandler {
    //1. 处理 BusinessException 异常
   @ExceptionHandler(BusinessException.class)
   public Result<Void> handleBusinessException(BusinessException e) {
       return Result.error(e.getResultCode());
   }
   //2,参数校验异常
   @ExceptionHandler(MethodArgumentNotValidException.class)
   public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
       return Result.error(ResultCode.BAD_REQUEST);
   }
   //3,权限异常
   @ExceptionHandler(AccessDeniedException.class)
   public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
       return Result.error(ResultCode.FORBIDDEN);
   }
   //4,其他异常
   @ExceptionHandler(Exception.class)
   public Result<Void> handleException(Exception e) {
       e.printStackTrace();
       return Result.error(ResultCode.INTERNAL_ERROR);
   }
}

