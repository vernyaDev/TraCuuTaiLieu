package vn.utc.tracuu.ingestion;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Bóc văn bản thuần và Markdown. Tiêu đề Markdown được ưu tiên hơn suy đoán theo văn phong. */
@Component
public class PlainTextExtractor implements TextExtractor {

    private static final char HASH = '#';

    private final VietnameseHeadingDetector headings;

    public PlainTextExtractor(VietnameseHeadingDetector headings) {
        this.headings = headings;
    }

    @Override
    public boolean supports(String extension) {
        return "txt".equals(extension) || "md".equals(extension);
    }

    @Override
    public ExtractedContent extract(Path file) {
        String raw;
        try {
            raw = Files.readString(file, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Tệp không phải mã hoá UTF-8 nên đọc ra ký tự hỏng", e);
        } catch (java.io.IOException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG, "Không đọc được tệp", e);
        }

        if (raw.isBlank()) {
            throw new ExtractionException(ErrorCode.TEP_KHONG_CO_VAN_BAN, "Tệp rỗng");
        }

        List<TextBlock> blocks = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        String title = null;

        for (String line : raw.split("\\R")) {
            String t = line.strip();
            if (t.isEmpty()) continue;

            int level;
            String text = t;
            if (t.charAt(0) == HASH) {
                int hashes = 0;
                while (hashes < t.length() && t.charAt(hashes) == HASH) hashes++;
                level = Math.min(6, hashes);
                text = t.substring(hashes).strip();
                if (text.isEmpty()) continue;
            } else {
                level = headings.levelOf(t);
            }

            int start = plain.length();
            plain.append(text).append(System.lineSeparator());
            if (level > 0) {
                blocks.add(TextBlock.heading(level, text, start, plain.length() - 1));
                if (title == null && level <= 2) title = headings.shorten(text);
            } else {
                blocks.add(TextBlock.paragraph(text, start, plain.length() - 1));
            }
        }
        return new ExtractedContent(plain.toString(), blocks, true, title);
    }
}
