package vn.utc.tracuu.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thống kê phục vụ tính điểm BM25 trong tầng ứng dụng.
 *
 * <p>Hai bảng thống kê tồn tại vì công thức BM25 cần hai thứ mà chỉ mục toàn văn của cơ sở dữ
 * liệu không cung cấp: <strong>số đoạn chứa mỗi từ</strong> và <strong>độ dài đoạn trung
 * bình</strong>. Không có chúng thì phải dùng hàm xếp hạng sẵn có, mà hàm ấy không phải BM25 và
 * không hiệu chỉnh được hai tham số mà thí nghiệm cần đổi.
 *
 * <p><strong>Thống kê gắn với TỪNG tập đoạn.</strong> Cùng một kho tách theo hai chiến lược cho
 * hai bộ thống kê khác nhau, và dùng nhầm bộ là làm sai chính thí nghiệm — sai theo kiểu vẫn ra
 * số đẹp.
 *
 * <p>Cập nhật <strong>theo đợt</strong> sau khi nạp xong một tài liệu, không cập nhật theo từng
 * đoạn: cập nhật lẻ từng đoạn biến một lần nạp thành hàng chục nghìn lượt ghi tranh khoá nhau.
 */
@Service
public class Bm25StatsService {

    private static final Logger log = LoggerFactory.getLogger(Bm25StatsService.class);

    private final JdbcTemplate jdbc;

    public Bm25StatsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Tính lại toàn bộ thống kê của một tập đoạn.
     *
     * <p>Tính lại toàn bộ thay vì cộng dồn là có chủ đích: cộng dồn sai một lần thì sai vĩnh
     * viễn và không ai phát hiện, vì kết quả xếp hạng vẫn trông hợp lý. Phép tính này chạy trên
     * chỉ mục toàn văn đã có nên vẫn nhanh, và nó chạy sau mỗi tài liệu chứ không sau mỗi đoạn.
     */
    @Transactional
    public void recompute(long chunkSetId) {
        long start = System.currentTimeMillis();

        jdbc.update("DELETE FROM bm25_term_df WHERE chunk_set_id = ?", chunkSetId);
        jdbc.update("""
                INSERT INTO bm25_term_df (chunk_set_id, term, df)
                SELECT ?, word, ndoc
                FROM ts_stat(
                    format('SELECT tsv FROM chunks WHERE chunk_set_id = %s', ?::text)
                )
                WHERE length(word) <= 200
                """, chunkSetId, chunkSetId);

        jdbc.update("""
                INSERT INTO bm25_corpus (chunk_set_id, chunk_count, avg_length)
                SELECT ?, count(*), coalesce(avg(token_count), 0)
                FROM chunks WHERE chunk_set_id = ?
                ON CONFLICT (chunk_set_id) DO UPDATE
                SET chunk_count = excluded.chunk_count, avg_length = excluded.avg_length
                """, chunkSetId, chunkSetId);

        Integer terms = jdbc.queryForObject(
                "SELECT count(*) FROM bm25_term_df WHERE chunk_set_id = ?", Integer.class, chunkSetId);
        log.info("Thống kê BM25 của tập đoạn {}: {} từ, {} ms",
                chunkSetId, terms, System.currentTimeMillis() - start);
    }

    /** Số đoạn và độ dài trung bình của tập đoạn — hai tham số của công thức. */
    public Corpus corpus(long chunkSetId) {
        return jdbc.query("""
                SELECT chunk_count, avg_length FROM bm25_corpus WHERE chunk_set_id = ?
                """, rs -> rs.next()
                        ? new Corpus(rs.getInt(1), rs.getDouble(2))
                        : new Corpus(0, 0), chunkSetId);
    }

    public record Corpus(int chunkCount, double averageLength) {
        public boolean isEmpty() { return chunkCount == 0; }
    }
}
