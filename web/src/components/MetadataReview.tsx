import { useEffect, useState } from 'react'
import { api, ApiError, type DocumentRow } from '../lib/api'

interface Suggested<T> { value: T; evidence: string | null; verified: boolean }

interface Suggestion {
  title?: Suggested<string>
  category?: Suggested<string>
  effectiveFrom?: Suggested<string>
  effectiveTo?: Suggested<string>
  hasNoExpiry?: Suggested<boolean>
  sensitivity?: Suggested<number>
  notes?: string[]
}

/**
 * Xem xét metadata hệ thống đề xuất, kèm trích đoạn làm căn cứ từng giá trị.
 *
 * <p>Trích đoạn là điểm chính của màn này, không phải phần trang trí. Quản trị phải xác nhận
 * metadata cho cả nghìn tài liệu; nếu mỗi giá trị chỉ hiện ra một con số không rõ từ đâu thì họ
 * chỉ có hai lựa chọn — bấm đồng ý hết, hoặc mở từng tệp gốc ra dò. Cả hai đều làm khâu xác nhận
 * trở nên vô nghĩa.
 *
 * <p>Giá trị **chưa đối chiếu được** hiện khác hẳn giá trị đã đối chiếu. Mô hình trả về căn cứ
 * rất thuyết phục cho những câu không có trong tài liệu, và một căn cứ bịa còn tệ hơn không có
 * căn cứ — vì nó làm người đọc tin.
 */
export function MetadataReview({ doc, onClose, onConfirmed }: {
  doc: DocumentRow
  onClose: () => void
  onConfirmed: () => void
}) {
  const [suggestion, setSuggestion] = useState<Suggestion | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let alive = true
    api.get<Suggestion>(`/api/admin/documents/${doc.id}/metadata-suggestion`)
      .then(s => { if (alive) { setSuggestion(s); setLoading(false) } })
      .catch(err => {
        if (!alive) return
        setError(err instanceof ApiError ? err.message : 'Không đọc được đề xuất metadata.')
        setLoading(false)
      })
    return () => { alive = false }
  }, [doc.id])

  async function confirm() {
    setBusy(true)
    try {
      await api.post(`/api/admin/documents/${doc.id}/confirm-metadata`)
      onConfirmed()
      onClose()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không xác nhận được.')
    } finally {
      setBusy(false)
    }
  }

  const rows: { label: string; field?: Suggested<unknown> }[] = suggestion ? [
    { label: 'Tên tài liệu',      field: suggestion.title },
    { label: 'Danh mục',          field: suggestion.category },
    { label: 'Ngày hiệu lực',     field: suggestion.effectiveFrom },
    { label: 'Ngày hết hiệu lực', field: suggestion.effectiveTo },
    { label: 'Không có thời hạn', field: suggestion.hasNoExpiry },
    { label: 'Nhãn mật đề xuất',  field: suggestion.sensitivity },
  ].filter(r => r.field) : []

  const unverified = rows.filter(r => r.field && !r.field.verified).length

  return (
    <div role="dialog" aria-modal="true" aria-label="Metadata đề xuất"
         onClick={e => { if (e.target === e.currentTarget) onClose() }}
         style={{ position: 'fixed', inset: 0, background: 'rgba(22,33,43,.45)',
                  display: 'flex', justifyContent: 'flex-end', zIndex: 50 }}>
      <div style={{ background: 'var(--bg-surface)', width: 'min(640px, 100%)', height: '100%',
                    display: 'flex', flexDirection: 'column' }}>

        <header style={{ padding: 'var(--s4) var(--s5)', borderBottom: '1px solid var(--border)' }}>
          <div className="row-between">
            <h3>Metadata đề xuất</h3>
            <button className="btn btn-sm btn-ghost" onClick={onClose}>Đóng</button>
          </div>
          <div className="xsmall muted" style={{ marginTop: 'var(--s1)' }}>{doc.originalFilename}</div>
        </header>

        <div style={{ overflowY: 'auto', padding: 'var(--s5)' }} className="stack">
          {loading && <div className="state"><span className="spinner" /><p>Đang đọc…</p></div>}

          {error && (
            <div className="notice notice-error" role="alert">
              <span className="notice-label">Lỗi</span><span>{error}</span>
            </div>
          )}

          {suggestion && rows.length === 0 && (
            <div className="state">
              <h3>Chưa có đề xuất nào</h3>
              <p>
                Hệ thống không rút được metadata nào từ tài liệu này. Nhập tay ở bảng danh sách.
              </p>
            </div>
          )}

          {unverified > 0 && (
            <div className="notice notice-warning">
              <span className="notice-label">Cần soi kỹ</span>
              <span>
                {unverified} giá trị có căn cứ <strong>không tìm thấy trong tài liệu</strong>.
                Hệ thống không tự áp những giá trị đó — đối chiếu tệp gốc trước khi dùng.
              </span>
            </div>
          )}

          {rows.map(({ label, field }) => field && (
            <div key={label} style={{
              border: '1px solid var(--border)',
              borderLeft: field.verified
                ? '3px solid var(--success)' : '3px solid var(--warning)',
              borderRadius: 'var(--radius-sm)',
              padding: 'var(--s3) var(--s4)',
            }}>
              <div className="row-between wrap" style={{ gap: 'var(--s2)' }}>
                <span className="xsmall muted">{label}</span>
                {/* Màu không bao giờ là kênh duy nhất: mỗi trạng thái có nhãn chữ. */}
                {field.verified
                  ? <span className="tag tag-success">Đã đối chiếu</span>
                  : <span className="tag tag-warning">Chưa đối chiếu</span>}
              </div>

              <div style={{ fontWeight: 500, marginTop: 'var(--s1)' }}>
                {formatValue(field.value)}
              </div>

              {field.evidence && (
                <blockquote style={{
                  margin: 'var(--s2) 0 0', paddingLeft: 'var(--s3)',
                  borderLeft: '2px solid var(--border-strong)',
                  fontSize: 'var(--text-sm)', color: 'var(--text-secondary)',
                }}>
                  {field.evidence}
                </blockquote>
              )}
            </div>
          ))}

          {suggestion?.notes && suggestion.notes.length > 0 && (
            <div className="notice notice-info">
              <span className="notice-label">Ghi chú</span>
              <span>{suggestion.notes.join(' ')}</span>
            </div>
          )}
        </div>

        <footer style={{ padding: 'var(--s4) var(--s5)', borderTop: '1px solid var(--border)' }}>
          <div className="row-between wrap">
            <span className="xsmall muted">
              {doc.metadataConfirmed ? 'Đã xác nhận' : 'Chưa xác nhận'}
            </span>
            <button className="btn btn-primary" onClick={() => void confirm()}
                    disabled={busy || doc.metadataConfirmed}>
              {busy ? <><span className="spinner" /> Đang lưu…</> : 'Xác nhận metadata'}
            </button>
          </div>
        </footer>
      </div>
    </div>
  )
}

function formatValue(value: unknown): string {
  if (typeof value === 'boolean') return value ? 'Có' : 'Không'
  if (value === null || value === undefined) return '—'
  return String(value)
}
