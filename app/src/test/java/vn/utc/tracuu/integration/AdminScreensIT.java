package vn.utc.tracuu.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.utc.tracuu.access.Role;
import vn.utc.tracuu.platform.security.AppUserDetails;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Mở lần lượt MỌI màn quản trị qua đúng tầng HTTP mà giao diện đi, và đòi không màn nào trả 500.
 *
 * <p><strong>Vì sao bộ kiểm này tồn tại.</strong> Ngày 30/09 màn <em>Tài khoản</em> trả 500 cho
 * mọi lần mở: danh sách người dùng ánh xạ sang bản hiển thị sau khi phiên Hibernate đã đóng, nên
 * chạm vào quan hệ phòng ban (LAZY) là nổ {@code LazyInitializationException}. Lỗi đó sống sót
 * qua 51 phép kiểm đơn vị và 14 phép kiểm tích hợp, vì <strong>không phép kiểm nào đi qua tầng
 * HTTP</strong> — chúng gọi thẳng lớp Java, nơi phiên vẫn còn mở. Nó chỉ lộ ra khi mở trình
 * duyệt thật.
 *
 * <p>Vì thế đây cố ý là phép kiểm <em>nông mà rộng</em>: không khẳng định nội dung trả về, chỉ
 * khẳng định mỗi màn mở được. Đó đúng là lớp lỗi đã lọt — một màn hỏng hoàn toàn, không phải một
 * con số sai trong màn chạy được.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestDoubles.class)
@TestPropertySource(properties = {
        "spring.datasource.url=${tracuu.it.db:jdbc:postgresql://localhost:5432/tracuu}?sslmode=disable",
        "spring.datasource.username=${tracuu.it.user:tracuu}",
        "spring.datasource.password=${tracuu.it.password:tracuu_dev}",
        "spring.data.redis.host=${tracuu.it.redis:localhost}",
        "tracuu.storage.root=./target/it-documents",
        "spring.liquibase.enabled=true",
        "tracuu.llm.model=stub-model",
})
@EnabledIfSystemProperty(named = "tracuu.it.enabled", matches = "true",
        disabledReason = "Bỏ qua kiểm thử tích hợp: chưa bật -Dtracuu.it.enabled=true")
class AdminScreensIT {

    @Autowired MockMvc mvc;

    /** Quản trị giả lập: không cần bản ghi thật trong bảng, chỉ cần danh tính để qua lớp quyền. */
    private AppUserDetails quanTri() {
        return new AppUserDetails(1L, "it-quantri", "Quản trị kiểm thử", "{noop}x",
                Role.QUAN_TRI, 3, true, false, Set.of());
    }

    /** Mọi đường GET mà giao diện gọi khi người quản trị bấm qua từng mục trên thanh điều hướng. */
    private static List<String> duongDan() {
        return List.of(
                "/api/auth/me",
                "/api/admin/users?page=0&size=20",
                "/api/admin/users?q=quan&page=0&size=20",
                "/api/admin/departments",
                "/api/admin/documents?page=0&size=20",
                "/api/admin/documents?q=quy&page=0&size=20",
                "/api/admin/documents/categories",
                "/api/admin/overlaps?page=0&size=20",
                "/api/admin/gaps?page=0&size=20");
    }

    @Test
    @DisplayName("Mọi màn quản trị mở được, không màn nào trả lỗi hệ thống")
    void moiManQuanTriMoDuoc() throws Exception {
        var hong = new java.util.ArrayList<String>();
        for (String duong : duongDan()) {
            MvcResult kq = mvc.perform(get(duong).with(user(quanTri()))).andReturn();
            int ma = kq.getResponse().getStatus();
            if (ma >= 500) {
                Exception e = kq.getResolvedException();
                hong.add(duong + " → " + ma
                        + (e == null ? "" : " (" + e.getClass().getSimpleName() + ": "
                                             + String.valueOf(e.getMessage()).split("\n")[0] + ")"));
            }
        }
        assertTrue(hong.isEmpty(), "Màn quản trị trả lỗi hệ thống:\n  " + String.join("\n  ", hong));
    }

    /**
     * Danh sách tài khoản phải trả được kèm phòng ban.
     *
     * <p>Tách riêng khỏi phép quét ở trên vì đây là đường đã hỏng thật: quét rộng chỉ nói
     * "không 500", còn phép kiểm này nói rõ trường {@code departments} có mặt — tức là quan
     * hệ đã thực sự được nạp chứ không phải được bỏ trống cho qua chuyện.
     */
    /**
     * Yêu cầu sai hình dạng phải trả 400, KHÔNG phải 500.
     *
     * <p>Khác biệt không chỉ là con số. Mã 500 nói với người gọi rằng máy chủ hỏng trong khi lỗi
     * là ở yêu cầu của họ, và nó còn để lại một vết ngoại lệ đầy đủ ở mức ERROR trong nhật ký —
     * nghĩa là mỗi đường dẫn gõ sai đều sinh một dòng đỏ, và lỗi thật thì chìm giữa chúng. Đo
     * ngày 30/09: cả bốn dạng dưới đây đều trả 500.
     */
    @Test
    @DisplayName("Yêu cầu sai hình dạng trả 400, không phải lỗi hệ thống")
    void yeuCauSaiTra400() throws Exception {
        var sai = List.of(
                "/api/admin/documents?page=abc&size=20",          // tham số sai kiểu
                "/api/admin/documents/khong-phai-so",             // biến đường dẫn sai kiểu
                "/api/admin/documents/1/sensitivity-impact",      // thiếu tham số bắt buộc
                "/api/admin/permissions/explain?userId=1");       // thiếu documentId
        var hong = new java.util.ArrayList<String>();
        for (String duong : sai) {
            int ma = mvc.perform(get(duong).with(user(quanTri()))).andReturn().getResponse().getStatus();
            if (ma >= 500) hong.add(duong + " → " + ma);
        }
        assertTrue(hong.isEmpty(), "Yêu cầu sai bị báo là lỗi hệ thống:\n  " + String.join("\n  ", hong));
    }

    /**
     * Số trang âm phải được kẹp, không được thành lỗi hệ thống.
     *
     * <p>{@code PageRequest.of} ném {@link IllegalArgumentException} với số trang âm, và ngoại lệ
     * đó rơi vào lưới cuối thành mã 500. Kích thước trang thì đã được kẹp sẵn ở cả bốn màn danh
     * sách — kẹp một nửa tham số rồi để nửa kia làm sập là chỗ lỗi nảy ra.
     */
    @Test
    @DisplayName("Số trang âm và kích thước trang quá lớn đều được kẹp")
    void thamSoPhanTrangNgoaiKhoangDuocKep() throws Exception {
        for (String duong : List.of(
                "/api/admin/documents?page=-5&size=20",
                "/api/admin/documents?page=0&size=999999",
                "/api/admin/users?page=-1&size=0",
                "/api/admin/gaps?page=-3&size=-3")) {
            int ma = mvc.perform(get(duong).with(user(quanTri()))).andReturn().getResponse().getStatus();
            assertTrue(ma < 500, duong + " → " + ma);
        }
    }

    @Test
    @DisplayName("Danh sách tài khoản trả được kèm phòng ban, không chạm quan hệ lười ngoài phiên")
    void danhSachTaiKhoanCoPhongBan() throws Exception {
        String than = mvc.perform(get("/api/admin/users?page=0&size=20").with(user(quanTri())))
                .andReturn().getResponse().getContentAsString();
        assertTrue(than.contains("\"departments\""),
                "Phản hồi thiếu trường departments: " + than.substring(0, Math.min(300, than.length())));
    }
}
