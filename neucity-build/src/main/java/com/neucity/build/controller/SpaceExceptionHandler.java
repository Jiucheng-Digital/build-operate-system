package com.neucity.build.controller;

import com.neucity.build.exception.SpaceDocumentException;
import com.neucity.common.core.web.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Scope HTTP status handling to the editor without changing existing APIs. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SpaceController.class)
public class SpaceExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SpaceExceptionHandler.class);

    @ExceptionHandler(SpaceDocumentException.class)
    public ResponseEntity<AjaxResult> handleSpace(SpaceDocumentException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(AjaxResult.error(exception.getStatus(), exception.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<AjaxResult> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(AjaxResult.error(400, "请求参数不完整或 JSON 格式错误"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleFailure(Exception exception) {
        LOG.error("空间持久化请求失败", exception);
        return ResponseEntity.status(500).body(AjaxResult.error(500, "空间操作失败，请稍后重试"));
    }
}
