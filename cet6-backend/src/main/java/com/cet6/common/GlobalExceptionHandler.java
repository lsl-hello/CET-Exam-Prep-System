package com.cet6.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 唯一键冲突：把数据库约束错误转成用户可读的提示。
     * 场景：软删除的用户名会被 count 查询漏掉，但 INSERT 时唯一索引仍会冲突，
     * 这里根据约束名给出具体提示，避免前端只看到通用"系统异常"。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突: {}", e.getMessage());
        String msg = e.getMessage();
        if (msg != null && msg.contains("uk_username")) {
            return Result.fail(400, "用户名已存在（可能是之前注销的账号），请更换用户名");
        }
        return Result.fail(400, "数据重复，请检查输入");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleException(Exception e) {
        // 详情打到后端日志，不暴露给前端，避免 SQL/堆栈信息泄漏到页面
        log.error("系统异常", e);
        return Result.fail(500, "系统异常，请稍后重试");
    }

    @Getter
    public static class BusinessException extends RuntimeException {
        private final int code;

        public BusinessException(String message) {
            this(400, message);
        }

        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }
    }
}
