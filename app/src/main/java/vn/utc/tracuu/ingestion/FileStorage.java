package vn.utc.tracuu.ingestion;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Lưu tệp gốc trên đĩa.
 *
 * <p>Tệp gốc phải giữ lại vì trích dẫn cho phép mở bản gốc, và vì việc đổi chiến lược tách đoạn
 * đòi bóc lại từ đầu. Đường dẫn sinh ra <strong>không dùng tên tệp người dùng đặt</strong>: tên
 * ấy có thể trùng, có thể chứa ký tự đường dẫn, và trên một số hệ tệp còn phân biệt hoa thường
 * khác nhau. Tên hiển thị vẫn giữ nguyên trong cơ sở dữ liệu.
 */
@Component
public class FileStorage {

    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    private final TracuuProperties props;
    private Path root;

    public FileStorage(TracuuProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        root = Path.of(props.storage().root()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            Path probe = root.resolve(".ghi-thu");
            Files.writeString(probe, "x");
            Files.delete(probe);
            log.info("Kho tệp gốc: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("""

                    ══ Không khởi động được ══
                    Không ghi được vào thư mục kho tệp: %s
                      Tạo thư mục và cấp quyền ghi, hoặc sửa tracuu.storage.root.
                      Chi tiết: %s
                    """.formatted(root, e.getMessage()), e);
        }
    }

    /** Kết quả lưu: đường dẫn tương đối và mã băm nội dung để phát hiện tải trùng. */
    public record Stored(String relativePath, String contentHash, long sizeBytes, String extension) {}

    public Stored store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "tai-lieu" : file.getOriginalFilename();
        String ext = extensionOf(original);

        if (!props.storage().accepts(ext)) {
            throw new BusinessException(ErrorCode.DINH_DANG_KHONG_HO_TRO, "file",
                    "Đuôi tệp không nằm trong danh sách nhận: " + ext);
        }
        if (file.getSize() > props.storage().maxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.TEP_QUA_LON, "file",
                    "Tệp " + file.getSize() + " byte, ngưỡng " + props.storage().maxFileSizeBytes());
        }
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.TEP_HONG, "file", "Tệp rỗng");
        }

        LocalDate today = LocalDate.now();
        Path dir = root.resolve(String.valueOf(today.getYear()))
                       .resolve(String.format("%02d", today.getMonthValue()));
        String name = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(name);

        try {
            Files.createDirectories(dir);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream();
                 DigestInputStream digesting = new DigestInputStream(in, digest)) {
                Files.copy(digesting, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String hash = HexFormat.of().formatHex(digest.digest());
            return new Stored(root.relativize(target).toString().replace('\\', '/'),
                    hash, Files.size(target), ext);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.LOI_HE_THONG, null,
                    "Không lưu được tệp: " + e.getMessage());
        }
    }

    /**
     * Đường dẫn tuyệt đối của một tệp đã lưu.
     *
     * <p>Chuẩn hoá rồi kiểm nằm trong gốc: đường dẫn lấy từ cơ sở dữ liệu vẫn phải kiểm, vì một
     * giá trị hỏng ở đó đủ để biến chức năng tải tệp thành chức năng đọc bất kỳ tệp nào trên máy chủ.
     */
    public Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new BusinessException(ErrorCode.KHONG_TIM_THAY, null,
                    "Đường dẫn nằm ngoài kho tệp: " + relativePath);
        }
        return p;
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("Không xoá được tệp {}: {}", relativePath, e.getMessage());
        }
    }

    public static String extensionOf(String filename) {
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
    }
}
