package vn.utc.tracuu.agent.planner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Bước 4 — phân rã câu hỏi phức tạp thành các truy vấn con có thứ tự.
 *
 * <p>Câu <em>"so sánh chế độ nghỉ ốm của nhân viên chính thức và nhân viên thử việc"</em> không
 * có một đoạn nào trả lời trọn. Nó cần hai lần truy hồi rồi mới tổng hợp. Truy hồi thẳng câu
 * gốc sẽ lấy về một mớ đoạn nói loáng thoáng về cả hai, và câu trả lời sẽ chung chung.
 *
 * <p><strong>Chuỗi phụ thuộc là thứ đáng giá nhất ở đây.</strong> Truy vấn con thứ hai đôi khi
 * cần kết quả của truy vấn con thứ nhất (<em>"mức phạt cho hành vi ở Điều bao nhiêu"</em> phải
 * biết Điều nào trước). {@code dependsOnOrdinal} ghi lại ràng buộc ấy, và cũng chính là nguồn
 * cho khối <em>"các bước đã thực hiện"</em> mà người dùng xem được.
 *
 * <p>Giới hạn số truy vấn con để ở cấu hình: mỗi truy vấn con là một lần truy hồi cộng thêm
 * ngữ cảnh đưa vào mô hình, nên con số này ăn thẳng vào ngưỡng độ trễ của câu phức tạp.
 */
@Component
public class QueryPlanner {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanner.class);

    private static final String SYSTEM = """
            Bạn chia một câu hỏi phức tạp thành các truy vấn con đơn giản để tra cứu tài liệu nội bộ.
            Quy tắc bắt buộc:
            - Mỗi dòng một truy vấn con, đánh số 1., 2., 3.
            - Nếu một truy vấn con cần kết quả của truy vấn trước, ghi [cần:N] ở cuối dòng.
            - Mỗi truy vấn con phải tự đứng được, viết bằng tiếng Việt, ngắn gọn.
            - KHÔNG trả lời câu hỏi. KHÔNG giải thích. Chỉ liệt kê truy vấn con.
            - Câu hỏi không cần chia thì trả về đúng một dòng là chính câu hỏi đó.
            """;

    private final OllamaClient llm;
    private final TracuuProperties props;

    public QueryPlanner(OllamaClient llm, TracuuProperties props) {
        this.llm = llm;
        this.props = props;
    }

    public AgentContext apply(AgentContext ctx) {
        String question = ctx.effectiveQuestion();

        if (ctx.complexity() == AgentContext.Complexity.DON_GIAN) {
            return ctx.withSubQueries(List.of(new AgentContext.SubQuery(1, question, null, 0)));
        }

        try {
            String raw = llm.generate(SYSTEM, "Câu hỏi: " + question + "\n\nCác truy vấn con:");
            List<AgentContext.SubQuery> parsed = parse(raw, props.agent().maxSubqueries());

            if (parsed.isEmpty()) {
                return ctx.withSubQueries(List.of(new AgentContext.SubQuery(1, question, null, 0)));
            }
            log.debug("Phân rã thành {} truy vấn con", parsed.size());
            return ctx.withSubQueries(parsed);

        } catch (RuntimeException e) {
            // Không phân rã được thì hỏi thẳng câu gốc. Kết quả kém hơn nhưng vẫn có kết quả.
            log.warn("Không phân rã được câu hỏi, dùng nguyên câu: {}", e.getMessage());
            return ctx.withSubQueries(List.of(new AgentContext.SubQuery(1, question, null, 0)));
        }
    }

    /**
     * Đọc danh sách truy vấn con từ kết quả mô hình.
     *
     * <p>Chấp nhận nhiều kiểu đánh số vì mô hình không ổn định ở chỗ này. Dòng nào không đọc
     * được thì bỏ qua thay vì hỏng cả bước — phân rã là bước <em>tăng cường</em>, không phải
     * bước bắt buộc.
     */
    List<AgentContext.SubQuery> parse(String raw, int max) {
        List<AgentContext.SubQuery> out = new ArrayList<>();
        if (raw == null) return out;

        for (String line : raw.split("\\R")) {
            String t = line.strip();
            if (t.isEmpty() || out.size() >= max) continue;

            Integer dependsOn = null;
            var matcher = java.util.regex.Pattern
                    .compile("\\[\\s*cần\\s*:\\s*(\\d+)\\s*]", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(t);
            if (matcher.find()) {
                int ref = Integer.parseInt(matcher.group(1));
                // Chỉ nhận phụ thuộc vào truy vấn ĐỨNG TRƯỚC. Phụ thuộc tiến hoặc phụ thuộc
                // chính mình là vòng lặp, và ràng buộc cơ sở dữ liệu cũng chặn nó.
                if (ref >= 1 && ref <= out.size()) dependsOn = ref;
                t = matcher.replaceAll("").strip();
            }

            t = t.replaceFirst("^\\d+[.)]\\s*", "")
                 .replaceFirst("^[-*•]\\s*", "")
                 .strip();

            if (t.length() < 4) continue;
            out.add(new AgentContext.SubQuery(out.size() + 1, t, dependsOn, 0));
        }
        return out;
    }
}
