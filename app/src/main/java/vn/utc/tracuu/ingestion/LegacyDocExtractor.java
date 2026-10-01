package vn.utc.tracuu.ingestion;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bóc văn bản từ Word đời cũ ({@code .doc}).
 *
 * <p>Định dạng này không mang kiểu tiêu đề đọc được dễ như docx, nên phân cấp suy hoàn toàn từ
 * văn phong pháp quy. Nhiều văn bản hành chính cũ vẫn ở định dạng này, nên bỏ qua nó là bỏ một
 * phần kho thật chứ không phải bỏ một ca hiếm.
 */
@Component
public class LegacyDocExtractor implements TextExtractor {

    private final VietnameseHeadingDetector headings;

    public LegacyDocExtractor(VietnameseHeadingDetector headings) {
        this.headings = headings;
    }

    @Override
    public boolean supports(String extension) {
        return "doc".equals(extension);
    }

    @Override
    public ExtractedContent extract(Path file) {
        String[] paragraphs;
        try (InputStream in = Files.newInputStream(file);
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            paragraphs = extractor.getParagraphText();
        } catch (java.io.IOException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Không đọc được tệp doc: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Tệp doc hỏng hoặc đặt mật khẩu: " + e.getMessage(), e);
        }

        List<TextBlock> blocks = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        String title = null;

        for (String p : paragraphs) {
            String t = p == null ? "" : p.strip();
            if (t.isEmpty()) continue;
            int start = plain.length();
            plain.append(t).append(System.lineSeparator());
            int level = headings.levelOf(t);
            if (level > 0) {
                blocks.add(TextBlock.heading(level, t, start, plain.length() - 1));
                if (title == null && level <= 2) title = headings.shorten(t);
            } else {
                blocks.add(TextBlock.paragraph(t, start, plain.length() - 1));
            }
        }

        if (plain.isEmpty()) {
            throw new ExtractionException(ErrorCode.TEP_KHONG_CO_VAN_BAN,
                    "Tệp doc không có đoạn văn bản nào");
        }
        return new ExtractedContent(plain.toString(), blocks, true, title);
    }
}
