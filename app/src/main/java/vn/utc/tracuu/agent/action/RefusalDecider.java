package vn.utc.tracuu.agent.action;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.List;
import java.util.Optional;

/**
 * Tầng chặn thứ nhất — quyết định có đủ căn cứ để trả lời không, <strong>trước khi</strong>
 * gọi mô hình sinh chữ.
 *
 * <p>Tầng này không gọi mô hình, nên nó nhanh hơn hẳn đường trả lời. Đó là yêu cầu của
 * {@code NFR-system-004}, và cũng là phép thử: nếu từ chối mất bằng thời gian trả lời thì chỗ
 * chặn đã bị cài sai chỗ.
 *
 * <h2>Điểm tinh vi nhất của cả hệ thống nằm ở đây</h2>
 *
 * <p>Người dùng cần phân biệt được vì sao bị từ chối, nhưng hệ thống lại <strong>không được
 * tiết lộ rằng có tài liệu phù hợp nằm ngoài quyền</strong> của họ. Hai yêu cầu ấy kéo ngược
 * nhau, và chỗ hoà giải là một lối nói rào đón — nhưng nó chỉ bảo vệ được khi <strong>hiện y
 * hệt ở MỌI lần từ chối vì không đủ căn cứ</strong>.
 *
 * <p>Nếu câu <em>"có thể bạn chưa được cấp quyền"</em> chỉ xuất hiện khi thật sự có tài liệu bị
 * chặn, thì nó thôi là một khả năng và trở thành một <em>lời khẳng định</em>: người hỏi chỉ cần
 * so hai lần từ chối khác nhau là suy ra được tài liệu ấy tồn tại. Vì vậy lớp này cố ý
 * <strong>không hề biết</strong> việc đọc-không-lọc cho ra kết quả gì. Việc phân loại lý do
 * thật thuộc về gói {@code gap/}, và nó chỉ đi vào nhật ký khoảng trống cho quản trị — không
 * bao giờ đi ra màn hình người hỏi.
 */
@Component
public class RefusalDecider {

    /** Lý do máy đọc được, ghi vào nhật ký khoảng trống. */
    public static final String THIEU_CAN_CU = "CAN_CU_MONG";
    public static final String THIEU_TAI_LIEU = "THIEU_TAI_LIEU";
    public static final String NGOAI_PHAM_VI = "NGOAI_PHAM_VI";

    public static final String TANG_MOT = "TANG_MOT";
    public static final String TANG_HAI = "TANG_HAI";

    /**
     * Câu từ chối vì không đủ căn cứ — <strong>MỘT câu duy nhất, dùng cho mọi trường hợp</strong>.
     *
     * <p>Ba khả năng được nêu ra như ba khả năng, không khả năng nào được khẳng định. Đừng sinh
     * biến thể theo tình huống: mỗi biến thể là một bit thông tin rò ra ngoài.
     */
    public static final String THONG_DIEP_KHONG_DU_CAN_CU = """
            Tôi không tìm thấy đủ căn cứ trong kho tài liệu để trả lời câu hỏi này.

            Có thể do một trong các lý do sau:
            • Kho chưa có tài liệu nào nói về nội dung bạn hỏi.
            • Tài liệu liên quan nằm ngoài phạm vi bạn được cấp quyền đọc.
            • Câu hỏi cần diễn đạt cụ thể hơn để tìm đúng tài liệu.

            Bạn có thể thử hỏi lại bằng từ ngữ cụ thể hơn, hoặc liên hệ quản trị tri thức \
            để được hỗ trợ.""";

    /** Câu hỏi nằm ngoài phạm vi kho tài liệu nội bộ. Ca này KHÔNG lộ gì nên nói thẳng được. */
    public static final String THONG_DIEP_NGOAI_PHAM_VI = """
            Câu hỏi này nằm ngoài phạm vi kho tài liệu nội bộ của công ty.

            Hệ thống chỉ trả lời dựa trên tài liệu đã được nạp vào kho — quy chế, quy trình, \
            biểu mẫu và văn bản liên quan. Bạn thử đặt câu hỏi về một nội dung trong nhóm đó.""";

    /** Lời chào. Không phải từ chối, nhưng cũng không đi vào truy hồi. */
    public static final String THONG_DIEP_CHAO_HOI = """
            Chào bạn. Tôi tra cứu giúp bạn các tài liệu nội bộ đã được cấp quyền: quy chế, \
            quy trình, biểu mẫu và văn bản liên quan.

            Bạn cứ đặt câu hỏi cụ thể, ví dụ "thủ tục xin nghỉ phép dài ngày gồm những bước nào".""";

    private final TracuuProperties props;

    public RefusalDecider(TracuuProperties props) {
        this.props = props;
    }

    /**
     * Có phải từ chối ở tầng một không.
     *
     * @return ngữ cảnh đã mang quyết định từ chối, hoặc rỗng nếu đủ căn cứ để đi tiếp
     */
    public Optional<AgentContext> decide(AgentContext ctx) {

        if (ctx.intent() == AgentContext.Intent.NGOAI_PHAM_VI) {
            return Optional.of(ctx.withRefusal(NGOAI_PHAM_VI, TANG_MOT, THONG_DIEP_NGOAI_PHAM_VI));
        }

        List<RetrievedChunk> chunks = ctx.chunks();

        // Không có đoạn nào. Lý do THẬT có thể là kho trống, có thể là phạm vi quyền rỗng —
        // ghi khác nhau vào nhật ký, nhưng NÓI RA cùng một câu.
        if (chunks.isEmpty()) {
            String reasonForLog = ctx.permittedDocumentCount() == 0 ? "THIEU_QUYEN" : THIEU_TAI_LIEU;
            return Optional.of(ctx.withRefusal(reasonForLog, TANG_MOT, THONG_DIEP_KHONG_DU_CAN_CU));
        }

        // Có đoạn nhưng điểm quá thấp: liên quan hời hợt còn tệ hơn không có gì, vì mô hình sẽ
        // cố dựng câu trả lời từ vật liệu không đủ.
        double bestScore = chunks.stream()
                .mapToDouble(c -> c.scoreVector() != null ? c.scoreVector() : 0)
                .max().orElse(0);
        long supporting = chunks.stream()
                .filter(c -> c.scoreVector() == null
                        || c.scoreVector() >= props.agent().refusal().minTopScore())
                .count();

        boolean weakEvidence = bestScore < props.agent().refusal().minTopScore()
                && supporting < props.agent().refusal().minSupportingChunks();

        if (weakEvidence) {
            return Optional.of(ctx.withRefusal(THIEU_CAN_CU, TANG_MOT, THONG_DIEP_KHONG_DU_CAN_CU));
        }
        return Optional.empty();
    }

    /**
     * Từ chối ở tầng hai — sau khi đã sinh chữ nhưng không ý nào gắn được về nguồn.
     *
     * <p>Giao diện phải <strong>xoá phần chữ đã hiện</strong> khi nhận sự kiện này
     * ({@code AC-002} của US-024). Dùng đúng thông điệp của tầng một là có chủ đích: người dùng
     * không cần biết hệ thống đã thử rồi bỏ, và hai câu khác nhau lại là hai bit thông tin.
     */
    public AgentContext refuseUngrounded(AgentContext ctx) {
        return ctx.withRefusal(THIEU_CAN_CU, TANG_HAI, THONG_DIEP_KHONG_DU_CAN_CU);
    }
}
