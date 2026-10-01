package vn.utc.tracuu.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IngestionItemRepository extends JpaRepository<IngestionItem, Long> {
    List<IngestionItem> findByBatchIdOrderById(Long batchId);
    long countByBatchIdAndStatus(Long batchId, IngestionStatus status);

    /**
     * Mục nạp còn dang dở — dùng để nhặt lại việc sau một lần khởi động lại.
     *
     * <p>Không có truy vấn này thì mọi tệp đang trong hàng đợi lúc máy chủ dừng sẽ nằm lại ở
     * {@code CHO} mãi mãi: bể luồng nằm trong bộ nhớ tiến trình, khởi động lại là mất sạch, và
     * không có gì đi tìm lại chúng.
     */
    List<IngestionItem> findByStatusIn(java.util.Collection<IngestionStatus> statuses);
}
