import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, type Page } from '../lib/api'

interface ClusterView {
  id: number; label: string; reason: string; reasonLabel: string
  memberCount: number; firstSeen: string; lastSeen: string
  status: string; resolveNote: string | null
}

interface QuestionView {
  id: number; question: string; rewritten: string | null
  departmentSnapshot: string | null; askedAt: string
}

/**
 * Báo cáo khoảng trống tri thức.
 *
 * Màn này trả lời câu **"kho đang thiếu gì"**. Nó cố ý không có bộ lọc theo người dùng, và
 * danh sách câu hỏi không kèm tên người hỏi — phòng ban lúc hỏi là thông tin nhóm, đủ để
 * quyết định cấp quyền cho ai, mà không biến hệ thống thành công cụ giám sát nhân viên.
 */
export function AdminGapsPage() {
  const [rows, setRows] = useState<ClusterView[]>([])
  const [status, setStatus] = useState('CHO')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState<ClusterView | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const params = new URLSearchParams({ size: '50' })
      if (status) params.set('status', status)
      if (reason) params.set('reason', reason)
      const data = await api.get<Page<ClusterView>>(`/api/admin/gaps?${params}`)
      setRows(data.content)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được báo cáo khoảng trống.')
    } finally { setLoading(false) }
  }, [status, reason])

  useEffect(() => { void load() }, [load])

  async function resolve(cluster: ClusterView, ignore: boolean) {
    try {
      await api.post(`/api/admin/gaps/${cluster.id}/resolve`, { note: null, ignore })
      setOpen(null)
      void load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không đánh dấu được nhóm này.')
    }
  }

  return (
    <div className="stack">
      <div>
        <h1>Khoảng trống tri thức</h1>
        <p className="muted small measure" style={{ marginTop: 'var(--s2)' }}>
          Các chủ đề mà người dùng hỏi nhưng hệ thống không trả lời được. Mỗi nhóm gom nhiều
          cách hỏi khác nhau về cùng một việc.
        </p>
      </div>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      <div className="card">
        <div className="row wrap" style={{ gap: 'var(--s3)' }}>
          <div className="field" style={{ marginBottom: 0, minWidth: 180 }}>
            <label htmlFor="gs">Trạng thái</label>
            <select id="gs" value={status} onChange={e => setStatus(e.target.value)}>
              <option value="CHO">Chờ xử lý</option>
              <option value="DA_XU_LY">Đã xử lý</option>
              <option value="BO_QUA">Đã bỏ qua</option>
              <option value="">Tất cả</option>
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, minWidth: 260 }}>
            <label htmlFor="gr">Lý do</label>
            <select id="gr" value={reason} onChange={e => setReason(e.target.value)}>
              <option value="">Tất cả lý do</option>
              <option value="THIEU_TAI_LIEU">Kho chưa có tài liệu</option>
              <option value="THIEU_QUYEN">Có tài liệu, người hỏi thiếu quyền</option>
              <option value="CAN_CU_MONG">Có tài liệu nhưng chưa đủ chi tiết</option>
              <option value="NGOAI_PHAM_VI">Ngoài phạm vi kho</option>
            </select>
          </div>
        </div>
      </div>

      {loading ? <div className="state"><span className="spinner" /><p>Đang tải…</p></div>
        : rows.length === 0 ? (
        <div className="state">
          <h3>Chưa có khoảng trống nào chờ xử lý</h3>
          <p>
            Hệ thống đang trả lời được các câu hỏi người dùng đặt ra. Nhóm mới sẽ hiện ở đây
            khi có nhiều người hỏi cùng một chủ đề mà không có câu trả lời.
          </p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Chủ đề</th><th>Lý do</th>
                <th className="num">Số lần hỏi</th><th>Gần nhất</th><th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map(c => (
                <tr key={c.id}>
                  <td style={{ fontWeight: 500 }}>{c.label}</td>
                  <td>
                    <span className={`tag ${c.reason === 'THIEU_QUYEN' ? 'tag-accent' : 'tag-warning'}`}>
                      {c.reasonLabel}
                    </span>
                  </td>
                  <td className="num">{c.memberCount}</td>
                  <td className="small muted">{formatDate(c.lastSeen)}</td>
                  <td>
                    <button className="btn btn-sm btn-ghost" onClick={() => setOpen(c)}>
                      Xem câu hỏi
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {open && <ClusterDrawer cluster={open} onClose={() => setOpen(null)}
                              onResolve={ignore => void resolve(open, ignore)} />}
    </div>
  )
}

function ClusterDrawer({ cluster, onClose, onResolve }: {
  cluster: ClusterView
  onClose: () => void
  onResolve: (ignore: boolean) => void
}) {
  const [questions, setQuestions] = useState<QuestionView[] | null>(null)

  useEffect(() => {
    api.get<QuestionView[]>(`/api/admin/gaps/${cluster.id}/questions`)
       .then(setQuestions)
       .catch(() => setQuestions([]))
  }, [cluster.id])

  return (
    <div role="dialog" aria-modal="true" aria-label="Câu hỏi trong nhóm"
         onClick={e => { if (e.target === e.currentTarget) onClose() }}
         style={{ position: 'fixed', inset: 0, background: 'rgba(22,33,43,.45)',
                  display: 'flex', justifyContent: 'flex-end', zIndex: 50 }}>
      <div style={{ background: 'var(--bg-surface)', width: 'min(620px, 100%)', height: '100%',
                    display: 'flex', flexDirection: 'column' }}>
        <header style={{ padding: 'var(--s4) var(--s5)', borderBottom: '1px solid var(--border)' }}>
          <div className="row-between">
            <h3>{cluster.label}</h3>
            <button className="btn btn-sm btn-ghost" onClick={onClose}>Đóng</button>
          </div>
          <div className="row wrap xsmall muted" style={{ marginTop: 'var(--s2)' }}>
            <span className="tag tag-warning">{cluster.reasonLabel}</span>
            <span>{cluster.memberCount} lần hỏi</span>
            <span>từ {formatDate(cluster.firstSeen)}</span>
          </div>
        </header>

        <div style={{ overflowY: 'auto', padding: 'var(--s5)' }} className="stack">
          {questions === null ? <div className="state"><span className="spinner" /></div>
            : questions.map(q => (
            <div key={q.id} style={{
              borderLeft: '3px solid var(--border-strong)', paddingLeft: 'var(--s4)',
            }}>
              <div>{q.question}</div>
              {q.rewritten && q.rewritten !== q.question && (
                <div className="xsmall muted" style={{ marginTop: 'var(--s1)' }}>
                  Hiểu thành: {q.rewritten}
                </div>
              )}
              <div className="xsmall muted" style={{ marginTop: 'var(--s1)' }}>
                {/* Phòng ban LÚC HỎI, không phải phòng ban hiện tại — người hỏi có thể đã chuyển. */}
                {q.departmentSnapshot || 'Không rõ phòng ban'} · {formatDate(q.askedAt)}
              </div>
            </div>
          ))}
        </div>

        <footer style={{ padding: 'var(--s4) var(--s5)', borderTop: '1px solid var(--border)' }}>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button className="btn" onClick={() => onResolve(true)}>Bỏ qua nhóm này</button>
            <button className="btn btn-primary" onClick={() => onResolve(false)}>
              Đánh dấu đã xử lý
            </button>
          </div>
        </footer>
      </div>
    </div>
  )
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    })
  } catch { return iso }
}
