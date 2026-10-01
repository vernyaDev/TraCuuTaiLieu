package vn.utc.tracuu.agent.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.utc.tracuu.TestProperties;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.ingestion.VietnameseSegmenter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tầng chặn thứ hai.
 *
 * <p>Tầng một chặn khi <em>chưa có</em> vật liệu; tầng này chặn khi <em>có vật liệu nhưng câu
 * trả lời không dùng nó</em> — tức mô hình đã tự bịa. Đây là hai loại lỗi khác nhau, và gộp
 * làm một thì loại thứ hai không ai bắt.
 */
class GroundingCheckerTest {

    private GroundingChecker checker() {
        // Không gọi load(): bộ tách từ khi chưa nạp từ điển vẫn cắt thành âm tiết, và phép
        // chồng lấn từ vựng vẫn chạy đúng. Bài kiểm này không phụ thuộc chất lượng từ điển.
        return new GroundingChecker(
                new VietnameseSegmenter(TestProperties.defaults()), TestProperties.defaults());
    }

    private RetrievedChunk source(String content) {
        return new RetrievedChunk(1L, 1L, "Quy chế nhân sự", "Điều 12", content, 0,
                false, null, null, null, 0.9, 0.5, 1);
    }

    @Test
    @DisplayName("Câu trả lời bám nguồn thì qua được")
    void bamNguonThiQua() {
        var result = checker().check(
                "Người lao động được nghỉ hằng năm mười hai ngày làm việc trong điều kiện bình thường.",
                List.of(source("Người lao động làm việc đủ mười hai tháng được nghỉ hằng năm "
                        + "mười hai ngày làm việc trong điều kiện bình thường.")));

        assertTrue(result.grounded());
        assertTrue(result.supportedRatio() > 0.5);
    }

    @Test
    @DisplayName("Câu bịa hoàn toàn bị chặn")
    void cauBiaBiChan() {
        var result = checker().check(
                "Công ty cấp xe đưa đón miễn phí cho toàn bộ nhân viên khu vực ngoại thành "
                + "và hỗ trợ tiền thuê nhà hằng tháng theo vị trí công tác.",
                List.of(source("Người lao động được nghỉ hằng năm mười hai ngày làm việc.")));

        assertFalse(result.grounded());
        assertFalse(result.ungrounded().isEmpty());
    }

    @Test
    @DisplayName("Không có nguồn thì không thể coi là có căn cứ")
    void khongNguonThiKhongCoCanCu() {
        assertFalse(checker().check("một câu bất kỳ", List.of()).grounded());
        assertFalse(checker().check(null, List.of(source("nội dung"))).grounded());
        assertFalse(checker().check("  ", List.of(source("nội dung"))).grounded());
    }

    /**
     * Câu trả lời ngắn vẫn phải bị kiểm — chỉ là kiểm cả khối thay vì từng câu.
     *
     * <p>Bản trước của phép kiểm này khẳng định điều ngược lại ("câu ngắn thì không kết luận là
     * bịa") và vì thế <strong>bảo vệ một lỗ hổng</strong>: bất kỳ câu trả lời nào đủ ngắn đều đi
     * thẳng qua tầng chặn thứ hai mà không bị kiểm dòng nào.
     *
     * <p>Đo thật ngày 30/09: hỏi "Cách nấu phở bò ngon nhất là gì?" trên kho quy chế nội bộ, mô
     * hình trả "Không đủ căn cứ" — bốn từ — và hệ thống phát nó ra như một câu trả lời kèm tám
     * trích dẫn. Bốn trong năm câu hỏi hoàn toàn ngoài kho đều được trả lời thay vì bị từ chối.
     */
    @Test
    @DisplayName("Câu ngắn CÓ trùng từ với nguồn thì vẫn đạt")
    void cauNganTrungNguonThiDat() {
        var result = checker().check("Mười hai ngày làm việc.",
                List.of(source("Người lao động được nghỉ mười hai ngày làm việc mỗi năm.")));
        assertTrue(result.grounded(), "câu trả lời ngắn hợp lệ không được bị chối oan");
    }

    @Test
    @DisplayName("Câu ngắn KHÔNG trùng từ với nguồn thì không được coi là có căn cứ")
    void cauNganKhongTrungNguonThiKhongDat() {
        var result = checker().check("Không đủ căn cứ",
                List.of(source("Người lao động được nghỉ mười hai ngày làm việc mỗi năm.")));
        assertFalse(result.grounded(),
                "câu trả lời ngắn không gắn được về nguồn phải bị chặn, không được đi thẳng qua");
    }
}
