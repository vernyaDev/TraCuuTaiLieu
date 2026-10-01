package vn.utc.tracuu.platform.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm phép so phiên bản của phép kiểm khởi động số 2.
 *
 * <p>Phép so này quyết định hệ thống có khởi động hay không, và nó canh đúng một thứ: phần mở
 * rộng vector có đủ mới để <strong>quét chỉ mục lặp</strong> không. Thiếu tính năng ấy thì lọc
 * quyền trước xếp hạng làm tụt độ phủ một cách âm thầm — hệ thống vẫn trả lời, chỉ là bỏ sót
 * tài liệu đúng, và không có dấu hiệu nào để nhận ra.
 *
 * <p>So sai theo chiều "dễ dãi" ở đây nguy hiểm hơn so sai theo chiều "khắt khe": cho qua một
 * bản quá cũ là bật đèn xanh cho đúng thứ phép kiểm này sinh ra để chặn.
 */
class StartupChecksTest {

    @Test
    @DisplayName("Bản cũ hơn ngưỡng bị chặn")
    void banCuBiChan() {
        assertTrue(StartupChecks.compareVersions("0.7.4", "0.8.0") < 0);
        assertTrue(StartupChecks.compareVersions("0.5.0", "0.8.0") < 0);
    }

    @Test
    @DisplayName("Bằng hoặc mới hơn thì cho qua")
    void banDuMoiChoQua() {
        assertEquals(0, StartupChecks.compareVersions("0.8.0", "0.8.0"));
        assertTrue(StartupChecks.compareVersions("0.8.1", "0.8.0") > 0);
        assertTrue(StartupChecks.compareVersions("1.0.0", "0.8.0") > 0);
        assertTrue(StartupChecks.compareVersions("0.10.0", "0.8.0") > 0,
                "So theo số chứ không so theo chuỗi: 0.10 mới hơn 0.8");
    }

    @Test
    @DisplayName("Hậu tố và thiếu thành phần không làm hỏng phép so")
    void dangChuoiLaKhongLamHong() {
        assertEquals(0, StartupChecks.compareVersions("0.8.0-beta", "0.8.0"));
        assertEquals(0, StartupChecks.compareVersions("0.8", "0.8.0"));
        assertTrue(StartupChecks.compareVersions("0.7", "0.8.0") < 0);
    }

    /**
     * Mô hình nạp MỘT PHẦN lên GPU phải bị coi là không đạt.
     *
     * <p>Bản trước của phép kiểm khởi động số 4 chỉ hỏi "có byte nào trên GPU không", nên một mô
     * hình tràn sang CPU vẫn qua cửa — đúng cái ca mà phép kiểm sinh ra để chặn. Đo ngày 30/09:
     * qwen2.5:7b trên GPU 6 GB nằm 84% trên GPU, phép kiểm in ra "84%" rồi báo ĐẠT, và chữ đầu ở
     * 5 câu hỏi đồng thời tăng từ 3,8 giây lên 12,0 giây.
     */
    @Test
    @DisplayName("Mô hình chỉ nạp một phần lên GPU thì tỉ lệ phải nhỏ hơn sàn")
    void napMotPhanLenGpuThiDuoiSan() {
        var motPhan = new vn.utc.tracuu.platform.llm.OllamaClient.VramStatus(
                true, 4_069L * 1024 * 1024, 4_828L * 1024 * 1024);
        assertTrue(motPhan.onGpu(), "vẫn có byte trên GPU — đây chính là lý do phép cũ cho qua");
        assertTrue(motPhan.gpuRatio() < 1.0, "nhưng tỉ lệ phải phản ánh phần tràn sang CPU");
        assertEquals(84, Math.round(motPhan.gpuRatio() * 100));

        var tron = new vn.utc.tracuu.platform.llm.OllamaClient.VramStatus(
                true, 2_059L * 1024 * 1024, 2_059L * 1024 * 1024);
        assertEquals(1.0, tron.gpuRatio(), 0.0001, "mô hình nằm trọn GPU phải đạt sàn 1.0");
    }
}
