package vn.utc.tracuu.ingestion;

import vn.utc.tracuu.platform.web.ErrorCode;

/** Bóc văn bản thất bại, kèm mã lỗi nghiệp vụ để màn nạp hiện đúng lý do. */
public class ExtractionException extends RuntimeException {

    private final ErrorCode code;

    public ExtractionException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ExtractionException(ErrorCode code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }

    public ErrorCode code() { return code; }
}
