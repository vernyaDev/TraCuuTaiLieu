package vn.utc.tracuu.ingestion;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bóc dữ liệu từ tệp bảng tính.
 *
 * <p>Mỗi trang tính thành một bảng, tên trang tính thành tiêu đề cấp hai. Cách này giữ được điều
 * quan trọng nhất: câu trả lời trích từ bảng lương tháng 3 thì đường dẫn phân cấp nói rõ
 * <em>Bảng lương › Tháng 3</em>, thay vì chỉ nói tên tệp.
 */
@Component
public class XlsxExtractor implements TextExtractor {

    private static final int MAX_ROWS_PER_SHEET = 5000;

    @Override
    public boolean supports(String extension) {
        return "xlsx".equals(extension) || "xls".equals(extension);
    }

    @Override
    public ExtractedContent extract(Path file) {
        List<TextBlock> blocks = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        String title = null;

        try (InputStream in = Files.newInputStream(file);
             Workbook wb = WorkbookFactory.create(in)) {

            DataFormatter fmt = new DataFormatter();

            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                if (title == null) title = sheetName;

                int start = plain.length();
                plain.append(sheetName).append(System.lineSeparator());
                blocks.add(TextBlock.heading(2, sheetName, start, plain.length() - 1));

                List<String> header = new ArrayList<>();
                List<List<String>> rows = new ArrayList<>();

                int count = 0;
                for (Row row : sheet) {
                    if (++count > MAX_ROWS_PER_SHEET) break;
                    List<String> cells = new ArrayList<>();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        cells.add(fmt.formatCellValue(cell).strip());
                    }
                    if (cells.stream().allMatch(String::isEmpty)) continue;
                    if (header.isEmpty()) header.addAll(cells); else rows.add(cells);
                }

                if (header.isEmpty() && rows.isEmpty()) continue;

                ExtractedTable table = new ExtractedTable(header, rows, sheetName);
                String rendered = table.renderAll();
                int tStart = plain.length();
                plain.append(rendered).append(System.lineSeparator());
                blocks.add(TextBlock.table(table, rendered, tStart, plain.length() - 1));
            }

        } catch (java.io.IOException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Không đọc được tệp bảng tính: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ExtractionException(ErrorCode.TEP_HONG,
                    "Tệp bảng tính hỏng hoặc đặt mật khẩu: " + e.getMessage(), e);
        }

        if (plain.isEmpty()) {
            throw new ExtractionException(ErrorCode.TEP_KHONG_CO_VAN_BAN,
                    "Tệp bảng tính không có ô nào có dữ liệu");
        }
        return new ExtractedContent(plain.toString(), blocks, true, title);
    }
}
