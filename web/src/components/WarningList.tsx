import type { Warning } from '../lib/api'

/**
 * Cảnh báo về tài liệu nguồn, hiện **trước** câu trả lời.
 *
 * Trước chứ không phải sau: người đọc cần biết nguồn có vấn đề ngay từ đầu, không phải sau
 * khi đã đọc xong và tin vào nó.
 *
 * Dùng màu cảnh báo chứ **không dùng màu lỗi**. "Văn bản này đã hết hiệu lực" không phải lỗi
 * của người dùng; tô nó màu lỗi thì họ tưởng mình vừa làm sai điều gì. Đây là chỗ hai thứ hay
 * bị gộp nhất, và gộp nhầm thì hoặc người dùng hoảng vô cớ, hoặc họ quen với màu đỏ rồi bỏ
 * qua cả lỗi thật.
 */
export function WarningList({ warnings }: { warnings: Warning[] }) {
  return (
    <div className="stack measure" style={{ gap: 'var(--s2)' }}>
      {warnings.map((w, i) => (
        <div key={`${w.documentId}-${w.warningType}-${i}`}
             className="notice notice-warning" style={{ marginBottom: 0 }} role="status">
          {/* Màu KHÔNG bao giờ là kênh thông tin duy nhất: mỗi cảnh báo có nhãn chữ. */}
          <span className="notice-label">{label(w.warningType)}</span>
          <span>
            <strong>{w.documentTitle}</strong>
            {w.detail && <> — {w.detail}</>}
          </span>
        </div>
      ))}
    </div>
  )
}

function label(type: string): string {
  switch (type) {
    case 'HET_HIEU_LUC':      return 'Hết hiệu lực'
    case 'SAP_HET_HIEU_LUC':  return 'Sắp hết hiệu lực'
    case 'DA_BI_THAY_THE':    return 'Đã bị thay thế'
    case 'CHONG_LAN':         return 'Còn nguồn khác'
    default:                  return 'Lưu ý'
  }
}
