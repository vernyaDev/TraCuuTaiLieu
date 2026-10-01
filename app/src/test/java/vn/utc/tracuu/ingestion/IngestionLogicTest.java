package vn.utc.tracuu.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.utc.tracuu.TestProperties;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm phần logic thuần của khâu nạp: nhận diện tiêu đề, tách từ, tách đoạn. */
class IngestionLogicTest {

    private final VietnameseHeadingDetector headings = new VietnameseHeadingDetector();

    @Nested
    @DisplayName("Nhận diện tiêu đề văn bản pháp quy")
    class HeadingDetection {

        @Test
        @DisplayName("Đúng thứ bậc Phần › Chương › Mục › Điều › Khoản › Điểm")
        void thuBacDung() {
            assertTrue(headings.levelOf("PHẦN THỨ NHẤT") < headings.levelOf("Chương VII"));
            assertTrue(headings.levelOf("Chương VII") < headings.levelOf("Mục 2"));
            assertTrue(headings.levelOf("Mục 2") < headings.levelOf("Điều 113"));
            assertTrue(headings.levelOf("Điều 113") < headings.levelOf("Khoản 2"));
            assertTrue(headings.levelOf("Khoản 2") < headings.levelOf("Điểm a"));
        }

        @Test
        @DisplayName("Câu văn xuôi nhắc tới điều luật KHÔNG phải tiêu đề")
        void vanXuoiKhongPhaiTieuDe() {
            // Thiếu chặn này thì một câu giữa bài cướp mất đường dẫn phân cấp, và MỌI đoạn
            // phía sau mang sai đường dẫn — sai âm thầm, không có dấu hiệu nào.
            String vanXuoi = "Điều 5 của quy chế này quy định rằng người lao động có quyền "
                    + "được nghỉ phép năm theo số ngày tương ứng với thời gian làm việc thực tế "
                    + "tại đơn vị, và số ngày đó được tính lại vào đầu mỗi năm dương lịch.";
            assertEquals(0, headings.levelOf(vanXuoi));
        }

        @Test
        @DisplayName("Dòng rỗng và dòng quá dài không phải tiêu đề")
        void bienKhongPhaiTieuDe() {
            assertEquals(0, headings.levelOf(""));
            assertEquals(0, headings.levelOf("   "));
            assertEquals(0, headings.levelOf(null));
            assertEquals(0, headings.levelOf("x".repeat(300)));
        }

        @Test
        @DisplayName("Đánh số thường cho tài liệu không theo chuẩn pháp quy")
        void danhSoThuong() {
            assertTrue(headings.levelOf("1. Mục đích") > 0);
            assertTrue(headings.levelOf("2.1. Phạm vi áp dụng") > headings.levelOf("2. Phạm vi"));
        }

        @Test
        @DisplayName("Rút gọn giữ phần định danh, cắt phần mô tả")
        void rutGonGiuDinhDanh() {
            String shortened = headings.shorten(
                    "Điều 113. Nghỉ hằng năm và cách tính số ngày nghỉ cho người lao động");
            assertTrue(shortened.startsWith("Điều 113"));
            assertTrue(shortened.length() < 40);
        }
    }

    @Nested
    @DisplayName("Tách từ tiếng Việt")
    class Segmentation {

        private VietnameseSegmenter segmenter;

        @BeforeEach
        void setUp() {
            segmenter = new VietnameseSegmenter(TestProperties.defaults());
            segmenter.load();
        }

        @Test
        @DisplayName("Từ điển nạp được từ tài nguyên đóng gói kèm")
        void tuDienNapDuoc() {
            assertTrue(segmenter.isLongestMatch(),
                    "Không nạp được từ điển thì nhánh từ khoá kém hẳn với từ ghép tiếng Việt");
            assertTrue(segmenter.dictionarySize() > 100);
        }

        @Test
        @DisplayName("Nối từ ghép thành MỘT token")
        void noiTuGhep() {
            List<String> tokens = segmenter.tokenize("chế độ bảo hiểm xã hội cho người lao động");

            assertTrue(tokens.contains("bảo_hiểm_xã_hội"),
                    """
                    Cắt theo khoảng trắng thì "xã" và "hội" mang trọng số ngang các từ có nghĩa,
                    và câu hỏi về bảo hiểm xã hội sẽ khớp nhầm mọi văn bản có chữ "hội".""");
            assertTrue(tokens.contains("người_lao_động"));
        }

        @Test
        @DisplayName("Khớp DÀI NHẤT chứ không khớp ngắn trước")
        void khopDaiNhat() {
            List<String> tokens = segmenter.tokenize("bảo hiểm xã hội");
            assertEquals(1, tokens.size(),
                    "Khớp ngắn trước sẽ cắt thành 'bảo hiểm' + 'xã hội' và mất khái niệm gốc");
        }

        @Test
        @DisplayName("Giữ nguyên mã hiệu văn bản như một định danh")
        void giuMaHieuVanBan() {
            List<String> tokens = segmenter.tokenize("theo Nghị định 15/2020/NĐ-CP");
            assertTrue(tokens.stream().anyMatch(t -> t.contains("15/2020")),
                    "Mã hiệu bị cắt vụn thì không tra cứu được theo số hiệu văn bản");
        }

        @Test
        @DisplayName("Chuẩn hoá Unicode: hai cách mã hoá cùng một chữ cho cùng token")
        void chuanHoaUnicode() {
            // "ệ" tổ hợp sẵn so với "ệ" dựng từ e + dấu mũ + dấu nặng. Mắt thường không phân
            // biệt được, nhưng chúng là hai chuỗi khác nhau ở mọi phép so khớp.
            String composed = "hiệu lực";
            String decomposed = java.text.Normalizer.normalize(composed, java.text.Normalizer.Form.NFD);

            assertNotEquals(composed, decomposed, "Hai chuỗi nguồn phải thật sự khác nhau");
            assertEquals(segmenter.tokenize(composed), segmenter.tokenize(decomposed));
        }

        @Test
        @DisplayName("Chuỗi rỗng không làm hỏng gì")
        void chuoiRong() {
            assertTrue(segmenter.tokenize("").isEmpty());
            assertTrue(segmenter.tokenize(null).isEmpty());
            assertEquals("", segmenter.segment(null));
        }
    }

    @Nested
    @DisplayName("Tách đoạn bám cấu trúc")
    class Chunking {

        private StructuralChunker chunker;
        private TracuuProperties.ChunkStrategy params;

        @BeforeEach
        void setUp() {
            VietnameseSegmenter segmenter = new VietnameseSegmenter(TestProperties.defaults());
            segmenter.load();
            chunker = new StructuralChunker(segmenter);
            params = TestProperties.defaults().chunking().strategy("structural");
        }

        @Test
        @DisplayName("Mỗi đoạn mang đường dẫn phân cấp của chỗ nó nằm")
        void duongDanPhanCap() {
            ExtractedContent content = new ExtractedContent(
                    "nội dung", List.of(
                    TextBlock.heading(2, "Chương VII", 0, 10),
                    TextBlock.heading(4, "Điều 113", 11, 20),
                    TextBlock.paragraph("Người lao động được nghỉ hằng năm mười hai ngày làm việc "
                            + "nếu đã làm đủ mười hai tháng cho một người sử dụng lao động.", 21, 140)),
                    true, null);

            List<ChunkCandidate> chunks = chunker.chunk(content, params);

            assertEquals(1, chunks.size());
            assertEquals("Chương VII › Điều 113", chunks.get(0).breadcrumb(),
                    "Thiếu đường dẫn thì trích dẫn chỉ chỉ được tới TÊN TÀI LIỆU");
        }

        @Test
        @DisplayName("Đầu mục mới thì chốt đoạn đang gom, không cắt ngang")
        void khongCatNgangDauMuc() {
            ExtractedContent content = new ExtractedContent("x", List.of(
                    TextBlock.heading(4, "Điều 1", 0, 10),
                    TextBlock.paragraph("Nội dung của điều một, đủ dài để thành một đoạn riêng biệt.", 11, 70),
                    TextBlock.heading(4, "Điều 2", 71, 80),
                    TextBlock.paragraph("Nội dung của điều hai, cũng đủ dài để thành một đoạn riêng.", 81, 140)),
                    true, null);

            List<ChunkCandidate> chunks = chunker.chunk(content, params);

            assertEquals(2, chunks.size());
            assertEquals("Điều 1", chunks.get(0).breadcrumb());
            assertEquals("Điều 2", chunks.get(1).breadcrumb());
        }

        @Test
        @DisplayName("Bảng dài chia theo HÀNG và lặp dòng tiêu đề ở mọi mảnh")
        void bangLapDongTieuDe() {
            List<List<String>> rows = new java.util.ArrayList<>();
            for (int i = 1; i <= 60; i++) {
                rows.add(List.of("Mục " + i, "Nội dung chi tiết của dòng thứ " + i,
                        "Ghi chú bổ sung cho dòng " + i));
            }
            ExtractedTable table = new ExtractedTable(
                    List.of("Số thứ tự", "Nội dung", "Ghi chú"), rows, "Bảng phụ cấp");

            ExtractedContent content = new ExtractedContent("x", List.of(
                    TextBlock.heading(2, "Phụ lục", 0, 10),
                    TextBlock.table(table, table.renderAll(), 11, 5000)), true, null);

            List<ChunkCandidate> chunks = chunker.chunk(content, params);

            assertTrue(chunks.size() > 1, "Bảng dài phải được chia nhỏ");
            for (ChunkCandidate c : chunks) {
                assertTrue(c.table());
                assertTrue(c.content().contains("Số thứ tự | Nội dung | Ghi chú"),
                        """
                        Thiếu dòng tiêu đề thì mảnh thứ hai trở đi là một khối số vô nghĩa —
                        người đọc không biết cột nào là cột nào.""");
                assertEquals(chunks.size(), c.tablePartsTotal());
            }
            assertEquals(1, chunks.get(0).tablePart());
        }

        @Test
        @DisplayName("Nội dung rỗng không sinh đoạn nào")
        void noiDungRong() {
            assertTrue(chunker.chunk(new ExtractedContent("", List.of(), true, null), params).isEmpty());
        }
    }
}
