package vn.utc.tracuu.ingestion;

import java.util.List;

/**
 * Sinh biểu diễn ngữ nghĩa cho văn bản.
 *
 * <p>Có hai bản cài và <strong>chúng không tương đương nhau</strong>:
 * <ul>
 *   <li>{@code OnnxEmbeddingModel} — đường CHÍNH, chạy trên CPU ngay trong máy ảo Java. GPU
 *       dành trọn cho mô hình sinh chữ (kiến trúc Mục 1), và đây là lý do việc nạp tài liệu
 *       tính bằng phút chứ không bằng giây.</li>
 *   <li>Nhánh dự phòng trong {@link EmbeddingService#embed(String)} — dùng khi chưa có tệp
 *       mô hình, gọi máy chủ mô hình từng đoạn một. Nó chạy trên GPU, nên việc nạp tài liệu
 *       sẽ tranh tài nguyên với việc trả lời câu hỏi. Đây là một nhánh trong
 *       {@code EmbeddingService}, KHÔNG phải một lớp riêng — chú giải cũ đặt tên một lớp
 *       {@code OllamaEmbeddingModel} chưa từng tồn tại, và một cái tên như thế khiến người đọc
 *       đi tìm nhầm chỗ khi cần sửa đường dự phòng.</li>
 * </ul>
 *
 * <p>Hai đường cho ra <strong>không gian vector khác nhau</strong>. Đổi đường giữa chừng mà
 * không nạp lại kho thì vector cũ và vector mới không so được với nhau, và độ phủ tụt mà không
 * có thông báo nào — {@link #signature()} tồn tại để phát hiện đúng chuyện đó.
 */
public interface EmbeddingModel {

    /** Số chiều của vector. Phải khớp khai báo cột trong lược đồ. */
    int dimension();

    /**
     * Chữ ký của mô hình đang dùng, ghi kèm tập đoạn và kết quả đo.
     *
     * <p>Hai lượt chạy khác chữ ký thì không so được với nhau mà trông vẫn như so được —
     * đúng loại lỗi mà {@code NFR-system-046} đòi chặn.
     */
    String signature();

    float[] embed(String text);

    /** Sinh theo lô. Mặc định gọi lần lượt; bản ONNX ghi đè để tận dụng lô thật. */
    default List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    /** Sẵn sàng dùng chưa. Chưa sẵn sàng thì khâu nạp phải dừng chứ không ghi vector rỗng. */
    boolean ready();
}
