package com.example.bank.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;
    private final Meta meta;

    private ApiResponse(boolean success, T data, ErrorDetail error, Meta meta) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    // --- Factory methods ---

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, data, null, new Meta(requestId, Instant.now().toString(), null));
    }

    public static <T> ApiResponse<T> successPaginated(T data, String requestId, PaginationMeta pagination) {
        return new ApiResponse<>(true, data, null, new Meta(requestId, Instant.now().toString(), pagination));
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message, null), new Meta(requestId, Instant.now().toString(), null));
    }

    public static <T> ApiResponse<T> validationError(String requestId, List<FieldError> details) {
        ErrorDetail err = new ErrorDetail("VALIDATION_FAILED", "One or more fields failed validation.", details);
        return new ApiResponse<>(false, null, err, new Meta(requestId, Instant.now().toString(), null));
    }

    // --- Nested types ---

    @Getter
    public static class Meta {
        private final String requestId;
        private final String timestamp;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final PaginationMeta pagination;

        public Meta(String requestId, String timestamp, PaginationMeta pagination) {
            this.requestId = requestId != null ? requestId : UUID.randomUUID().toString();
            this.timestamp = timestamp;
            this.pagination = pagination;
        }
    }

    @Getter
    public static class ErrorDetail {
        private final String code;
        private final String message;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final List<FieldError> details;

        public ErrorDetail(String code, String message, List<FieldError> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String rule;
        private final String message;

        public FieldError(String field, String rule, String message) {
            this.field = field;
            this.rule = rule;
            this.message = message;
        }
    }
}
