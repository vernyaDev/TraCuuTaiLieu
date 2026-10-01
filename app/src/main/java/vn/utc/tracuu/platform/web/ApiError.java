package vn.utc.tracuu.platform.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Hình dạng DUY NHẤT của mọi lỗi trả ra ngoài (hợp đồng giao diện Mục 1, nguyên tắc hai):
 * một mã máy đọc được, một câu nghiệp vụ cho người đọc, và khi áp dụng thì kèm tên trường lỗi.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, String field, List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ApiError of(ErrorCode code) {
        return new ApiError(code.name(), code.message(), null, null);
    }

    public static ApiError of(ErrorCode code, String field) {
        return new ApiError(code.name(), code.message(), field, null);
    }

    public static ApiError withFields(ErrorCode code, List<FieldError> errors) {
        return new ApiError(code.name(), code.message(), null, errors);
    }
}
