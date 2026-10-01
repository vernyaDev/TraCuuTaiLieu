package vn.utc.tracuu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.access.ReadableDocuments;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.prompt.PromptNormalizer;
import vn.utc.tracuu.agent.reasoning.ComplexityClassifier;
import vn.utc.tracuu.ingestion.ChunkWriter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm phần logic thuần của tầng phân quyền, agent và khởi động. */
class CoreLogicTest {

    @Nested
    @DisplayName("Phạm vi quyền — luật R1")
    class Permissions {

        @Test
        @DisplayName("Đọc không lọc quyền BẮT BUỘC nêu lý do")
        void docKhongLocPhaiCoLyDo() {
            // Lý do trống là thứ khiến nhật ký kiểm toán trở nên vô dụng đúng lúc cần đến nó.
            assertThrows(IllegalArgumentException.class, () -> new PermissionScope.Unfiltered(""));
            assertThrows(IllegalArgumentException.class, () -> new PermissionScope.Unfiltered(null));
            assertDoesNotThrow(() -> new PermissionScope.Unfiltered("phan-loai-khoang-trong"));
        }

        @Test
        @DisplayName("Chỉ đường đọc-không-lọc mới ghi nhật ký kiểm toán")
        void chiDuongKhongLocMoiGhiKiemToan() {
            assertFalse(new PermissionScope.ForUser(1L).requiresAudit());
            assertFalse(new PermissionScope.ForEvaluation(1L, "run-1").requiresAudit());
            assertTrue(new PermissionScope.Unfiltered("lý do").requiresAudit());
        }

        @Test
        @DisplayName("Danh sách cho phép và danh sách chặn cho CÙNG một kết quả")
        void haiChieuLocTuongDuong() {
            // Cùng một tập được phép, biểu diễn hai cách. Bên gọi không cần biết cách nào đang
            // dùng — nhưng kết quả phải giống hệt nhau, nếu không thì chọn chiều lọc theo hiệu
            // năng sẽ âm thầm đổi cả kết quả.
            // Tài liệu ra đời SAU khi phạm vi được tính thì KHÔNG được đọc.
            //
            // Đây là lỗ hổng đo thật ngày 30/09: danh sách chặn là một ảnh chụp, nên tài liệu
            // mới không nằm trong đó, và câu hỏi "có bị chặn không" trả lời "không" — biến
            // từ-chối-mặc-định thành cho-phép-mặc-định cho đúng những tài liệu mới nhất. Một
            // tài khoản thường đã đọc trọn nội dung một tài liệu không có một dòng cấp quyền nào.
            ReadableDocuments denyCuTruoc = new ReadableDocuments.DenyList(Set.of(4L, 5L), 5, 5L);
            assertFalse(denyCuTruoc.permits(6L), "tài liệu nạp sau mốc kho phải bị từ chối");
            assertFalse(denyCuTruoc.permits(9999L), "id vượt mốc kho phải bị từ chối");
            assertTrue(denyCuTruoc.sqlPredicate("c").contains("document_id <= 5"),
                    "mệnh đề SQL phải mang cả mốc kho, nếu không tiền lọc vẫn hở");

            ReadableDocuments allow = new ReadableDocuments.AllowList(Set.of(1L, 2L, 3L));
            ReadableDocuments deny = new ReadableDocuments.DenyList(Set.of(4L, 5L), 5, 5L);

            for (long id = 1; id <= 3; id++) {
                assertTrue(allow.permits(id));
                assertTrue(deny.permits(id));
            }
            for (long id = 4; id <= 5; id++) {
                assertFalse(allow.permits(id));
                assertFalse(deny.permits(id));
            }
        }

        @Test
        @DisplayName("Tài khoản chưa được cấp quyền không đọc được gì")
        void chuaCapQuyenThiKhongDocDuocGi() {
            ReadableDocuments none = new ReadableDocuments.None();
            assertFalse(none.permits(1L));
            assertTrue(none.isEmpty());
            assertEquals("false", none.sqlPredicate("c"));
            assertFalse(none.needsIdParameter());
        }

        @Test
        @DisplayName("Mệnh đề SQL của hai chiều lọc đều cần đúng MỘT tham số")
        void menhDeSqlDungMotThamSo() {
            assertTrue(new ReadableDocuments.AllowList(Set.of(1L)).needsIdParameter());
            assertTrue(new ReadableDocuments.DenyList(Set.of(1L), 10, 10L).needsIdParameter());
            assertFalse(new ReadableDocuments.All().needsIdParameter());
        }
    }

    @Nested
    @DisplayName("Chuẩn hoá câu hỏi và nhận diện ý định")
    class Prompt {

        private final PromptNormalizer normalizer = new PromptNormalizer();

        @Test
        @DisplayName("Gỡ ký tự vô hình lọt vào khi dán từ Word hoặc trình duyệt")
        void goKyTuVoHinh() {
            String dirty = "nghỉ​ phép﻿ năm";
            assertEquals("nghỉ phép năm", normalizer.normalize(dirty));
        }

        @Test
        @DisplayName("Gộp khoảng trắng thừa")
        void gopKhoangTrang() {
            assertEquals("quy trình duyệt chi", normalizer.normalize("  quy   trình\n\nduyệt  chi  "));
        }

        @Test
        @DisplayName("Nhận diện lời chào, không đưa vào truy hồi")
        void nhanDienLoiChao() {
            assertEquals(AgentContext.Intent.CHAO_HOI, normalizer.detectIntent("xin chào"));
            assertEquals(AgentContext.Intent.CHAO_HOI, normalizer.detectIntent("cảm ơn bạn"));
        }

        @Test
        @DisplayName("Nghi ngờ thì coi là tra cứu — chặn nhầm câu hỏi thật tệ hơn chạy thừa")
        void nghiNgoThiCoiLaTraCuu() {
            assertEquals(AgentContext.Intent.TRA_CUU,
                    normalizer.detectIntent("chào hỏi khách hàng theo quy trình nào"));
        }

        @Test
        @DisplayName("Nhận diện câu ngoài phạm vi kho nội bộ")
        void nhanDienNgoaiPhamVi() {
            assertEquals(AgentContext.Intent.NGOAI_PHAM_VI,
                    normalizer.detectIntent("thời tiết hôm nay thế nào"));
        }
    }

    @Nested
    @DisplayName("Phân loại độ phức tạp")
    class Complexity {

        private final ComplexityClassifier classifier = new ComplexityClassifier();

        @Test
        @DisplayName("Câu đối chiếu luôn là câu phức tạp")
        void cauDoiChieuLaPhucTap() {
            assertEquals(AgentContext.Complexity.PHUC_TAP,
                    classifier.classify("so sánh chế độ nghỉ ốm của nhân viên chính thức và thử việc"));
        }

        @Test
        @DisplayName("Câu hỏi thẳng một việc là câu đơn giản")
        void cauThangLaDonGian() {
            assertEquals(AgentContext.Complexity.DON_GIAN,
                    classifier.classify("nghỉ phép năm được bao nhiêu ngày"));
        }

        @Test
        @DisplayName("Chữ 'và' một mình chưa đủ để thành câu phức tạp")
        void chuVaMotMinhChuaDu() {
            // "chế độ thai sản và ốm đau" là MỘT chủ đề, không phải hai việc gộp lại.
            assertEquals(AgentContext.Complexity.DON_GIAN,
                    classifier.classify("chế độ thai sản và ốm đau"));
        }

        @Test
        @DisplayName("Nhiều dấu hỏi là nhiều câu hỏi gộp làm một")
        void nhieuDauHoi() {
            assertEquals(AgentContext.Complexity.PHUC_TAP,
                    classifier.classify("ai duyệt đơn? mất bao lâu?"));
        }

        @Test
        @DisplayName("Câu rỗng không làm hỏng gì")
        void cauRong() {
            assertEquals(AgentContext.Complexity.DON_GIAN, classifier.classify(""));
            assertEquals(AgentContext.Complexity.DON_GIAN, classifier.classify(null));
        }
    }




    @Nested
    @DisplayName("Ghi vector xuống cơ sở dữ liệu")
    class VectorLiteral {

        @Test
        @DisplayName("Dạng chuỗi vector đúng cú pháp phần mở rộng")
        void dangChuoiDung() {
            assertEquals("[1.0,0.5,-0.25]",
                    ChunkWriter.toVectorLiteral(new float[]{1.0f, 0.5f, -0.25f}));
        }

        @Test
        @DisplayName("Vector rỗng vẫn ra chuỗi hợp lệ")
        void vectorRong() {
            assertEquals("[]", ChunkWriter.toVectorLiteral(new float[0]));
        }
    }
}
