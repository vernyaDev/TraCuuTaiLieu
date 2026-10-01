package vn.utc.tracuu.agent.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.utc.tracuu.TestProperties;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm bất biến quan trọng nhất về mặt an toàn của cả hệ thống.
 *
 * <p>Người dùng cần phân biệt được vì sao bị từ chối, nhưng hệ thống không được tiết lộ rằng
 * có tài liệu phù hợp nằm ngoài quyền họ. Chỗ hoà giải là một lối nói rào đón — và nó
 * <strong>chỉ bảo vệ được khi hiện y hệt ở MỌI lần từ chối vì không đủ căn cứ</strong>.
 *
 * <p>Nếu câu "có thể bạn chưa được cấp quyền" chỉ xuất hiện khi thật sự có tài liệu bị chặn,
 * thì nó thôi là một khả năng và trở thành một lời khẳng định: người hỏi chỉ cần so hai lần từ
 * chối khác nhau là suy ra được tài liệu ấy tồn tại.
 *
 * <p>Bộ kiểm này tồn tại để một người sửa mã sau này, thấy thông điệp "chung chung quá" và muốn
 * làm nó "hữu ích hơn", sẽ gặp một lần đỏ chứ không lặng lẽ mở một kênh rò rỉ.
 */
class RefusalDeciderTest {

    private final RefusalDecider decider = new RefusalDecider(TestProperties.defaults());

    private AgentContext contextWith(List<RetrievedChunk> chunks, int permittedDocuments) {
        return AgentContext
                .start("câu hỏi thử", new PermissionScope.ForUser(1L), 1L, null, List.of())
                .withChunks(chunks, permittedDocuments, 10);
    }

    private RetrievedChunk chunk(double vectorScore) {
        return new RetrievedChunk(1L, 1L, "Tài liệu", "Điều 1", "nội dung", 0,
                false, null, null, null, vectorScore, 0.5, 1);
    }

    @Test
    @DisplayName("Kho trống và thiếu quyền phải cho RA cùng một thông điệp")
    void khoTrongVaThieuQuyenChungMotThongDiep() {
        // Không đọc được tài liệu nào: người dùng chưa được cấp quyền trên bất cứ thứ gì.
        Optional<AgentContext> thieuQuyen = decider.decide(contextWith(List.of(), 0));
        // Có quyền trên cả kho nhưng kho không có gì khớp.
        Optional<AgentContext> khoTrong = decider.decide(contextWith(List.of(), 500));

        assertTrue(thieuQuyen.isPresent());
        assertTrue(khoTrong.isPresent());

        assertEquals(
                thieuQuyen.get().refusal().message(),
                khoTrong.get().refusal().message(),
                """
                Hai ca này PHẢI cho cùng một câu. Khác nhau dù chỉ một chữ là đủ để người hỏi
                so hai lần từ chối và suy ra tài liệu nào tồn tại ngoài quyền của mình.""");
    }

    @Test
    @DisplayName("Lý do ghi nhật ký thì khác nhau — chỉ thông điệp mới giống")
    void lyDoGhiNhatKyVanPhanBiet() {
        AgentContext thieuQuyen = decider.decide(contextWith(List.of(), 0)).orElseThrow();
        AgentContext khoTrong = decider.decide(contextWith(List.of(), 500)).orElseThrow();

        // Quản trị CẦN phân biệt được hai ca để biết nên bổ sung tài liệu hay nên cấp quyền.
        assertNotEquals(thieuQuyen.refusal().reason(), khoTrong.refusal().reason());
        assertEquals("THIEU_QUYEN", thieuQuyen.refusal().reason());
        assertEquals("THIEU_TAI_LIEU", khoTrong.refusal().reason());
    }

    @Test
    @DisplayName("Thông điệp nêu ba khả năng, không khẳng định khả năng nào")
    void thongDiepNeuKhaNangChuKhongKhangDinh() {
        String message = RefusalDecider.THONG_DIEP_KHONG_DU_CAN_CU;

        assertTrue(message.contains("Có thể"),
                "Phải nói đây là các KHẢ NĂNG, không phải kết luận");
        assertTrue(message.contains("Kho chưa có tài liệu"));
        assertTrue(message.contains("ngoài phạm vi bạn được cấp quyền"));
        assertTrue(message.contains("cụ thể hơn"));

        assertFalse(message.contains("bạn không có quyền đọc tài liệu này"),
                "Khẳng định tồn tại tài liệu là đúng thứ bất biến này cấm");
    }

    @Test
    @DisplayName("Từ chối tầng hai dùng ĐÚNG thông điệp của tầng một")
    void tangHaiDungChungThongDiep() {
        AgentContext ctx = contextWith(List.of(chunk(0.9)), 100).withAnswer("một câu trả lời");
        AgentContext refused = decider.refuseUngrounded(ctx);

        assertEquals(RefusalDecider.THONG_DIEP_KHONG_DU_CAN_CU, refused.refusal().message(),
                "Hai câu khác nhau giữa hai tầng lại là hai bit thông tin rò ra ngoài");
        assertEquals(RefusalDecider.TANG_HAI, refused.refusal().stage(),
                "Giao diện cần biết tầng hai để XOÁ phần chữ đã hiện");
    }

    @Test
    @DisplayName("Từ chối xoá luôn phần câu trả lời đã có")
    void tuChoiXoaCauTraLoiDaCo() {
        AgentContext ctx = contextWith(List.of(chunk(0.9)), 100)
                .withAnswer("nội dung đã sinh ra từ tài liệu");

        AgentContext refused = decider.refuseUngrounded(ctx);

        assertNull(refused.answer(),
                """
                Một lượt hoặc trả lời hoặc từ chối. Giữ lại phần chữ đã sinh là mở đường cho nó
                rò ra ngoài qua một đường khác, ví dụ qua lịch sử hội thoại.""");
    }

    @Test
    @DisplayName("Đủ căn cứ thì KHÔNG từ chối")
    void duCanCuThiDiTiep() {
        assertTrue(decider.decide(contextWith(List.of(chunk(0.82)), 100)).isEmpty());
    }

    @Test
    @DisplayName("Căn cứ quá mỏng thì từ chối ở tầng một")
    void canCuMongThiTuChoiSom() {
        Optional<AgentContext> refused = decider.decide(contextWith(List.of(chunk(0.05)), 100));

        assertTrue(refused.isPresent());
        assertEquals(RefusalDecider.TANG_MOT, refused.get().refusal().stage(),
                "Tầng một không gọi mô hình nên phải nhanh hơn hẳn đường trả lời");
    }

    @Test
    @DisplayName("Câu ngoài phạm vi nói thẳng được, vì ca này không lộ gì")
    void ngoaiPhamViNoiThang() {
        AgentContext ctx = AgentContext
                .start("tỷ giá hôm nay", new PermissionScope.ForUser(1L), 1L, null, List.of())
                .withNormalized("tỷ giá hôm nay", AgentContext.Intent.NGOAI_PHAM_VI);

        AgentContext refused = decider.decide(ctx).orElseThrow();

        assertEquals(RefusalDecider.THONG_DIEP_NGOAI_PHAM_VI, refused.refusal().message());
        assertNotEquals(RefusalDecider.THONG_DIEP_KHONG_DU_CAN_CU, refused.refusal().message(),
                "Ca này không tiết lộ gì về nội dung kho nên được phép nói rõ");
    }

    /**
     * Lý do máy-đọc-được KHÔNG được có mặt trong sự kiện gửi ra ngoài.
     *
     * <p>Bất biến "kho trống và thiếu quyền cho ra cùng một câu chữ" chỉ có nghĩa khi TOÀN BỘ
     * phản hồi giống nhau, không riêng phần câu chữ. Bản trước gửi kèm một trường lý do phân
     * biệt đúng hai ca đó; giao diện không hiển thị nó, nhưng bất kỳ ai mở bảng mạng của trình
     * duyệt đều đọc được — và khi đó công sức dùng chung một câu chữ thành vô nghĩa.
     *
     * <p>Kiểm bằng cách soi các thành phần của kiểu bản ghi: một trường mới tên "reason" thêm
     * vào sau này sẽ làm phép kiểm này đỏ, kể cả khi không ai nhớ vì sao nó từng bị bỏ đi.
     */
    @Test
    @DisplayName("Sự kiện từ chối gửi ra ngoài không mang lý do máy-đọc-được")
    void suKienTuChoiKhongMangLyDo() {
        var truong = java.util.Arrays
                .stream(vn.utc.tracuu.agent.AgentEvent.Refusal.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        assertFalse(truong.contains("reason"),
                "Sự kiện từ chối đang mang trường lý do ra ngoài: " + truong);
        assertTrue(truong.contains("message"), "vẫn phải có câu chữ cho người dùng");
    }
}
