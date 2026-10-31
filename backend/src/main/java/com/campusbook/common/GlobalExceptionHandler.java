package com.campusbook.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        return R.fail(400, msg);
    }

    /** 图片太大时 Spring 抛的是这个，翻译成用户看得懂的话 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传超限: {}", e.getMessage());
        return R.fail(413, "图片太大了，换一张小一点的（建议 5MB 以内）");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleOther(Exception e) {
        // 详细堆栈只进日志；返回给前端的内容不能带 SQL 和类名，既不安全也不好看
        log.error("系统异常", e);
        String msg = "服务开小差了，请稍后重试";
        if (e instanceof org.springframework.dao.DataIntegrityViolationException) {
            msg = "提交的数据不符合要求，请检查后重试";
        } else if (e instanceof org.springframework.web.bind.MissingServletRequestParameterException
                || e instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
            msg = "请求参数不正确";
        }
        return R.fail(500, msg);
    }
}
