package vn.utc.tracuu.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bóc văn bản từ tệp {@code .pdf}.
 *
 * <p>Ba nhánh lỗi của định dạng này <strong>khác nhau về hành động người dùng phải làm</strong>,
 * nên chúng được tách riêng thay vì gộp thành "không đọc được tệp":
 * <ul>
 *   <li><em>Đặt mật khẩu</em> — người dùng gỡ mật khẩu rồi tải lại đúng tệp ấy.</li>
 *   <li><em>Hỏng cấu trúc</em> — người dùng phải đi tìm bản khác.</li>
 *   <li><em>Không có lớp văn bản</em> (bản chụp quét) — tệp vẫn tốt, nhưng người dùng cần bản
 *       có chữ thật. Báo "tệp hỏng" ở ca này là đẩy họ đi sửa nhầm chỗ.</li>
 * </ul>
 *
 * <p>Pdf không mang cấu trúc phân cấp nào — nó chỉ mang vị trí chữ trên trang. Đường dẫn phân
 * cấp vì vậy suy hoàn toàn từ văn phong pháp quy, và tài liệu không theo văn phong ấy sẽ tách
 * đoạn nông hơn docx. Đây là giới hạn của định dạng, không phải của khâu bóc chữ.
 */
@Component
public class PdfExtractor implements TextExtractor {

    /** Dưới ngưỡng này coi như không có lớp văn bản — bản quét thường trả vài ký tự rác. */
    private static final int MIN_MEANINGFUL_CHARS = 40;

    private final VietnameseHeadingDetector headings;

    public PdfExtractor(VietnameseHeadingDetector headings) {
        this.headings = headings;
    }

    @Override
    public boolean supports(String extension) {
        return "pdf".equals(extension);
    }

    @Override
    public ExtractedContent extract(Path file) {
        String raw;
        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            stripper.setParagraphEnd("\n");
            raw = stripper.getText(doc);

        } catch (InvalidPasswordException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Tệp pdf đặt mật khẩu nên không mở được", e);
        } catch (java.io.IOException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Không đọc được tệp pdf: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Tệp pdf hỏng cấu trúc: " + e.getMessage(), e);
        }

        String text = raw == null ? "" : raw.strip();
        if (text.replaceAll("\\s+", "").length() < MIN_MEANINGFUL_CHARS) {
            throw new ExtractionException(ErrorCode.TEP_KHONG_CO_VAN_BAN,
                    "Tệp pdf không có lớp văn bản, nhiều khả năng là bản chụp quét");
        }
        return buildBlocks(text);
    }

    /** Ghép dòng thành đoạn rồi nhận diện tiêu đề — pdf trả về từng dòng, không phải từng đoạn. */
    private ExtractedContent buildBlocks(String text) {
        List<TextBlock> blocks = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        int bufferStart = 0;
        String title = null;

        for (String line : text.split("\n")) {
            String t = line.strip();

            if (t.isEmpty() || headings.isHeading(t)) {
                bufferStart = flush(buffer, bufferStart, plain, blocks);
                if (!t.isEmpty()) {
                    int start = plain.length();
                    plain.append(t).append('\n');
                    int level = headings.levelOf(t);
                    blocks.add(TextBlock.heading(level, t, start, plain.length() - 1));
                    if (title == null && level <= 2) title = headings.shorten(t);
                    bufferStart = plain.length();
                }
                continue;
            }

            if (buffer.isEmpty()) bufferStart = plain.length();
            if (!buffer.isEmpty()) buffer.append(' ');
            buffer.append(t);

            // Dòng kết thúc bằng dấu chấm câu: hết đoạn.
            if (t.endsWith(".") || t.endsWith(";") || t.endsWith(":")) {
                bufferStart = flush(buffer, bufferStart, plain, blocks);
            }
        }
        flush(buffer, bufferStart, plain, blocks);

        return new ExtractedContent(plain.toString(), blocks, true, title);
    }

    private int flush(StringBuilder buffer, int start, StringBuilder plain, List<TextBlock> blocks) {
        if (buffer.isEmpty()) return plain.length();
        String paragraph = buffer.toString().strip();
        buffer.setLength(0);
        if (paragraph.isEmpty()) return plain.length();
        plain.append(paragraph).append('\n');
        blocks.add(TextBlock.paragraph(paragraph, start, plain.length() - 1));
        return plain.length();
    }
}
