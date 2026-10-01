package vn.utc.tracuu.agent.planner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.utc.tracuu.TestProperties;
import vn.utc.tracuu.agent.AgentContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm phần đọc danh sách truy vấn con.
 *
 * <p>Đặt cùng gói với lớp được kiểm là có chủ đích: phương thức đọc kết quả là chi tiết nội bộ,
 * và mở rộng phạm vi truy cập của nó chỉ để kiểm là biến một quyết định thiết kế thành một lỗ
 * hổng đóng gói.
 */
class QueryPlannerTest {

    private final QueryPlanner planner = new QueryPlanner(null, TestProperties.defaults());

    @Test
    @DisplayName("Đọc được nhiều kiểu đánh số vì mô hình không ổn định ở chỗ này")
    void docNhieuKieuDanhSo() {
        List<AgentContext.SubQuery> parsed = planner.parse("""
                1. Chế độ nghỉ ốm của nhân viên chính thức
                2) Chế độ nghỉ ốm của nhân viên thử việc
                - Mức hưởng trong hai trường hợp trên
                """, 4);

        assertEquals(3, parsed.size());
        assertEquals(1, parsed.get(0).ordinal());
        assertFalse(parsed.get(0).text().startsWith("1."),
                "Ký hiệu đánh số phải bị gỡ, không đem đi truy hồi");
    }

    @Test
    @DisplayName("Chỉ nhận phụ thuộc vào truy vấn ĐỨNG TRƯỚC")
    void chiNhanPhuThuocLui() {
        List<AgentContext.SubQuery> parsed = planner.parse("""
                1. Tìm điều khoản quy định mức phạt
                2. Mức phạt cụ thể là bao nhiêu [cần:1]
                3. Ai có thẩm quyền quyết định [cần:9]
                """, 4);

        assertEquals(3, parsed.size());
        assertNull(parsed.get(0).dependsOnOrdinal());
        assertEquals(1, parsed.get(1).dependsOnOrdinal());
        assertNull(parsed.get(2).dependsOnOrdinal(),
                "Phụ thuộc tiến hoặc trỏ vào số không tồn tại là vòng lặp — phải bỏ đi");
    }

    @Test
    @DisplayName("Tôn trọng giới hạn số truy vấn con")
    void tonTrongGioiHan() {
        // Mỗi truy vấn con là một lần truy hồi cộng thêm ngữ cảnh, nên con số này ăn thẳng
        // vào ngưỡng độ trễ của câu phức tạp.
        assertEquals(2, planner.parse("1. một câu hỏi\n2. hai câu hỏi\n3. ba\n4. bốn\n", 2).size());
    }

    @Test
    @DisplayName("Kết quả rỗng hoặc rác không làm hỏng bước")
    void ketQuaRacKhongLamHong() {
        assertTrue(planner.parse(null, 4).isEmpty());
        assertTrue(planner.parse("", 4).isEmpty());
        assertTrue(planner.parse("1. ab\n2. cd\n", 4).isEmpty(),
                "Dòng quá ngắn không phải một truy vấn con dùng được");
    }
}
