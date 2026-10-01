package vn.utc.tracuu.platform.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Máy khách của dịch vụ mô hình ngôn ngữ.
 *
 * <p>Đây là hạ tầng dùng chung: cả {@code ingestion/} (gợi ý metadata) lẫn {@code agent/action}
 * (sinh câu trả lời) đều gọi qua đây. Luật R7 nói rõ hai đường ấy dùng chung dịch vụ nhưng
 * <strong>khác hoàn toàn đường mã trả kết quả</strong> — nên lớp này cố ý chỉ biết gửi và nhận,
 * không biết gì về nghiệp vụ của hai bên gọi.
 *
 * <p>Dùng {@link HttpClient} có sẵn trong máy ảo Java, không thêm thư viện. Sinh chữ trả về
 * theo dòng để đẩy dần ra luồng sự kiện — người dùng thấy chữ đầu tiên sau vài giây thay vì
 * nhìn màn hình trắng tới khi xong.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    /** Chỉ kiểm một lần cho mỗi lần chạy — kiểm mỗi lượt là thêm một lượt gọi mạng vô ích. */
    private final java.util.concurrent.atomic.AtomicBoolean daKiemGpu =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private final TracuuProperties props;
    private final ObjectMapper json;
    private final HttpClient http;

    public OllamaClient(TracuuProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
        this.http = HttpClient.newBuilder()
                .connectTimeout(props.llm().connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    // ── Sinh chữ ───────────────────────────────────────────────────────────────────────────

    /**
     * Sinh chữ, đẩy từng mẩu qua {@code onToken}.
     *
     * <p>{@code cancelled} được hỏi trước mỗi mẩu: người gọi ngắt kết nối thì máy chủ
     * <strong>dừng sinh chữ</strong> chứ không chạy tiếp cho hết rồi vứt kết quả
     * (hợp đồng giao diện Mục 3).
     *
     * @return toàn văn đã sinh, kể cả khi bị dừng giữa chừng
     */
    public String generateStreaming(String systemPrompt, String userPrompt,
                                    Consumer<String> onToken,
                                    java.util.function.BooleanSupplier cancelled) {
        ObjectNode body = baseRequest(systemPrompt, userPrompt);
        body.put("stream", true);

        StringBuilder full = new StringBuilder();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.llm().baseUrl() + "/api/generate"))
                    .timeout(props.llm().readTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> res =
                    http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                throw new LlmUnavailableException("Máy chủ mô hình trả mã " + res.statusCode());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    if (cancelled.getAsBoolean()) {
                        log.debug("Người gọi đã ngắt, dừng sinh chữ.");
                        break;
                    }
                    JsonNode node = json.readTree(line);
                    String piece = node.path("response").asText("");
                    if (!piece.isEmpty()) {
                        full.append(piece);
                        onToken.accept(piece);
                    }
                    if (node.path("done").asBoolean(false)) break;
                }
            }
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmUnavailableException("Không gọi được máy chủ mô hình: " + e.getMessage(), e);
        }
        kiemGpuLanDau();
        return full.toString();
    }

    /**
     * Kiểm lại vị trí mô hình sau lượt sinh chữ ĐẦU TIÊN — lời hứa mà phép kiểm khởi động số 4
     * để lại.
     *
     * <p>Máy chủ mô hình nạp mô hình lười: lần gọi đầu mới nạp. Nên lúc ứng dụng khởi động,
     * trạng thái gần như luôn là "chưa nạp", phép kiểm số 4 bỏ qua kèm câu <em>"Sẽ kiểm lại ở
     * câu hỏi đầu tiên"</em> — và <strong>không có dòng mã nào làm việc đó</strong>. Kết quả:
     * phép kiểm đáng giá thứ hai của hệ thống bị bỏ qua vĩnh viễn trong đúng ca thường gặp nhất,
     * là ca mà mọi người đều gặp.
     *
     * <p>Ở đây chỉ ghi nhật ký, KHÔNG ném ngoại lệ: lúc này ứng dụng đã phục vụ, và giết một
     * tiến trình đang trả lời người dùng thì hại hơn là để nó chạy chậm. Nhưng phải kêu to —
     * một cảnh báo mức lỗi là thứ người vận hành thấy được, còn im lặng thì không.
     */
    private void kiemGpuLanDau() {
        if (!daKiemGpu.compareAndSet(false, true)) return;
        try {
            VramStatus st = vramStatus();
            if (!st.loaded()) {
                daKiemGpu.set(false);   // chưa nạp thì chưa kiểm được, để lượt sau kiểm lại
                return;
            }
            double san = props.llm().minGpuRatio();
            if (st.gpuRatio() < san) {
                log.error("Mô hình '{}' chỉ nằm {}% trên GPU (sàn {}%). Phần tràn sang CPU chậm "
                        + "hơn hàng chục lần và kéo theo mọi ngưỡng độ trễ đã cam kết. Dùng mô "
                        + "hình nhỏ hơn, giảm ngữ cảnh tối đa, hoặc hạ sàn nếu đã chấp nhận.",
                        props.llm().model(), Math.round(st.gpuRatio() * 100), Math.round(san * 100));
            } else {
                log.info("Mô hình trên GPU sau lượt sinh chữ đầu: {} MB ({}% mô hình).",
                        st.vramBytes() / (1024 * 1024), Math.round(st.gpuRatio() * 100));
            }
        } catch (RuntimeException e) {
            log.warn("Không kiểm lại được vị trí mô hình sau lượt sinh chữ đầu", e);
        }
    }

    /** Sinh chữ một lần, dùng cho gợi ý metadata và chấm điểm — hai việc không cần đẩy dần. */
    public String generate(String systemPrompt, String userPrompt) {
        ObjectNode body = baseRequest(systemPrompt, userPrompt);
        body.put("stream", false);
        JsonNode res = post("/api/generate", body);
        return res.path("response").asText("");
    }

    private ObjectNode baseRequest(String systemPrompt, String userPrompt) {
        ObjectNode body = json.createObjectNode();
        body.put("model", props.llm().model());
        if (systemPrompt != null && !systemPrompt.isBlank()) body.put("system", systemPrompt);
        body.put("prompt", userPrompt);

        // NFR-system-044: yếu tố ngẫu nhiên cố định được VÀ ghi cùng kết quả đo.
        ObjectNode options = body.putObject("options");
        options.put("temperature", props.llm().temperature());
        options.put("top_p", props.llm().topP());
        options.put("seed", props.llm().seed());
        options.put("num_predict", props.llm().numPredict());
        return body;
    }

    // ── Biểu diễn ngữ nghĩa (đường dự phòng) ───────────────────────────────────────────────

    /**
     * Sinh biểu diễn ngữ nghĩa qua máy chủ mô hình.
     *
     * <p>Đây là đường <strong>dự phòng</strong>. Đường chính chạy ONNX ngay trong máy ảo Java
     * trên CPU, vì GPU dành cho sinh chữ (kiến trúc Mục 1). Dùng đường này nghĩa là việc nạp tài
     * liệu tranh GPU với việc trả lời câu hỏi — chấp nhận được khi chưa có mô hình ONNX, nhưng
     * không phải hình dạng đã thiết kế.
     */
    public float[] embed(String text, String model) {
        ObjectNode body = json.createObjectNode();
        body.put("model", model);
        body.put("prompt", text);
        JsonNode res = post("/api/embeddings", body);
        JsonNode arr = res.path("embedding");
        float[] out = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) out[i] = (float) arr.get(i).asDouble();
        return out;
    }

    // ── Dùng cho phép kiểm khởi động ───────────────────────────────────────────────────────

    public boolean reachable() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.llm().baseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            return http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> availableModels() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.llm().baseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            JsonNode res = json.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
            return res.path("models").findValuesAsText("name");
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Mô hình đang nạp có nằm trên bộ nhớ đồ hoạ không.
     *
     * <p>Phục vụ phép kiểm khởi động số 4. {@code size_vram} bằng 0 nghĩa là mô hình đang chạy
     * trên CPU — hệ thống vẫn trả lời được, chỉ là chậm tới mức không dùng nổi, và người vận
     * hành không có cách nào biết nếu không kiểm. NFR-system-020: thà không chạy còn hơn chạy chậm.
     */
    public VramStatus vramStatus() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.llm().baseUrl() + "/api/ps"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            JsonNode res = json.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
            for (JsonNode m : res.path("models")) {
                if (m.path("name").asText("").startsWith(props.llm().model().split(":")[0])) {
                    long vram = m.path("size_vram").asLong(0);
                    long total = m.path("size").asLong(0);
                    return new VramStatus(true, vram, total);
                }
            }
            return new VramStatus(false, 0, 0);
        } catch (Exception e) {
            return new VramStatus(false, 0, 0);
        }
    }

    /** {@code loaded} sai nghĩa là mô hình chưa nạp; {@code vramBytes} bằng 0 nghĩa là đang chạy CPU. */
    public record VramStatus(boolean loaded, long vramBytes, long totalBytes) {
        public boolean onGpu() { return loaded && vramBytes > 0; }
        public double gpuRatio() { return totalBytes == 0 ? 0 : (double) vramBytes / totalBytes; }
    }

    private JsonNode post(String path, ObjectNode body) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.llm().baseUrl() + path))
                    .timeout(props.llm().readTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() != 200) {
                throw new LlmUnavailableException("Máy chủ mô hình trả mã " + res.statusCode());
            }
            return json.readTree(res.body());
        } catch (LlmUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmUnavailableException("Không gọi được máy chủ mô hình: " + e.getMessage(), e);
        }
    }

    /** Máy chủ mô hình không dùng được. Tách riêng để tầng trên phân biệt với lỗi nghiệp vụ. */
    public static class LlmUnavailableException extends RuntimeException {
        public LlmUnavailableException(String message) { super(message); }
        public LlmUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
