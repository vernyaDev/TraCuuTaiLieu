package vn.utc.tracuu.ingestion;

import java.nio.file.Path;

/** Bóc văn bản và cấu trúc từ một định dạng tệp. */
public interface TextExtractor {

    /** Đuôi tệp lớp này xử lý, viết thường, không có dấu chấm. */
    boolean supports(String extension);

    ExtractedContent extract(Path file);
}
