package vn.utc.tracuu.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.utc.tracuu.knowledge.*;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Điều phối việc nạp tài liệu theo đợt, chạy nền.
 *
 * <p>Nạp là thao tác <strong>không đồng bộ</strong>: gọi xong trả mã theo dõi ngay, tiến độ đọc
 * qua đường riêng. {@code AC-007} của US-009 đòi việc xử lý không phụ thuộc người dùng còn mở
 * màn hình — đóng tab giữa chừng thì việc nạp vẫn chạy tiếp, và đây là lý do tiến độ phải nằm
 * trong cơ sở dữ liệu chứ không nằm trong bộ nhớ của một phiên.
 *
 * <p>Lỗi của một tệp <strong>không làm hỏng các tệp khác cùng đợt</strong>. Một đợt 50 tệp mà
 * tệp thứ ba hỏng thì 47 tệp còn lại vẫn phải vào kho; dừng cả đợt là biến một lỗi nhỏ thành
 * một buổi làm lại.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    /**
     * Tham chiếu tới CHÍNH bean này qua bộ chứa, để lời gọi đi qua proxy giao dịch.
     *
     * <p>Gọi thẳng {@code processOne(...)} trong cùng lớp là một lời gọi Java thường: nó KHÔNG
     * đi qua proxy, nên {@code @Transactional(REQUIRES_NEW)} trên phương thức đó không có tác
     * dụng nào. Chú thích cũ khẳng định "giao dịch RIÊNG cho từng tệp — đây là điều làm 'lỗi một
     * tệp không hỏng cả đợt' thành sự thật chứ không phải lời hứa"; thực tế nó vẫn chỉ là lời
     * hứa, vì không có giao dịch nào được mở. Đây đúng loại lỗi mà cả người lẫn máy đều tin là
     * đã được chặn.
     */
    private final org.springframework.beans.factory.ObjectProvider<IngestionService> chinhNo;

    private final FileStorage storage;
    private final IngestionPipeline pipeline;
    private final DocumentRepository documents;
    private final ChunkSetRepository chunkSets;
    private final IngestionBatchRepository batches;
    private final IngestionItemRepository items;
    private final OverlapDetector overlapDetector;
    private final MetadataSuggester metadataSuggester;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final vn.utc.tracuu.platform.config.TracuuProperties props;
    private final ThreadPoolTaskExecutor executor;

    /** Đợt đã bị người dùng huỷ — các tệp chưa chạy sẽ bỏ qua. */
    private final Set<Long> cancelled = ConcurrentHashMap.newKeySet();

    public IngestionService(FileStorage storage,
                            IngestionPipeline pipeline,
                            DocumentRepository documents,
                            ChunkSetRepository chunkSets,
                            IngestionBatchRepository batches,
                            IngestionItemRepository items,
                            OverlapDetector overlapDetector,
                            MetadataSuggester metadataSuggester,
                            com.fasterxml.jackson.databind.ObjectMapper json,
                            vn.utc.tracuu.platform.config.TracuuProperties props,
                            @Qualifier("ingestionExecutor") ThreadPoolTaskExecutor executor,
                            org.springframework.beans.factory.ObjectProvider<IngestionService> chinhNo) {
        this.storage = storage;
        this.pipeline = pipeline;
        this.documents = documents;
        this.chunkSets = chunkSets;
        this.batches = batches;
        this.items = items;
        this.overlapDetector = overlapDetector;
        this.metadataSuggester = metadataSuggester;
        this.json = json;
        this.props = props;
        this.executor = executor;
        this.chinhNo = chinhNo;
    }

    /**
     * Nhận một đợt tệp và trả mã theo dõi <strong>ngay lập tức</strong>.
     *
     * <p>Tệp được ghi xuống đĩa đồng bộ (nhanh, và phải xong trước khi phản hồi vì luồng tải lên
     * đóng sau đó), còn việc bóc chữ và sinh vector thì chạy nền.
     */
    @Transactional
    public UUID startBatch(List<MultipartFile> files, long userId) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "files", "Không có tệp nào");
        }

        ChunkSet activeSet = chunkSets.findByActiveTrue().orElseThrow(() ->
                new BusinessException(ErrorCode.LOI_HE_THONG, null,
                        "Chưa có tập đoạn nào đang hoạt động"));

        IngestionBatch batch = new IngestionBatch();
        batch.setCreatedBy(userId);
        batch.setTotalFiles(files.size());
        batches.save(batch);

        List<Long> itemIds = new ArrayList<>();
        for (MultipartFile file : files) {
            IngestionItem item = new IngestionItem();
            item.setBatchId(batch.getId());
            item.setFilename(file.getOriginalFilename() == null ? "tai-lieu" : file.getOriginalFilename());

            try {
                FileStorage.Stored stored = storage.store(file);

                // Trùng nội dung: không nạp lại, nhưng cũng KHÔNG coi là lỗi của người dùng —
                // họ cần biết tài liệu đã có, không cần biết mình làm sai.
                var existing = documents.findByContentHash(stored.contentHash());
                if (existing.isPresent()) {
                    storage.delete(stored.relativePath());
                    item.setStatus(IngestionStatus.XONG);
                    item.setDocumentId(existing.get().getId());
                    item.setStage("Đã có trong kho");
                    item.setFinishedAt(Instant.now());
                    items.save(item);
                    continue;
                }

                Document doc = new Document();
                doc.setTitle(stripExtension(item.getFilename()));
                doc.setOriginalFilename(item.getFilename());
                doc.setStoragePath(stored.relativePath());
                doc.setContentHash(stored.contentHash());
                doc.setFileSizeBytes(stored.sizeBytes());
                doc.setUploadedBy(userId);
                doc.setStatus(DocumentStatus.DANG_XU_LY);
                documents.save(doc);

                item.setDocumentId(doc.getId());
                item.setStatus(IngestionStatus.CHO);
                items.save(item);
                itemIds.add(item.getId());

            } catch (BusinessException e) {
                item.setStatus(IngestionStatus.LOI);
                item.setErrorCode(e.code().name());
                item.setErrorReason(e.code().message());
                item.setFinishedAt(Instant.now());
                items.save(item);
            }
        }

        long batchId = batch.getId();
        long setId = activeSet.getId();
        giaoViecSauKhiCommit(itemIds, batchId, setId);

        log.info("Nhận đợt nạp {}: {} tệp, {} tệp vào hàng đợi",
                batch.getTrackingId(), files.size(), itemIds.size());
        return batch.getTrackingId();
    }

    /**
     * Nhặt lại việc nạp còn dang dở khi máy chủ khởi động.
     *
     * <p>Bể luồng nạp nằm trong bộ nhớ của tiến trình. Máy chủ dừng giữa một đợt nạp — do khởi
     * động lại, do triển khai bản mới, do máy mất điện — thì mọi tệp còn trong hàng đợi nằm lại
     * ở {@code CHO} và không bao giờ có ai đi tìm chúng nữa. Tài liệu kẹt ở {@code DANG_XU_LY},
     * đợt nạp kẹt ở {@code DANG_CHAY}, và người nạp không nhận được tín hiệu nào.
     *
     * <p>Chạy sau khi ứng dụng sẵn sàng, không phải trong lúc dựng bean: ở thời điểm dựng bean
     * thì bể luồng và kết nối cơ sở dữ liệu chưa chắc đã sẵn sàng.
     */
    @org.springframework.context.event.EventListener(
            org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void nhatLaiViecDangDo() {
        List<IngestionItem> conDangDo;
        try {
            conDangDo = items.findByStatusIn(
                    List.of(IngestionStatus.CHO, IngestionStatus.DANG_CHAY));
        } catch (RuntimeException e) {
            log.error("Không đọc được danh sách việc nạp dang dở", e);
            return;
        }
        if (conDangDo.isEmpty()) return;

        long setId = chunkSets.findByActiveTrue().map(ChunkSet::getId).orElse(-1L);
        if (setId < 0) {
            log.error("Có {} tệp nạp dang dở nhưng chưa có tập đoạn hoạt động — bỏ qua.",
                    conDangDo.size());
            return;
        }

        log.warn("Nhặt lại {} tệp nạp còn dang dở từ lần chạy trước.", conDangDo.size());
        for (IngestionItem item : conDangDo) {
            long id = item.getId();
            long batchId = item.getBatchId();
            executor.submit(() -> chinhNo.getObject().processOne(id, batchId, setId));
        }
    }

    /**
     * Đưa việc vào bể luồng <strong>sau khi giao dịch đã commit</strong>.
     *
     * <p>Bản trước gọi {@code executor.submit} ngay trong thân {@code startBatch}, tức là trong
     * lúc giao dịch còn đang mở. Luồng nền chạy ở một kết nối khác nên nó có thể tìm mục nạp
     * <em>trước khi dòng đó tồn tại</em>, không thấy gì, rồi {@code return} im lặng.
     *
     * <p>Hậu quả tái lập được ngày 30/09: tài liệu kẹt vĩnh viễn ở {@code DANG_XU_LY} với 0
     * đoạn, mục nạp kẹt ở {@code CHO}, đợt nạp kẹt ở {@code DANG_CHAY}, <strong>không một
     * thông báo lỗi và không một dòng nhật ký nào</strong>. Người nạp nhìn màn hình thấy "đang
     * xử lý" cho tới hết ngày. Đây là cuộc đua nên nó không xảy ra mọi lần — 919 tệp trước đó
     * đều qua được, và chính điều đó làm nó khó thấy.
     */
    private void giaoViecSauKhiCommit(List<Long> itemIds, long batchId, long setId) {
        if (itemIds.isEmpty()) return;

        Runnable giao = () -> itemIds.forEach(
                id -> executor.submit(() -> chinhNo.getObject().processOne(id, batchId, setId)));

        if (org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronization() {
                                @Override public void afterCommit() { giao.run(); }
                            });
        } else {
            // Không có giao dịch nào đang mở thì không có gì phải chờ.
            giao.run();
        }
    }

    /**
     * Xử lý một tệp. Giao dịch RIÊNG cho từng tệp — đây là điều làm "lỗi một tệp không hỏng cả
     * đợt" thành sự thật chứ không phải lời hứa.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(long itemId, long batchId, long chunkSetId) {
        IngestionItem item = items.findById(itemId).orElse(null);
        if (item == null) {
            // Im lặng ở đây là cách một tài liệu biến mất mà không ai biết.
            log.error("Không tìm thấy mục nạp {} của đợt {} — tệp này sẽ kẹt ở trạng "
                    + "thái đang xử lý. Đây là lỗi hệ thống, không phải lỗi của tệp.",
                    itemId, batchId);
            return;
        }

        if (cancelled.contains(batchId)) {
            item.setStatus(IngestionStatus.DA_HUY);
            item.setFinishedAt(Instant.now());
            items.save(item);
            return;
        }

        Document doc = documents.findById(item.getDocumentId()).orElse(null);
        if (doc == null) return;

        item.setStatus(IngestionStatus.DANG_CHAY);
        item.setStartedAt(Instant.now());
        items.save(item);

        try {
            ChunkSet set = chunkSets.findById(chunkSetId).orElseThrow();
            Path file = storage.resolve(doc.getStoragePath());

            IngestionPipeline.Result result = pipeline.run(
                    file, doc.getOriginalFilename(), doc.getId(), set,
                    stage -> updateStage(itemId, stage));

            applyMetadataSuggestion(doc, result, itemId);

            // Bật cờ xác nhận thì tài liệu dừng ở "chờ xác nhận" và CHƯA tham gia truy hồi.
            if (props.ingestion().requireMetadataConfirmation()) {
                doc.setStatus(DocumentStatus.CHO_XAC_NHAN);
                doc.setErrorReason(null);
            } else {
                pipeline.markIndexed(doc);
            }
            documents.save(doc);

            // Dò chồng lấn SAU khi đã lập chỉ mục: cần vector của chính tài liệu này để so.
            overlapDetector.detectFor(doc, chunkSetId);

            item.setStatus(IngestionStatus.XONG);
            item.setStage("Xong");

        } catch (ExtractionException e) {
            fail(item, doc, e.code(), e.code().message(), e.getMessage());
        } catch (Exception e) {
            log.error("Nạp tệp {} thất bại", item.getFilename(), e);
            fail(item, doc, ErrorCode.LOI_HE_THONG, ErrorCode.LOI_HE_THONG.message(), e.getMessage());
        } finally {
            item.setFinishedAt(Instant.now());
            items.save(item);
            documents.save(doc);
            closeBatchIfDone(batchId);
        }
    }

    /**
     * Ghi metadata hệ thống đề xuất và áp những giá trị ĐÁNG TIN.
     *
     * <p>"Đáng tin" nghĩa là có căn cứ đã đối chiếu thấy trong chính tài liệu. Giá trị không
     * đối chiếu được vẫn được LƯU để quản trị xem, nhưng KHÔNG tự áp lên tài liệu — một ngày
     * hiệu lực bịa mà tự động áp sẽ làm mọi cảnh báo hết hiệu lực về sau tính trên con số sai,
     * và không ai biết để truy lại.
     *
     * <p>Riêng nhãn mật thì <strong>không bao giờ tự áp</strong>, kể cả khi có căn cứ: nâng
     * nhãn mật lấy quyền đọc của người khác, và đó là quyết định của người, không phải của một
     * phép dò từ khoá.
     */
    private void applyMetadataSuggestion(Document doc, IngestionPipeline.Result result, long itemId) {
        updateStage(itemId, "Đang đề xuất metadata");
        try {
            MetadataSuggestion suggestion = metadataSuggester.suggest(result.content(), doc.getOriginalFilename());
            doc.setMetadataSuggestion(json.writeValueAsString(suggestion));

            if (suggestion.title() != null && suggestion.title().trustworthy()) {
                doc.setTitle(suggestion.title().value());
            } else if (result.detectedTitle() != null && !result.detectedTitle().isBlank()) {
                doc.setTitle(result.detectedTitle());
            }
            if (suggestion.effectiveFrom() != null && suggestion.effectiveFrom().trustworthy()) {
                doc.setEffectiveFrom(suggestion.effectiveFrom().value());
            }
            if (suggestion.hasNoExpiry() != null && suggestion.hasNoExpiry().trustworthy()) {
                doc.setHasNoExpiry(true);
            } else if (suggestion.effectiveTo() != null && suggestion.effectiveTo().trustworthy()) {
                doc.setEffectiveTo(suggestion.effectiveTo().value());
            }

        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            // Gợi ý metadata là bước TĂNG CƯỜNG. Nó hỏng thì tài liệu vẫn vào kho với tên tệp
            // làm tên, và quản trị sửa tay — chặn cả việc nạp vì một bước phụ trợ là sai.
            log.info("Bỏ bước gợi ý metadata cho {}: {}", doc.getOriginalFilename(), e.getMessage());
            if (result.detectedTitle() != null && !result.detectedTitle().isBlank()) {
                doc.setTitle(result.detectedTitle());
            }
        }
    }

    private void fail(IngestionItem item, Document doc, ErrorCode code,
                      String userMessage, String internalDetail) {
        item.setStatus(IngestionStatus.LOI);
        item.setErrorCode(code.name());
        item.setErrorReason(userMessage);
        doc.setStatus(DocumentStatus.LOI);
        doc.setErrorReason(userMessage);
        log.info("Tệp {} nạp lỗi [{}]: {}", item.getFilename(), code, internalDetail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateStage(long itemId, String stage) {
        items.findById(itemId).ifPresent(i -> {
            i.setStage(stage);
            items.save(i);
        });
    }

    private void closeBatchIfDone(long batchId) {
        long remaining = items.countByBatchIdAndStatus(batchId, IngestionStatus.CHO)
                       + items.countByBatchIdAndStatus(batchId, IngestionStatus.DANG_CHAY);
        if (remaining > 0) return;
        batches.findById(batchId).ifPresent(b -> {
            if (!"DANG_CHAY".equals(b.getStatus())) return;
            b.setStatus(cancelled.contains(batchId) ? "DA_HUY" : "XONG");
            b.setFinishedAt(Instant.now());
            batches.save(b);
            cancelled.remove(batchId);
        });
    }

    /**
     * Huỷ một đợt đang chạy.
     *
     * <p>Chỉ huỷ được những tệp <strong>chưa bắt đầu</strong>. Tệp đang bóc chữ giữa chừng vẫn
     * chạy hết — cắt ngang sẽ để lại một tài liệu có nửa số đoạn trong chỉ mục, và trạng thái
     * ấy tệ hơn cả việc nạp xong rồi gỡ đi.
     */
    @Transactional
    public void cancel(UUID trackingId) {
        IngestionBatch batch = batches.findByTrackingId(trackingId)
                .orElseThrow(BusinessException::khongTimThay);
        cancelled.add(batch.getId());
        log.info("Huỷ đợt nạp {} — các tệp chưa bắt đầu sẽ bị bỏ qua.", trackingId);
    }

    @Transactional(readOnly = true)
    public BatchProgress progress(UUID trackingId) {
        IngestionBatch batch = batches.findByTrackingId(trackingId)
                .orElseThrow(BusinessException::khongTimThay);
        List<IngestionItem> list = items.findByBatchIdOrderById(batch.getId());
        List<FileProgress> files = list.stream()
                .map(i -> new FileProgress(i.getId(), i.getFilename(), i.getDocumentId(),
                        i.getStatus().name(), i.getStage(), i.getErrorCode(), i.getErrorReason()))
                .toList();
        long done = list.stream().filter(i -> i.getStatus() == IngestionStatus.XONG).count();
        long failed = list.stream().filter(i -> i.getStatus() == IngestionStatus.LOI).count();
        return new BatchProgress(trackingId, batch.getStatus(), batch.getTotalFiles(),
                (int) done, (int) failed, files);
    }

    public record FileProgress(Long itemId, String filename, Long documentId,
                               String status, String stage, String errorCode, String errorReason) {}

    public record BatchProgress(UUID trackingId, String status, int total,
                                int done, int failed, List<FileProgress> files) {}

    private static String stripExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i <= 0 ? filename : filename.substring(0, i);
    }
}
