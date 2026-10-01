package vn.utc.tracuu.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Thay thế dịch vụ mô hình ngôn ngữ bằng bản giả tất định, cho kiểm thử tích hợp.
 *
 * <p><strong>Vì sao thay, và vì sao thay như thế này.</strong> Điều cần kiểm ở tầng tích hợp
 * không phải chất lượng câu trả lời — cái đó thuộc bộ đo, và nó cần mô hình thật. Điều cần
 * kiểm ở đây là <em>hạ tầng</em>: lược đồ khớp thực thể chưa, truy vấn truy hồi lai chạy đúng
 * chưa, lọc quyền có thật sự chặn chưa, vector ghi và đọc lại có khớp chưa. Một mô hình thật
 * ở đây chỉ thêm thời gian chờ và thêm yếu tố ngẫu nhiên vào đúng chỗ cần tất định.
 *
 * <p>Bản giả này <strong>không phải mock rỗng</strong>. Phần sinh biểu diễn ngữ nghĩa là một
 * phép nhúng túi-từ thật: cùng một từ luôn rơi vào cùng một chiều, nên hai đoạn văn giống nhau
 * cho hai vector gần nhau. Nhờ đó nhánh tìm theo vector <em>thật sự được kiểm</em> — một mock
 * trả vector ngẫu nhiên sẽ làm phép kiểm xanh mà không chứng minh được gì.
 */
@TestConfiguration
public class TestDoubles {

    @Bean
    @Primary
    public OllamaClient stubOllamaClient(TracuuProperties props, ObjectMapper json) {
        return new StubOllamaClient(props, json);
    }

    /** Bản giả tất định của máy khách mô hình. */
    public static class StubOllamaClient extends OllamaClient {

        private final int dimension;

        public StubOllamaClient(TracuuProperties props, ObjectMapper json) {
            super(props, json);
            this.dimension = props.embedding().dimension();
        }

        @Override
        public boolean reachable() {
            return true;
        }

        @Override
        public List<String> availableModels() {
            return List.of("stub-model");
        }

        @Override
        public VramStatus vramStatus() {
            return new VramStatus(true, 1024L * 1024 * 1024, 1024L * 1024 * 1024);
        }

        /**
         * Sinh biểu diễn ngữ nghĩa bằng phép nhúng túi-từ có băm.
         *
         * <p>Tất định và phản ánh được độ giống nhau về từ vựng: đủ để nhánh tìm theo vector
         * trả về đúng đoạn khi đoạn ấy dùng chung từ với câu hỏi. Không mô phỏng ngữ nghĩa sâu,
         * và không cần — phần ngữ nghĩa sâu là việc của mô hình thật, đo ở bộ đo.
         */
        @Override
        public float[] embed(String text, String model) {
            float[] vector = new float[dimension];
            if (text == null || text.isBlank()) {
                vector[0] = 1f;   // vector khác không, để phép chuẩn hoá không chia cho 0
                return vector;
            }
            for (String token : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
                if (token.isEmpty()) continue;
                int slot = Math.abs(token.hashCode()) % dimension;
                vector[slot] += 1f;
            }
            return vector;
        }

        /** Trả lời bằng cách trích lại chính nguồn, kèm số hiệu nguồn — để tầng kiểm gắn nguồn qua được. */
        @Override
        public String generate(String systemPrompt, String userPrompt) {
            return answerFrom(userPrompt);
        }

        @Override
        public String generateStreaming(String systemPrompt, String userPrompt,
                                        Consumer<String> onToken, BooleanSupplier cancelled) {
            String answer = answerFrom(userPrompt);
            for (String word : answer.split(" ")) {
                if (cancelled.getAsBoolean()) break;
                onToken.accept(word + " ");
            }
            return answer;
        }

        /**
         * Dựng câu trả lời từ chính phần Nguồn trong lời nhắc.
         *
         * <p>Cố ý bám sát nguồn: tầng chặn thứ hai kiểm chồng lấn từ vựng giữa câu trả lời và
         * đoạn nguồn, nên một bản giả trả về văn bản bất kỳ sẽ bị chặn và phép kiểm tích hợp
         * sẽ đo sai thứ. Ở đây bản giả hành xử như một mô hình <em>ngoan</em>; hành vi bịa đặt
         * được kiểm riêng ở {@code GroundingCheckerTest}.
         */
        private String answerFrom(String userPrompt) {
            if (userPrompt == null) return "Không có nội dung.";
            int start = userPrompt.indexOf("Nguồn:");
            if (start < 0) return "Không tìm thấy phần nguồn trong lời nhắc.";

            String sources = userPrompt.substring(start + "Nguồn:".length());
            int questionAt = sources.indexOf("Câu hỏi:");
            if (questionAt > 0) sources = sources.substring(0, questionAt);

            StringBuilder answer = new StringBuilder();
            for (String line : sources.split("\\R")) {
                String t = line.strip();
                if (t.isEmpty() || t.startsWith("[Nguồn")) continue;
                answer.append(t).append(' ');
                if (answer.length() > 400) break;
            }
            if (answer.isEmpty()) return "Không có nội dung nguồn.";
            return answer.toString().strip() + " [1]";
        }
    }
}
