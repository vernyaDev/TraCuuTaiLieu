package vn.utc.tracuu.platform.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Dựng tham số phân trang từ giá trị người gọi gửi lên — <strong>đã kẹp trong khoảng hợp lệ</strong>.
 *
 * <p>Lớp này tồn tại vì {@code PageRequest.of} ném {@link IllegalArgumentException} khi số trang
 * âm, và ngoại lệ đó rơi vào lưới cuối của {@link GlobalExceptionHandler} thành mã 500. Đo ngày
 * 30/09: {@code ?page=-5} trả về "hệ thống gặp sự cố" — nói với người gọi rằng máy chủ hỏng vì
 * họ gõ một số âm.
 *
 * <p><strong>Kẹp chứ không báo lỗi</strong>, vì đó là điều đã làm sẵn với {@code size} ở cả bốn
 * màn danh sách ({@code Math.min(size, 100)}) — kẹp một nửa tham số rồi ném ngoại lệ với nửa kia
 * là bất nhất, và sự bất nhất đó chính là chỗ lỗi nảy ra. Giới hạn trên của {@code size} giữ
 * nguyên 100: nó bảo vệ bộ nhớ máy chủ khỏi một yêu cầu xin cả kho về trong một lần.
 */
public final class Trang {

    /** Số bản ghi tối đa một trang. Người gọi xin nhiều hơn thì nhận đúng ngần này. */
    public static final int TOI_DA = 100;

    private Trang() {}

    public static PageRequest cua(int page, int size) {
        return PageRequest.of(Math.max(0, page), kepSize(size));
    }

    public static PageRequest cua(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), kepSize(size), sort);
    }

    private static int kepSize(int size) {
        return Math.min(Math.max(1, size), TOI_DA);
    }
}
