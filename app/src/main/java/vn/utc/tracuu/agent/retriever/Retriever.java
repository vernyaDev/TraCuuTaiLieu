package vn.utc.tracuu.agent.retriever;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.PermissionResolver;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.access.ReadableDocuments;
import vn.utc.tracuu.ingestion.Bm25StatsService;
import vn.utc.tracuu.ingestion.ChunkWriter;
import vn.utc.tracuu.ingestion.EmbeddingService;
import vn.utc.tracuu.ingestion.VietnameseSegmenter;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.*;

/**
 * Truy hồi lai: nhánh từ khoá và nhánh ngữ nghĩa, hợp nhất bằng thứ hạng.
 *
 * <p>Đây là truy vấn quan trọng nhất của hệ thống, và trình tự của nó không được đổi:
 *
 * <ol>
 *   <li><strong>Tính tập tài liệu được phép TRƯỚC.</strong> Lọc quyền là <em>tiền lọc</em>, không
 *       phải hậu lọc. Lọc sau khi xếp hạng làm số kết quả trả về ít hơn mong đợi một cách khó
 *       đoán, và làm mọi số đo truy hồi mất ý nghĩa ({@code AC-003} của US-008).</li>
 *   <li><strong>Đổi chiều lọc theo độ chọn lọc.</strong> Phạm vi hẹp thì liệt kê tài liệu được
 *       phép; phạm vi rộng thì liệt kê tài liệu bị chặn. Truyền chín trăm mã vào truy vấn chậm
 *       hơn cả việc loại trừ vài chục mã còn lại.</li>
 *   <li><strong>Bật quét chỉ mục lặp khi phạm vi hẹp.</strong> Chỉ mục xấp xỉ tìm láng giềng
 *       <em>trước</em> rồi mới áp bộ lọc; phần lớn láng giềng nằm ngoài quyền thì kết quả ít hơn
 *       số cần và <strong>độ phủ tụt không có dấu hiệu gì</strong>. Đây là rủi ro kỹ thuật lớn
 *       nhất của dự án, và là lý do phép kiểm khởi động số 2 tồn tại.</li>
 * </ol>
 */
@Component
public class Retriever {

    private static final Logger log = LoggerFactory.getLogger(Retriever.class);

    /** Dưới tỉ lệ này coi là phạm vi hẹp — chỗ chỉ mục xấp xỉ bắt đầu bỏ sót. */

    private final JdbcTemplate jdbc;
    private final PermissionResolver permissions;
    private final EmbeddingService embeddings;
    private final VietnameseSegmenter segmenter;
    private final Bm25StatsService bm25Stats;
    private final TracuuProperties props;

    public Retriever(JdbcTemplate jdbc, PermissionResolver permissions,
                     EmbeddingService embeddings, VietnameseSegmenter segmenter,
                     Bm25StatsService bm25Stats, TracuuProperties props) {
        this.jdbc = jdbc;
        this.permissions = permissions;
        this.embeddings = embeddings;
        this.segmenter = segmenter;
        this.bm25Stats = bm25Stats;
        this.props = props;
    }

    /** Kết quả truy hồi kèm thông tin đủ để giải thích vì sao ra như vậy. */
    public record Outcome(List<RetrievedChunk> chunks, int permittedDocumentCount,
                          boolean iterativeScanUsed, long elapsedMs) {
        public boolean isEmpty() { return chunks.isEmpty(); }
    }

    /**
     * Truy hồi cho một truy vấn.
     *
     * <p>Tham số {@link PermissionScope} là <strong>bắt buộc</strong>, không có quá tải nào bỏ
     * nó đi. Luật R1: không hàm nào đọc bảng đoạn mà không mang phạm vi quyền tường minh.
     */
    @Transactional(readOnly = true)
    public Outcome retrieve(String query, PermissionScope scope, long chunkSetId, int topK) {
        long start = System.currentTimeMillis();

        ReadableDocuments readable = permissions.resolve(scope);
        if (readable.isEmpty()) {
            // Không đọc được tài liệu nào. Trả rỗng NGAY — chạy tiếp truy vấn chỉ tốn thời gian
            // để đi tới cùng một kết quả.
            return new Outcome(List.of(), 0, false, System.currentTimeMillis() - start);
        }

        int candidateK = Math.max(topK, props.retrieval().candidateK());
        boolean narrow = isNarrow(readable);

        List<RetrievedChunk> keyword = props.retrieval().usesKeyword()
                ? bm25Branch(query, readable, chunkSetId, candidateK) : List.of();
        List<RetrievedChunk> vector = props.retrieval().usesVector()
                ? vectorBranch(query, readable, chunkSetId, candidateK, narrow) : List.of();

        List<RetrievedChunk> fused = fuse(keyword, vector, topK);
        long elapsed = System.currentTimeMillis() - start;

        log.debug("Truy hồi: {} đoạn từ khoá, {} đoạn ngữ nghĩa, {} đoạn sau hợp nhất, {} ms",
                keyword.size(), vector.size(), fused.size(), elapsed);
        return new Outcome(fused, readable.estimatedSize(), narrow && props.retrieval().iterativeScan(), elapsed);
    }

    private boolean isNarrow(ReadableDocuments readable) {
        if (readable instanceof ReadableDocuments.All) return false;
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM documents WHERE status = 'DA_LAP_CHI_MUC'", Long.class);
        if (total == null || total == 0) return false;
        return (double) readable.estimatedSize() / total < props.retrieval().narrowScopeRatio();
    }

    // ── Nhánh từ khoá: BM25 tính trong tầng ứng dụng ───────────────────────────────────────

    /**
     * BM25 tính ở đây chứ không dùng hàm xếp hạng sẵn có của cơ sở dữ liệu.
     *
     * <p>Lý do: hai tham số {@code k1} và {@code b} phải đổi được bằng cấu hình để chạy thí
     * nghiệm, mà hàm sẵn có không cho đổi. Tần suất từ lấy trực tiếp từ chỉ mục toàn văn bằng
     * cách trải vị trí của từng từ — nên đây là tần suất THẬT, không phải xấp xỉ.
     */
    private List<RetrievedChunk> bm25Branch(String query, ReadableDocuments readable,
                                            long chunkSetId, int limit) {
        List<String> terms = segmenter.tokenize(query).stream()
                .filter(t -> t.length() > 1)
                .distinct()
                .toList();
        if (terms.isEmpty()) return List.of();

        Bm25StatsService.Corpus corpus = bm25Stats.corpus(chunkSetId);
        if (corpus.isEmpty()) return List.of();

        Map<String, Integer> df = documentFrequencies(chunkSetId, terms);
        if (df.isEmpty()) return List.of();

        String sql = """
                SELECT c.id, c.document_id, d.title, c.breadcrumb, c.content, c.ordinal,
                       c.is_table, c.table_part, c.table_parts_total, c.token_count,
                       t.lexeme, coalesce(array_length(t.positions, 1), 1) AS tf
                FROM chunks c
                JOIN documents d ON d.id = c.document_id
                CROSS JOIN LATERAL unnest(c.tsv) t
                WHERE c.chunk_set_id = ?
                  AND d.status = 'DA_LAP_CHI_MUC'
                  AND t.lexeme = ANY(?)
                  AND %s
                """.formatted(readable.sqlPredicate("c"));

        List<Object> args = new ArrayList<>();
        args.add(chunkSetId);
        args.add(terms.toArray(new String[0]));
        if (readable.needsIdParameter()) args.add(toArray(readable.parameterIds()));

        Map<Long, Accumulator> byChunk = new HashMap<>();
        double k1 = props.retrieval().bm25().k1();
        double b = props.retrieval().bm25().b();
        double avgLen = Math.max(1.0, corpus.averageLength());
        int n = Math.max(1, corpus.chunkCount());

        jdbc.query(sql, rs -> {
            long chunkId = rs.getLong("id");
            Accumulator acc = byChunk.computeIfAbsent(chunkId, id -> {
                try {
                    return new Accumulator(id, rs.getLong("document_id"), rs.getString("title"),
                            rs.getString("breadcrumb"), rs.getString("content"), rs.getInt("ordinal"),
                            rs.getBoolean("is_table"), (Integer) rs.getObject("table_part"),
                            (Integer) rs.getObject("table_parts_total"), rs.getInt("token_count"));
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            String lexeme = rs.getString("lexeme");
            int tf = rs.getInt("tf");
            int dfValue = df.getOrDefault(lexeme, 1);

            double idf = Math.log(1 + (n - dfValue + 0.5) / (dfValue + 0.5));
            double norm = tf + k1 * (1 - b + b * acc.length / avgLen);
            acc.score += idf * (tf * (k1 + 1)) / Math.max(1e-9, norm);
        }, args.toArray());

        return byChunk.values().stream()
                .sorted(Comparator.comparingDouble((Accumulator a) -> a.score).reversed())
                .limit(limit)
                .map(a -> a.toChunk(a.score, null))
                .toList();
    }

    private Map<String, Integer> documentFrequencies(long chunkSetId, List<String> terms) {
        Map<String, Integer> out = new HashMap<>();
        jdbc.query("SELECT term, df FROM bm25_term_df WHERE chunk_set_id = ? AND term = ANY(?)",
                rs -> { out.put(rs.getString(1), rs.getInt(2)); },
                chunkSetId, terms.toArray(new String[0]));
        return out;
    }

    // ── Nhánh ngữ nghĩa ────────────────────────────────────────────────────────────────────

    /** Đã kêu về lệch chữ ký chưa. */
    private final java.util.concurrent.atomic.AtomicBoolean daKeuLechChuKy =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Mô hình sinh biểu diễn đang chạy có cùng chữ ký với tập đoạn không.
     *
     * <p>Khác chữ ký nghĩa là vector câu hỏi và vector các đoạn nằm ở <strong>hai không gian
     * khác nhau</strong>. Phép so khoảng cách giữa chúng vẫn chạy và vẫn trả về những con số
     * trông hợp lý — đó mới là chỗ nguy hiểm: không có lỗi nào, chỉ có kết quả sai.
     *
     * <p>Chọn BỎ nhánh ngữ nghĩa thay vì trả kết quả sai. Mất một nhánh là suy giảm thấy được
     * (nhánh từ khoá vẫn chạy); trả số vô nghĩa là hỏng không thấy được.
     */
    private boolean chuKyKhop(long chunkSetId) {
        try {
            var hang = jdbc.queryForMap(
                    "SELECT embedding_signature, embedding_dimension FROM chunk_sets WHERE id = ?",
                    chunkSetId);
            Object cuaTap = hang.get("embedding_signature");
            if (cuaTap == null) return true;     // tập chưa đóng dấu: chưa có gì để mâu thuẫn

            Object soChieu = hang.get("embedding_dimension");
            boolean khop = cuaTap.toString().equals(embeddings.signature())
                    && (soChieu == null || ((Number) soChieu).intValue() == embeddings.dimension());
            if (!khop && daKeuLechChuKy.compareAndSet(false, true)) {
                log.error("Mô hình sinh biểu diễn đang chạy ('{}', {} chiều) KHÁC mô hình đã nạp "
                        + "tập đoạn ('{}', {} chiều). Bỏ nhánh tìm theo ngữ nghĩa để không trả "
                        + "về kết quả vô nghĩa; chỉ còn nhánh từ khoá. Khôi phục bằng cách chạy "
                        + "lại đúng mô hình cũ, hoặc nạp lại kho bằng một tập đoạn mới.",
                        embeddings.signature(), embeddings.dimension(), cuaTap, soChieu);
            }
            return khop;
        } catch (RuntimeException e) {
            log.debug("Không đọc được chữ ký của tập đoạn {}: {}", chunkSetId, e.getMessage());
            return true;   // không đọc được thì không chặn — đây là lớp phát hiện, không phải cổng
        }
    }

    /** Đã cảnh báo chưa — kêu mỗi lượt truy vấn thì biến một thông tin thành tiếng ồn. */
    private final java.util.concurrent.atomic.AtomicBoolean daCanhBaoTran =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Kêu lên khi trần quét lặp nhỏ hơn số đoạn của tập đang hoạt động.
     *
     * <p>Quét lặp tồn tại để quét TIẾP cho tới khi gom đủ kết quả được phép đọc. Một trần thấp
     * hơn kích thước tập làm nó dừng giữa chừng — tức là chính cái nó sinh ra để chống lại vẫn
     * xảy ra, chỉ muộn hơn. Người vận hành sẽ không bao giờ tự phát hiện: kết quả vẫn trả về,
     * chỉ là thiếu, và không có gì phân biệt "thiếu vì kho không có" với "thiếu vì quét dừng".
     */
    private void canhBaoTranQuetNeuThap(long chunkSetId) {
        if (daCanhBaoTran.get()) return;
        try {
            Integer soDoan = jdbc.queryForObject(
                    "SELECT count(*) FROM chunks WHERE chunk_set_id = ?", Integer.class, chunkSetId);
            int tran = props.retrieval().maxScanTuples();
            if (soDoan != null && soDoan > tran && daCanhBaoTran.compareAndSet(false, true)) {
                log.warn("Trần quét chỉ mục lặp ({}) NHỎ HƠN số đoạn của tập đang hoạt động ({}). "
                       + "Truy vấn của người có phạm vi quyền hẹp có thể thiếu kết quả mà không "
                       + "có dấu hiệu. Nâng tracuu.retrieval.max-scan-tuples, hoặc chấp nhận "
                       + "đánh đổi này một cách có ý thức.", tran, soDoan);
            }
        } catch (RuntimeException e) {
            log.debug("Không đếm được số đoạn để kiểm trần quét lặp: {}", e.getMessage());
        }
    }

    private List<RetrievedChunk> vectorBranch(String query, ReadableDocuments readable,
                                              long chunkSetId, int limit, boolean narrow) {
        if (!chuKyKhop(chunkSetId)) return List.of();

        float[] queryVector;
        try {
            queryVector = embeddings.embed(query);
        } catch (RuntimeException e) {
            log.warn("Không sinh được vector cho câu hỏi, bỏ nhánh ngữ nghĩa: {}", e.getMessage());
            return List.of();
        }
        String literal = ChunkWriter.toVectorLiteral(queryVector);

        // Bật quét lặp khi phạm vi hẹp. Quét lặp đổi ĐỘ TRỄ lấy ĐỘ PHỦ — chỉ trả giá đó ở đúng
        // trường hợp cần, không bật mặc định cho mọi truy vấn.
        if (narrow && props.retrieval().iterativeScan()) {
            try {
                jdbc.execute("SET LOCAL hnsw.iterative_scan = relaxed_order");
                jdbc.execute("SET LOCAL hnsw.max_scan_tuples = "
                        + props.retrieval().maxScanTuples());
                canhBaoTranQuetNeuThap(chunkSetId);
            } catch (RuntimeException e) {
                log.warn("Không bật được quét chỉ mục lặp: {}. Phạm vi quyền hẹp có thể làm "
                       + "TỤT ĐỘ PHỦ mà không có dấu hiệu.", e.getMessage());
            }
        }

        String sql = """
                SELECT c.id, c.document_id, d.title, c.breadcrumb, c.content, c.ordinal,
                       c.is_table, c.table_part, c.table_parts_total, c.token_count,
                       1 - (c.embedding <=> ?::vector) AS score
                FROM chunks c
                JOIN documents d ON d.id = c.document_id
                WHERE c.chunk_set_id = ?
                  AND d.status = 'DA_LAP_CHI_MUC'
                  AND c.embedding IS NOT NULL
                  AND %s
                ORDER BY c.embedding <=> ?::vector
                LIMIT ?
                """.formatted(readable.sqlPredicate("c"));

        List<Object> args = new ArrayList<>();
        args.add(literal);
        args.add(chunkSetId);
        if (readable.needsIdParameter()) args.add(toArray(readable.parameterIds()));
        args.add(literal);
        args.add(limit);

        return jdbc.query(sql, (rs, i) -> new RetrievedChunk(
                rs.getLong("id"), rs.getLong("document_id"), rs.getString("title"),
                rs.getString("breadcrumb"), rs.getString("content"), rs.getInt("ordinal"),
                rs.getBoolean("is_table"), (Integer) rs.getObject("table_part"),
                (Integer) rs.getObject("table_parts_total"),
                null, rs.getDouble("score"), 0, i + 1), args.toArray());
    }

    // ── Hợp nhất thứ hạng ──────────────────────────────────────────────────────────────────

    /**
     * Hợp nhất hai nhánh theo thứ hạng thay vì theo điểm.
     *
     * <p>Điểm BM25 không chặn trên còn điểm cosin nằm trong khoảng cố định — cộng thẳng hai loại
     * ấy là để một nhánh lấn át nhánh kia tuỳ theo độ dài câu hỏi. Hợp nhất theo thứ hạng chỉ
     * dùng <em>vị trí</em> nên không cần chuẩn hoá thang điểm, và đó là lý do nó là cách ghép
     * mặc định trong các hệ truy hồi lai.
     */
    private List<RetrievedChunk> fuse(List<RetrievedChunk> keyword, List<RetrievedChunk> vector, int topK) {
        double k = props.retrieval().rrfK();
        Map<Long, RetrievedChunk> merged = new LinkedHashMap<>();
        Map<Long, Double> scores = new HashMap<>();

        for (int i = 0; i < keyword.size(); i++) {
            RetrievedChunk c = keyword.get(i);
            merged.put(c.chunkId(), c);
            scores.merge(c.chunkId(), 1.0 / (k + i + 1), Double::sum);
        }
        for (int i = 0; i < vector.size(); i++) {
            RetrievedChunk c = vector.get(i);
            RetrievedChunk existing = merged.get(c.chunkId());
            if (existing != null) {
                // Cùng một đoạn được cả hai nhánh tìm thấy: giữ cả hai điểm.
                merged.put(c.chunkId(), new RetrievedChunk(
                        existing.chunkId(), existing.documentId(), existing.documentTitle(),
                        existing.breadcrumb(), existing.content(), existing.ordinal(),
                        existing.fromTable(), existing.tablePart(), existing.tablePartsTotal(),
                        existing.scoreBm25(), c.scoreVector(), 0, 0));
            } else {
                merged.put(c.chunkId(), c);
            }
            scores.merge(c.chunkId(), 1.0 / (k + i + 1), Double::sum);
        }

        List<RetrievedChunk> out = new ArrayList<>(merged.values());
        out.sort(Comparator.comparingDouble((RetrievedChunk c) -> scores.get(c.chunkId())).reversed());

        List<RetrievedChunk> result = new ArrayList<>(Math.min(topK, out.size()));
        for (int i = 0; i < out.size() && i < topK; i++) {
            result.add(out.get(i).withRrf(scores.get(out.get(i).chunkId())).withRank(i + 1));
        }
        return result;
    }

    private static Long[] toArray(Set<Long> ids) {
        return ids.toArray(new Long[0]);
    }

    /** Gom điểm BM25 của nhiều từ trên cùng một đoạn. */
    private static final class Accumulator {
        final long chunkId, documentId;
        final String title, breadcrumb, content;
        final int ordinal, length;
        final boolean table;
        final Integer tablePart, tablePartsTotal;
        double score;

        Accumulator(long chunkId, long documentId, String title, String breadcrumb, String content,
                    int ordinal, boolean table, Integer tablePart, Integer tablePartsTotal, int length) {
            this.chunkId = chunkId; this.documentId = documentId; this.title = title;
            this.breadcrumb = breadcrumb; this.content = content; this.ordinal = ordinal;
            this.table = table; this.tablePart = tablePart; this.tablePartsTotal = tablePartsTotal;
            this.length = Math.max(1, length);
        }

        RetrievedChunk toChunk(Double bm25, Double vectorScore) {
            return new RetrievedChunk(chunkId, documentId, title, breadcrumb, content, ordinal,
                    table, tablePart, tablePartsTotal, bm25, vectorScore, 0, 0);
        }
    }
}
