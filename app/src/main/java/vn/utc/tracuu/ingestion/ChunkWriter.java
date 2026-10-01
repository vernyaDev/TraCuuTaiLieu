package vn.utc.tracuu.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Ghi đoạn kèm vector xuống cơ sở dữ liệu.
 *
 * <p>Đi thẳng tầng JDBC chứ không qua tầng ánh xạ quan hệ đối tượng, vì hai lý do cụ thể:
 * kiểu vector là kiểu riêng của phần mở rộng nên ánh xạ được thì cũng phải đăng ký kiểu tuỳ
 * biến; và việc nạp ghi hàng chục nghìn đoạn nên ghi theo lô là thứ quyết định thời gian nạp
 * tính bằng phút hay bằng giờ.
 *
 * <p>Cột chỉ mục toàn văn <strong>không ghi ở đây</strong> — nó là cột sinh tự động từ cột văn
 * bản đã tách từ. Một nguồn duy nhất, không có cách nào để hai cột lệch nhau.
 */
@Component
public class ChunkWriter {

    private static final Logger log = LoggerFactory.getLogger(ChunkWriter.class);

    private static final String INSERT = """
            INSERT INTO chunks (chunk_set_id, document_id, ordinal, breadcrumb, content,
                                content_segmented, embedding, token_count, is_table,
                                table_part, table_parts_total, char_start, char_end)
            VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    public ChunkWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ghi toàn bộ đoạn của một tài liệu trong một giao dịch.
     *
     * <p>Toàn bộ hoặc không gì cả: nửa tài liệu nằm trong chỉ mục là trạng thái tệ hơn cả không
     * có gì, vì câu trả lời sẽ trích dẫn một tài liệu mà nội dung thật của nó chỉ có một nửa.
     */
    @Transactional
    public int write(long chunkSetId, long documentId,
                     List<ChunkCandidate> candidates,
                     List<String> segmented,
                     List<float[]> embeddings,
                     List<Integer> tokenCounts) {

        if (candidates.isEmpty()) return 0;
        if (candidates.size() != segmented.size() || candidates.size() != embeddings.size()) {
            throw new IllegalArgumentException(
                    "Số đoạn, số bản đã tách từ và số vector phải bằng nhau: "
                    + candidates.size() + " / " + segmented.size() + " / " + embeddings.size());
        }

        // Ghi lại là thay thế: xoá trước để nạp lại một tài liệu không sinh đoạn trùng.
        jdbc.update("DELETE FROM chunks WHERE chunk_set_id = ? AND document_id = ?",
                chunkSetId, documentId);

        jdbc.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ChunkCandidate c = candidates.get(i);
                ps.setLong(1, chunkSetId);
                ps.setLong(2, documentId);
                ps.setInt(3, i);
                ps.setString(4, c.breadcrumb() == null ? "" : c.breadcrumb());
                ps.setString(5, c.content());
                ps.setString(6, segmented.get(i));
                ps.setString(7, toVectorLiteral(embeddings.get(i)));
                ps.setInt(8, tokenCounts.get(i));
                ps.setBoolean(9, c.table());
                setNullableInt(ps, 10, c.tablePart());
                setNullableInt(ps, 11, c.tablePartsTotal());
                ps.setInt(12, c.charStart());
                ps.setInt(13, c.charEnd());
            }

            @Override
            public int getBatchSize() {
                return candidates.size();
            }
        });

        jdbc.update("""
                UPDATE chunk_sets SET chunk_count =
                    (SELECT count(*) FROM chunks WHERE chunk_set_id = ?) WHERE id = ?
                """, chunkSetId, chunkSetId);

        log.debug("Đã ghi {} đoạn cho tài liệu {} vào tập đoạn {}",
                candidates.size(), documentId, chunkSetId);
        return candidates.size();
    }

    @Transactional
    public int deleteForDocument(long documentId) {
        return jdbc.update("DELETE FROM chunks WHERE document_id = ?", documentId);
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value)
            throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER); else ps.setInt(index, value);
    }

    /**
     * Vector ở dạng {@code [0.1,0.2,…]} rồi ép kiểu trong câu lệnh.
     *
     * <p>Cách này không cần đăng ký kiểu JDBC tuỳ biến, nên nó không hỏng khi đổi phiên bản
     * trình điều khiển — một loại hỏng khó chẩn đoán vì thông báo lỗi nói về kiểu chứ không nói
     * về phiên bản.
     */
    public static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 9 + 2);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
