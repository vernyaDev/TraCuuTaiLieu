package vn.utc.tracuu.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Kho câu hỏi chuẩn.
 *
 * <p>Là interface CẤP CAO NHẤT, không lồng trong lớp khác — Spring Data chỉ dựng bean cho
 * interface cấp cao nhất, và interface lồng bị bỏ qua <strong>im lặng</strong> cho tới lúc
 * khởi động thất bại. Gom năm kho vào một tệp cho gọn là đúng loại tối ưu hoá đổi một chút
 * gọn gàng lấy một lỗi chỉ hiện lúc chạy.
 */
public interface GroundTruthQuestionRepository extends JpaRepository<GroundTruthQuestion, Long> {

    List<GroundTruthQuestion> findByQuestionSetVersionOrderByOrdinal(String version);

    @Query("select distinct q.questionSetVersion from GroundTruthQuestion q")
    List<String> findVersions();
}
