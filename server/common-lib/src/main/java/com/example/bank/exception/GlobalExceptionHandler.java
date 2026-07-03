package com.example.bank.exception;

import com.example.bank.common.ApiResponse;
import com.example.bank.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String requestId(HttpServletRequest req) {
        String id = req.getHeader("X-Request-Id");
        return id != null ? id : "unknown";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResponse.FieldError(
                        fe.getField(),
                        fe.getCode(),
                        fe.getDefaultMessage()))
                .collect(Collectors.toList());
        
        log.warn("requestId={} path={} VALIDATION_FAILED", requestId(req), req.getRequestURI());
        
        String message = ErrorMessageConstants.getMessage(ErrorCode.VALIDATION_FAILED);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.validationError(requestId(req), details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleMalformed(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("requestId={} path={} MALFORMED_REQUEST", requestId(req), req.getRequestURI());
        
        String message = ErrorMessageConstants.getMessage(ErrorCode.MALFORMED_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.MALFORMED_REQUEST.name(), message, requestId(req)));
    }

    @ExceptionHandler(BaseAppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(BaseAppException ex, HttpServletRequest req) {
        HttpStatus status = ex.getErrorCode().getHttpStatus();
        log.warn("requestId={} path={} {} {}", requestId(req), req.getRequestURI(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getErrorCode().name(), ex.getMessage(), requestId(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex, HttpServletRequest req) {
        String className = ex.getClass().getName();
        
        if (className.contains("AccessDeniedException")) {
            log.warn("requestId={} path={} ACCESS_DENIED", requestId(req), req.getRequestURI());
            String message = ErrorMessageConstants.getMessage(ErrorCode.ACCESS_DENIED);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ErrorCode.ACCESS_DENIED.name(), message, requestId(req)));
        }

        if (className.contains("FeignException")) {
            log.warn("requestId={} path={} FEIGN_ERROR {}", requestId(req), req.getRequestURI(), ex.getMessage());
            String feignMsg = ErrorMessageConstants.getMessage(ErrorCode.DOWNSTREAM_SERVICE_ERROR);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponse.error(ErrorCode.DOWNSTREAM_SERVICE_ERROR.name(), feignMsg, requestId(req)));
        }

        log.error("requestId={} path={} INTERNAL_ERROR", requestId(req), req.getRequestURI(), ex);
        String message = ErrorMessageConstants.getMessage(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), message, requestId(req)));
    }
}
