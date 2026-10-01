package vn.utc.tracuu.ingestion;

import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bóc văn bản từ tệp {@code .docx}, giữ thứ tự đoạn và bảng đúng như trong tài liệu.
 *
 * <p>Điểm đáng chú ý: duyệt theo {@link IBodyElement} chứ không duyệt riêng danh sách đoạn rồi
 * danh sách bảng. Duyệt riêng thì mọi bảng bị dồn xuống cuối, và một bảng nằm giữa Điều 5 sẽ
 * mang đường dẫn phân cấp của điều cuối cùng trong tài liệu — sai một cách lặng lẽ.
 *
 * <p>Cấp tiêu đề lấy từ kiểu định dạng của Word khi có ({@code Heading 1}…), và rơi về nhận diện
 * theo văn phong pháp quy khi tài liệu không dùng kiểu — phần lớn văn bản hành chính Việt Nam
 * thuộc nhóm sau.
 */
@Component
public class DocxExtractor implements TextExtractor {

    private final VietnameseHeadingDetector headings;

    public DocxExtractor(VietnameseHeadingDetector headings) {
        this.headings = headings;
    }

    @Override
    public boolean supports(String extension) {
        return "docx".equals(extension);
    }

    @Override
    public ExtractedContent extract(Path file) {
        List<TextBlock> blocks = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        String title = null;

        try (InputStream in = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph p) {
                    String text = p.getText() == null ? "" : p.getText().strip();
                    if (text.isEmpty()) continue;

                    int start = plain.length();
                    plain.append(text).append('\n');
                    int end = plain.length() - 1;

                    int level = styleLevel(p);
                    if (level == 0) level = headings.levelOf(text);

                    if (level > 0) {
                        blocks.add(TextBlock.heading(level, text, start, end));
                        if (title == null && level <= 2) title = headings.shorten(text);
                    } else {
                        blocks.add(TextBlock.paragraph(text, start, end));
                    }

                } else if (element instanceof XWPFTable t) {
                    ExtractedTable table = readTable(t);
                    if (table.rowCount() == 0 && !table.hasHeader()) continue;

                    String rendered = table.renderAll();
                    int start = plain.length();
                    plain.append(rendered).append('\n');
                    blocks.add(TextBlock.table(table, rendered, start, plain.length() - 1));
                }
            }

        } catch (OLE2NotOfficeXmlFileException e) {
            throw new ExtractionException(ErrorCode.DINH_DANG_KHONG_HO_TRO,
                    "Tệp mang đuôi docx nhưng bên trong là định dạng Word cũ", e);
        } catch (java.io.IOException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Không mở được tệp docx: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Tệp docx hỏng cấu trúc: " + e.getMessage(), e);
        }

        if (plain.isEmpty()) {
            throw new ExtractionException(ErrorCode.TEP_KHONG_CO_VAN_BAN,
                    "Tệp docx không có đoạn văn bản nào");
        }
        return new ExtractedContent(plain.toString(), blocks, true, title);
    }

    /** Cấp lấy từ kiểu định dạng Word. Trả 0 khi tài liệu không dùng kiểu tiêu đề. */
    private int styleLevel(XWPFParagraph p) {
        String style = p.getStyle();
        if (style == null) return 0;
        String s = style.toLowerCase();
        if (s.startsWith("heading")) {
            String digits = s.replaceAll("\\D+", "");
            if (!digits.isEmpty()) return Math.min(6, Integer.parseInt(digits));
        }
        if (s.contains("title")) return 1;
        return 0;
    }

    private ExtractedTable readTable(XWPFTable t) {
        List<String> header = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        List<XWPFTableRow> tableRows = t.getRows();
        for (int i = 0; i < tableRows.size(); i++) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : tableRows.get(i).getTableCells()) {
                String v = cell.getText() == null ? "" : cell.getText().strip().replace('\n', ' ');
                cells.add(v);
            }
            if (cells.stream().allMatch(String::isEmpty)) continue;
            // Hàng đầu coi là dòng tiêu đề. Bảng có ô gộp vẫn giữ được quan hệ hàng–cột vì POI
            // trả ô gộp lặp lại ở mỗi hàng nó phủ.
            if (i == 0 && header.isEmpty()) header.addAll(cells); else rows.add(cells);
        }
        return new ExtractedTable(header, rows, null);
    }
}
