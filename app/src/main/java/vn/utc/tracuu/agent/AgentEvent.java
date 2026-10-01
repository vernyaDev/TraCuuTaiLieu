package vn.utc.tracuu.agent;

import java.util.List;

/**
 * Mười loại sự kiện một lượt hỏi đáp có thể phát ra, theo hợp đồng giao diện Mục 3.
 *
 * <p>Gói {@code agent} <strong>không biết gì về giao thức web</strong> (luật R2): những bản ghi
 * này là sự kiện nghiệp vụ, còn việc biến chúng thành luồng sự kiện một chiều là việc của tầng
 * web. Nhờ vậy bộ đo gọi thẳng bộ điều phối và nhận cùng một chuỗi sự kiện mà không phải dựng
 * máy chủ giả — nếu agent dính vào web thì bộ đo sẽ đo một thứ khác với thứ người dùng dùng.
 *
 * <p>Hai ràng buộc của hợp đồng, thể hiện ngay trong kiểu:
 * <ul>
 *   <li>{@link Refusal} và {@link Token} loại trừ nhau trong một lượt.</li>
 *   <li>{@link Citations} luôn đến <em>sau</em> mẩu chữ cuối, vì danh sách nguồn chỉ chốt được
 *       khi câu trả lời đã xong.</li>
 * </ul>
 */
public sealed interface AgentEvent {

    String type();

    /** Vị trí trong hàng đợi, khi quá số câu hỏi xử lý cùng lúc. */
    record Queued(int position, int ahead) implements AgentEvent {
        public String type() { return "queued"; }
    }

    /** Bước đang chạy, bằng lời nghiệp vụ — không phải tên lớp hay tên hàm. */
    record Status(String step) implements AgentEvent {
        public String type() { return "status"; }
    }

    /** Câu hỏi đã viết lại thành câu độc lập. */
    record Rewritten(String question) implements AgentEvent {
        public String type() { return "rewritten"; }
    }

    /** Danh sách truy vấn con theo thứ tự. */
    record Plan(List<PlanItem> steps) implements AgentEvent {
        public String type() { return "plan"; }
    }

    record PlanItem(int ordinal, String text, Integer dependsOn) {}

    /** Một mẩu chữ của câu trả lời. */
    record Token(String text) implements AgentEvent {
        public String type() { return "token"; }
    }

    /** Cảnh báo về tài liệu nguồn: hết hiệu lực, đã bị thay thế, hoặc chồng lấn. */
    record Warning(String warningType, Long documentId, String documentTitle, String detail)
            implements AgentEvent {
        public String type() { return "warning"; }
    }

    /** Danh sách nguồn, mỗi nguồn kèm đường dẫn phân cấp. */
    record Citations(List<Citation> items) implements AgentEvent {
        public String type() { return "citations"; }
    }

    record Citation(int rank, long chunkId, long documentId, String documentTitle,
                    String breadcrumb, String excerpt,
                    Double scoreBm25, Double scoreVector, Double scoreRrf) {}

    /**
     * Từ chối trả lời.
     *
     * <p>{@code stage} bằng {@code TANG_HAI} nghĩa là chữ đã hiện trên màn hình và giao diện
     * <strong>phải xoá phần đã hiện</strong> ({@code AC-002} của US-024). Không nói rõ tầng thì
     * giao diện không biết có phải thu hồi hay không.
     */
    /**
     * Từ chối trả lời.
     *
     * <p><strong>KHÔNG mang lý do máy-đọc-được.</strong> Lý do nội bộ phân biệt "kho không có
     * tài liệu nào" với "tài liệu nằm ngoài phạm vi quyền" — đúng hai ca mà cả hệ thống được
     * dựng để không phân biệt được từ bên ngoài. Bản trước gửi nó qua luồng sự kiện: giao diện
     * không hiển thị, nhưng bất kỳ ai mở bảng mạng của trình duyệt đều đọc được, và khi đó việc
     * dùng chung MỘT câu chữ trở thành vô nghĩa — người hỏi so hai lượt từ chối là suy ra được
     * tài liệu nào tồn tại ngoài quyền mình.
     *
     * <p>Lý do vẫn được giữ trong {@code AgentContext} và ghi vào nhật ký khoảng trống cho quản
     * trị. Nó chỉ không đi ra ngoài.
     */
    record Refusal(String stage, String message) implements AgentEvent {
        public String type() { return "refusal"; }
    }

    /** Kết thúc lượt. Hai con số này là nguồn dữ liệu THẬT cho ngưỡng độ trễ. */
    record Done(long turnId, long firstTokenMs, long totalMs) implements AgentEvent {
        public String type() { return "done"; }
    }

    /** Lỗi kỹ thuật. Thông điệp là câu nghiệp vụ, không phải vết gọi hàm. */
    record Error(String code, String message) implements AgentEvent {
        public String type() { return "error"; }
    }
}
