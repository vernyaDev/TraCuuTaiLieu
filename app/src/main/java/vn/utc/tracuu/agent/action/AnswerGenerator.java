package vn.utc.tracuu.agent.action;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Sinh câu trả lời từ ngữ cảnh đã dựng, đẩy chữ ra dần.
 *
 * <p>Lời nhắc hệ thống ở đây là phần quan trọng nhất của cả bước: nó là thứ quyết định mô hình
 * trả lời <em>theo tài liệu</em> hay trả lời <em>theo trí nhớ của nó</em>. Bốn ràng buộc trong
 * lời nhắc đều có lý do cụ thể, và không ràng buộc nào là trang trí.
 */
@Component
public class AnswerGenerator {

    /**
     * Lời nhắc hệ thống.
     *
     * <p>Điều khoản <em>"chỉ dùng thông tin trong phần Nguồn"</em> là điều kiện để tầng kiểm
     * gắn nguồn có nghĩa. Điều khoản <em>"không đủ thì nói không đủ"</em> là thứ ngăn mô hình
     * lấp chỗ trống bằng kiến thức chung — loại lỗi nguy hiểm nhất ở hệ thống nội bộ, vì câu
     * trả lời nghe rất hợp lý mà lại không phải quy định của công ty này.
     */
    private static final String SYSTEM = """
            Bạn trả lời câu hỏi của nhân viên dựa trên tài liệu nội bộ của công ty.

            Quy tắc bắt buộc:
            1. CHỈ dùng thông tin có trong phần Nguồn bên dưới. Tuyệt đối không dùng kiến thức
               bên ngoài, không suy đoán, không lấp chỗ trống.
            2. Sau mỗi ý, ghi số hiệu nguồn trong ngoặc vuông, ví dụ [1] hoặc [2][3].
            3. Nếu các nguồn không đủ để trả lời, hãy nói thẳng là không đủ căn cứ. Đừng cố
               dựng một câu trả lời từ thông tin không có.
            4. Trả lời bằng tiếng Việt, đi thẳng vào nội dung, không mở đầu bằng lời khách sáo.
               Trích nguyên văn số liệu, thời hạn và mã văn bản nếu có.
            5. KHÔNG chép lại dòng tiêu đề "[Nguồn N] Tên tài liệu" ở đầu câu trả lời. Dòng đó
               là nhãn của phần Nguồn, không phải nội dung để trả lời.
            """;

    private final OllamaClient llm;

    public AnswerGenerator(OllamaClient llm) {
        this.llm = llm;
    }

    /**
     * Sinh câu trả lời, đẩy từng mẩu chữ qua {@code onToken}.
     *
     * @param cancelled hỏi trước mỗi mẩu — người gọi ngắt kết nối thì dừng sinh chữ, không chạy
     *                  tiếp cho hết rồi vứt kết quả. Với một mô hình dùng chung GPU, chạy tiếp
     *                  một lượt bị bỏ là lấy mất lượt của người đang chờ.
     */
    public String generate(AgentContext ctx, ContextBuilder.Context context,
                           Consumer<String> onToken, BooleanSupplier cancelled) {
        String prompt = buildPrompt(ctx, context);
        var locNhan = new LocNhanNguonODau(onToken);
        String answer = llm.generateStreaming(SYSTEM, prompt, locNhan, cancelled);
        locNhan.xaPhanConLai();
        return answer;
    }

    /**
     * Bỏ nhãn <code>[Nguồn N]</code> mà mô hình chép lại ở ĐẦU câu trả lời.
     *
     * <p>Quy tắc 5 của lời nhắc đã cấm việc này, nhưng mô hình 3B vẫn làm — và lời nhắc không
     * phải là cơ chế thi hành. Người đọc thấy câu trả lời mở đầu bằng một nhãn của phần Nguồn,
     * tưởng đó là nội dung.
     *
     * <p><strong>Chỉ bỏ dạng có chữ "Nguồn", chỉ ở vị trí đầu.</strong> Dấu <code>[1]</code>,
     * <code>[3]</code> trần là trích dẫn thật, mô hình được yêu cầu ghi chúng — xoá đi là xoá
     * mất thứ cả tầng kiểm gắn nguồn lẫn danh sách trích dẫn dựa vào.
     *
     * <p>Lọc trên LUỒNG chữ chứ không sửa sau, vì chữ đã đẩy ra màn hình thì không rút lại được.
     * Nên phải giữ lại vài mẩu đầu đủ để quyết định, rồi mới cho chảy tiếp. Chuỗi trả về của
     * {@code generateStreaming} vẫn là bản GỐC chưa lọc — {@link #citedOnly} đọc bản đó, và nó
     * cần cả nhãn bị bỏ để biết nguồn nào thật sự được nhắc.
     */
    static final class LocNhanNguonODau implements Consumer<String> {

        /** Đủ dài để chứa "[Nguồn 99] " kể cả khi mô hình chèn khoảng trắng. */
        private static final int DU_DE_QUYET = 24;

        private static final java.util.regex.Pattern NHAN_O_DAU = java.util.regex.Pattern
                .compile("^\\s*\\[\\s*Nguồn\\s*\\d{1,2}\\s*]\\s*",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                                | java.util.regex.Pattern.UNICODE_CASE);

        private final Consumer<String> ra;
        private final StringBuilder dem = new StringBuilder();
        private boolean daQuyet;

        LocNhanNguonODau(Consumer<String> ra) { this.ra = ra; }

        @Override
        public void accept(String mau) {
            if (daQuyet) { ra.accept(mau); return; }
            dem.append(mau);
            if (dem.length() < DU_DE_QUYET) return;
            xa();
        }

        /** Câu trả lời ngắn hơn ngưỡng quyết định thì phần đệm vẫn phải được đẩy ra. */
        void xaPhanConLai() {
            if (!daQuyet) xa();
        }

        private void xa() {
            daQuyet = true;
            String s = dem.toString();
            // Bỏ LẶP LẠI: mô hình đôi khi chép cả một chuỗi nhãn liền nhau
            // ("[Nguồn 2] [Nguồn 3] [Nguồn 4] …"). Bỏ đúng một nhãn thì phần còn lại vẫn
            // hiện ra như nội dung.
            while (true) {
                var m = NHAN_O_DAU.matcher(s);
                if (!m.find()) break;
                s = s.substring(m.end());
            }
            dem.setLength(0);
            if (!s.isEmpty()) ra.accept(s);
        }
    }

    private String buildPrompt(AgentContext ctx, ContextBuilder.Context context) {
        StringBuilder sb = new StringBuilder();

        // Các bước đã phân rã giúp mô hình biết câu hỏi có mấy phần cần trả lời — thiếu phần nào
        // là lỗi hay gặp nhất với câu hỏi so sánh.
        if (ctx.subQueries().size() > 1) {
            sb.append("Câu hỏi này gồm các phần sau, hãy trả lời đủ cả:").append(System.lineSeparator());
            for (AgentContext.SubQuery sq : ctx.subQueries()) {
                sb.append("  ").append(sq.ordinal()).append(". ").append(sq.text())
                  .append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }

        sb.append("Nguồn:").append(System.lineSeparator())
          .append(context.text()).append(System.lineSeparator()).append(System.lineSeparator())
          .append("Câu hỏi: ").append(ctx.effectiveQuestion()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("Trả lời (nhớ ghi số hiệu nguồn sau mỗi ý):");
        return sb.toString();
    }

    /**
     * Lọc danh sách trích dẫn xuống còn những nguồn <strong>thật sự được nhắc</strong> trong
     * câu trả lời.
     *
     * <p>Liệt kê cả tám nguồn đã đưa vào ngữ cảnh trong khi câu trả lời chỉ dùng hai là làm
     * loãng giá trị của trích dẫn: người đọc không biết mở cái nào để đối chiếu. Nhưng nếu mô
     * hình <em>không ghi số hiệu nào</em> thì giữ nguyên cả danh sách — thà thừa còn hơn bỏ
     * trống phần nguồn.
     */
    public List<RetrievedChunk> citedOnly(String answer, List<RetrievedChunk> included) {
        if (answer == null || answer.isBlank() || included.isEmpty()) return included;

        java.util.Set<Integer> mentioned = new java.util.LinkedHashSet<>();
        // Nhận cả "[1]" lẫn "[Nguồn 1]". Mô hình nhỏ hay chép lại nguyên dòng nhãn của phần
        // Nguồn; chỉ bắt dạng "[1]" thì mọi lượt như vậy rơi về nhánh giữ-hết, và khối nguồn
        // liệt kê cả tám đoạn trong khi câu trả lời chỉ thực sự dùng một.
        var matcher = java.util.regex.Pattern
                .compile("\\[\\s*(?:Nguồn\\s*)?(\\d{1,2})\\s*]",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                                | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(answer);
        while (matcher.find()) {
            int n = Integer.parseInt(matcher.group(1));
            if (n >= 1 && n <= included.size()) mentioned.add(n);
        }
        if (mentioned.isEmpty()) return included;

        return mentioned.stream().map(n -> included.get(n - 1)).toList();
    }
}
