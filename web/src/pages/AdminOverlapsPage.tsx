import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'

interface PairView {
  docAId: number; titleA: string
  docBId: number; titleB: string
  similarity: number; similarityPercent: number
  decision: string; detectedAt: string; decidedAt: string | null
}

/**
 * Xử lý các cặp tài liệu bị nghi chồng lấn.
 *
 * **Mỗi cặp một thẻ**, xếp theo mức độ giống nhau giảm dần. Bốn hướng xử lý dẫn tới bốn hệ
 * quả khác nhau nên chúng là bốn nút, không phải một nút "đã xem":
 *
 * - *Thay thế* ghi luôn chuỗi thay thế và gỡ bản cũ khỏi kho.
 * - *Cùng tồn tại* làm mọi câu trả lời dựa trên một trong hai bản **kèm cảnh báo** từ đó về sau.
 * - *Báo động giả* khiến cặp này không bao giờ hiện lại — kể cả sau khi nạp lại kho.
 */
export function AdminOverlapsPage() {
  const [rows, setRows] = useState<PairView[]>([])
  const [filter, setFilter] = useState('CHO')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deciding, setDeciding] = useState<PairView | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      setRows(await api.get<PairView[]>(`/api/admin/overlaps?decision=${filter}&size=50`))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được danh sách chồng lấn.')
    } finally { setLoading(false) }
  }, [filter])

  useEffect(() => { void load() }, [load])

  async function decide(pair: PairView, decision: string, newerDocumentId?: number) {
    try {
      await api.post(`/api/admin/overlaps/${pair.docAId}/${pair.docBId}`,
                     { decision, newerDocumentId: newerDocumentId ?? null })
      setDeciding(null)
      void load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không lưu được quyết định.')
    }
  }

  return (
    <div className="stack">
      <div>
        <h1>Tài liệu chồng lấn</h1>
        <p className="muted small measure" style={{ marginTop: 'var(--s2)' }}>
          Các cặp tài liệu có nội dung gần giống nhau. Xử lý để người hỏi không nhận hai câu
          trả lời khác nhau cho cùng một việc.
        </p>
      </div>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      <div className="row">
        {[['CHO', 'Chờ xử lý'], ['CUNG_TON_TAI', 'Cùng tồn tại'],
          ['THAY_THE', 'Đã thay thế'], ['BAO_DONG_GIA', 'Báo động giả']].map(([value, label]) => (
          <button key={value}
                  className={`btn btn-sm ${filter === value ? 'btn-primary' : 'btn-ghost'}`}
                  onClick={() => setFilter(value)}>{label}</button>
        ))}
      </div>

      {loading ? <div className="state"><span className="spinner" /><p>Đang tải…</p></div>
        : rows.length === 0 ? (
        <div className="state">
          <h3>Không có cặp nào trong nhóm này</h3>
          <p>Hệ thống dò chồng lấn sau mỗi lần nạp tài liệu mới.</p>
        </div>
      ) : (
        <div className="stack">
          {rows.map(p => (
            <div key={`${p.docAId}-${p.docBId}`} className="card">
              <div className="row-between wrap">
                <span className="tag tag-warning">
                  Giống nhau khoảng {p.similarityPercent}%
                </span>
                {p.decision !== 'CHO' && (
                  <span className="tag">{decisionLabel(p.decision)}</span>
                )}
              </div>

              <div className="row wrap" style={{ gap: 'var(--s4)', marginTop: 'var(--s3)' }}>
                <div className="grow" style={{ minWidth: 220 }}>
                  <div className="xsmall muted">Tài liệu A</div>
                  <div style={{ fontWeight: 500 }}>{p.titleA}</div>
                </div>
                <div className="grow" style={{ minWidth: 220 }}>
                  <div className="xsmall muted">Tài liệu B</div>
                  <div style={{ fontWeight: 500 }}>{p.titleB}</div>
                </div>
              </div>

              {p.decision === 'CHO' && (
                <div className="row wrap" style={{ marginTop: 'var(--s4)' }}>
                  <button className="btn btn-sm" onClick={() => setDeciding(p)}>
                    Một bản thay thế bản kia
                  </button>
                  <button className="btn btn-sm"
                          onClick={() => void decide(p, 'CUNG_TON_TAI')}>
                    Cả hai đều hợp lệ
                  </button>
                  <button className="btn btn-sm btn-ghost"
                          onClick={() => void decide(p, 'BAO_DONG_GIA')}>
                    Báo động giả
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {deciding && (
        <div role="dialog" aria-modal="true"
             style={{ position: 'fixed', inset: 0, background: 'rgba(22,33,43,.45)',
                      display: 'grid', placeItems: 'center', zIndex: 50 }}>
          <div className="card" style={{ maxWidth: 520, margin: 'var(--s5)' }}>
            <h3>Bản nào là bản mới?</h3>
            <p className="small muted">
              {/* Không đoán theo ngày tải lên: tài liệu cũ hoàn toàn có thể được số hoá sau. */}
              Hệ thống không tự đoán theo ngày tải lên, vì tài liệu cũ vẫn có thể được số hoá
              sau tài liệu mới. Bản cũ sẽ được gỡ khỏi kho và chuỗi thay thế được ghi lại.
            </p>
            <div className="stack" style={{ gap: 'var(--s2)' }}>
              <button className="btn" onClick={() => void decide(deciding, 'THAY_THE', deciding.docAId)}>
                “{deciding.titleA}” là bản mới
              </button>
              <button className="btn" onClick={() => void decide(deciding, 'THAY_THE', deciding.docBId)}>
                “{deciding.titleB}” là bản mới
              </button>
            </div>
            <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
              <button className="btn btn-ghost" onClick={() => setDeciding(null)}>Huỷ</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function decisionLabel(decision: string): string {
  switch (decision) {
    case 'THAY_THE':     return 'Đã thay thế'
    case 'CUNG_TON_TAI': return 'Cùng tồn tại hợp lệ'
    case 'BAO_DONG_GIA': return 'Báo động giả'
    default:             return 'Chờ xử lý'
  }
}
