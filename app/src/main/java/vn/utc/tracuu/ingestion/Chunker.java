package vn.utc.tracuu.ingestion;

import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.List;

/**
 * Chiến lược tách đoạn.
 *
 * <p>Mỗi chiến lược là một bản cài riêng vì US-030 so sánh chúng bằng SỐ LIỆU, không bằng cảm
 * tính. Đổi chiến lược phải là đổi cấu hình, không phải sửa mã (NFR-system-045).
 */
public interface Chunker {

    String name();

    List<ChunkCandidate> chunk(ExtractedContent content, TracuuProperties.ChunkStrategy params);
}
