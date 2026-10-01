import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, type Page } from '../lib/api'

/**
 * Cấp và gỡ quyền đọc tài liệu.
 *
 * Đây là màn khoá cả hệ thống: quyền là **từ chối mặc định**, nên một kho vừa nạp xong là một
 * kho chưa ai đọc được. Trước khi có màn này, việc mở quyền chỉ làm được bằng cách gọi API bằng
 * tay — tức là hệ thống chạy được nhưng không giao cho ai dùng được.
 *
 * Hai điều màn này cố ý làm khác một bảng CRUD thường:
 *
 * 1. **Tách "cấp trực tiếp" khỏi "cấp qua phòng ban".** Chúng trông giống nhau khi đọc nhưng
 *    gỡ khác nhau: mục trực tiếp gỡ ngay tại đây, còn mục qua phòng ban phải sửa ở phòng ban —
 *    gộp chung một danh sách sẽ sinh ra những cú bấm "Gỡ" không có tác dụng.
 * 2. **Từ chối hiện tách và hiện rõ là thắng.** Trong mô hình này một mục từ chối thắng mọi mục
 *    cho phép, kể cả cho phép trực tiếp. Một hàng màu đỏ lẫn giữa các hàng xanh là cách người
 *    quản trị mất nửa giờ tự hỏi vì sao đã cấp quyền mà người kia vẫn không đọc được.
 */

export interface AclEntry {
  id: number
  subjectType: string
  subjectId: number
  subjectName: string
  effect: string
  grantedAt: string
}

export interface DocumentPermissions {
  documentId: number
  direct: AclEntry[]
  viaDepartment: AclEntry[]
}

export interface UserLite {
  id: number; username: string; fullName: string; active: boolean
}

export interface DepartmentLite {
  id: number; name: string; memberCount: number
}

type SubjectKind = 'NGUOI_DUNG' | 'PHONG_BAN'

/** Giá trị mà máy chủ nhận — viết một chỗ để không có chuỗi rời rạc nào lệch đi. */
const CHO_PHEP = 'CHO_PHEP'
const TU_CHOI = 'TU_CHOI'

// ── Bộ chọn đối tượng được cấp quyền ─────────────────────────────────────────────

/**
 * Chọn một người hoặc một phòng ban.
 *
 * Danh sách người dùng tải theo từ khoá chứ không tải hết: với hai trăm tài khoản, một ô chọn
 * đổ đầy là một ô không dùng được. Phòng ban thì tải hết vì chúng ít và người quản trị cần
 * thấy toàn bộ để chọn đúng.
 */
export function SubjectPicker({ kind, onKindChange, subjectId, onSubjectChange }: {
  kind: SubjectKind
  onKindChange: (k: SubjectKind) => void
  subjectId: number | null
  onSubjectChange: (id: number | null, name: string) => void
}) {
  const [q, setQ] = useState('')
  const [users, setUsers] = useState<UserLite[]>([])
  const [departments, setDepartments] = useState<DepartmentLite[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (kind !== 'PHONG_BAN') return
    void (async () => {
      try { setDepartments(await api.get<DepartmentLite[]>('/api/admin/departments')) }
      catch { /* lỗi hiện ở nơi gọi */ }
    })()
  }, [kind])

  useEffect(() => {
    if (kind !== 'NGUOI_DUNG') return
    const timer = window.setTimeout(async () => {
      setLoading(true)
      try {
        const params = new URLSearchParams({ page: '0', size: '20' })
        if (q.trim()) params.set('q', q.trim())
        const data = await api.get<Page<UserLite>>(`/api/admin/users?${params}`)
        setUsers(data.content)
      } catch { /* lỗi hiện ở nơi gọi */ }
      finally { setLoading(false) }
    }, 250)   // gõ tới đâu gọi tới đó sẽ bắn một yêu cầu mỗi phím
    return () => window.clearTimeout(timer)
  }, [kind, q])

  return (
    <div className="stack" style={{ gap: 'var(--s3)' }}>
      <div className="row" role="radiogroup" aria-label="Cấp cho ai">
        <button type="button" role="radio" aria-checked={kind === 'NGUOI_DUNG'}
                className={`btn btn-sm ${kind === 'NGUOI_DUNG' ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => { onKindChange('NGUOI_DUNG'); onSubjectChange(null, '') }}>
          Một người
        </button>
        <button type="button" role="radio" aria-checked={kind === 'PHONG_BAN'}
                className={`btn btn-sm ${kind === 'PHONG_BAN' ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => { onKindChange('PHONG_BAN'); onSubjectChange(null, '') }}>
          Cả phòng ban
        </button>
      </div>

      {kind === 'NGUOI_DUNG' ? (
        <>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="sp-q">Tìm người</label>
            <input id="sp-q" type="text" value={q} placeholder="Tên hoặc tên đăng nhập"
                   onChange={e => setQ(e.target.value)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="sp-u">Chọn người</label>
            <select id="sp-u" value={subjectId ?? ''}
                    onChange={e => {
                      const id = e.target.value ? Number(e.target.value) : null
                      const u = users.find(x => x.id === id)
                      onSubjectChange(id, u ? `${u.fullName} (${u.username})` : '')
                    }}>
              <option value="">{loading ? 'Đang tìm…' : '— chọn một người —'}</option>
              {users.map(u => (
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.username}){u.active ? '' : ' — đã vô hiệu hoá'}
                </option>
              ))}
            </select>
          </div>
        </>
      ) : (
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="sp-d">Chọn phòng ban</label>
          <select id="sp-d" value={subjectId ?? ''}
                  onChange={e => {
                    const id = e.target.value ? Number(e.target.value) : null
                    const d = departments.find(x => x.id === id)
                    onSubjectChange(id, d ? d.name : '')
                  }}>
            <option value="">— chọn một phòng ban —</option>
            {departments.map(d => (
              <option key={d.id} value={d.id}>{d.name} ({d.memberCount} người)</option>
            ))}
          </select>
          {departments.length === 0 && (
            <p className="xsmall muted" style={{ marginTop: 'var(--s2)' }}>
              Chưa có phòng ban nào. Tạo ở màn <strong>Tài khoản → Phòng ban</strong>.
            </p>
          )}
        </div>
      )}
    </div>
  )
}

// ── Quyền của MỘT tài liệu ──────────────────────────────────────────────────────

export function DocumentPermissionsDialog({ documentId, documentTitle, onClose, onExplain }: {
  documentId: number
  documentTitle: string
  onClose: () => void
  onExplain?: () => void
}) {
  const [data, setData] = useState<DocumentPermissions | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [kind, setKind] = useState<SubjectKind>('PHONG_BAN')
  const [subjectId, setSubjectId] = useState<number | null>(null)
  const [effect, setEffect] = useState(CHO_PHEP)

  const load = useCallback(async () => {
    setError(null)
    try {
      setData(await api.get<DocumentPermissions>(`/api/admin/documents/${documentId}/permissions`))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được danh sách quyền.')
    }
  }, [documentId])

  useEffect(() => { void load() }, [load])

  async function them() {
    if (!subjectId) return
    setBusy(true); setError(null)
    try {
      await api.post(`/api/admin/documents/${documentId}/permissions`,
        { subjectType: kind, subjectId, effect })
      setSubjectId(null)
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không cấp được quyền.')
    } finally { setBusy(false) }
  }

  async function go(e: AclEntry) {
    setBusy(true); setError(null)
    try {
      await api.del(`/api/admin/documents/${documentId}/permissions`,
        { subjectType: e.subjectType, subjectId: e.subjectId, effect: e.effect })
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không gỡ được mục quyền.')
    } finally { setBusy(false) }
  }

  const truc_tiep = data?.direct ?? []
  const qua_phong = data?.viaDepartment ?? []
  const trong = truc_tiep.length === 0 && qua_phong.length === 0

  return (
    <Modal onClose={onClose} rong={640}>
      <h3>Quyền đọc — {documentTitle}</h3>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      {trong && (
        <div className="notice notice-warning">
          <span className="notice-label">Chưa ai đọc được</span>
          <span>
            Tài liệu này chưa có mục cấp quyền nào, nên <strong>không ai</strong> tra cứu được
            nó — kể cả quản trị. Cấp cho một phòng ban là cách nhanh nhất.
          </span>
        </div>
      )}

      {truc_tiep.length > 0 && (
        <BangQuyen tieuDe="Cấp trực tiếp" muc={truc_tiep} goDuoc onGo={go} busy={busy} />
      )}
      {qua_phong.length > 0 && (
        <BangQuyen tieuDe="Cấp qua phòng ban" muc={qua_phong} goDuoc={false} onGo={go} busy={busy}
                   ghiChu="Gỡ những mục này ở chính phòng ban, không gỡ tại đây." />
      )}

      <div className="card" style={{ marginTop: 'var(--s4)' }}>
        <h4 style={{ marginTop: 0 }}>Thêm quyền</h4>
        <SubjectPicker kind={kind} onKindChange={setKind}
                       subjectId={subjectId} onSubjectChange={id => setSubjectId(id)} />
        <div className="field" style={{ marginTop: 'var(--s3)', marginBottom: 0 }}>
          <label htmlFor="eff">Loại</label>
          <select id="eff" value={effect} onChange={e => setEffect(e.target.value)}>
            <option value={CHO_PHEP}>Cho phép đọc</option>
            <option value={TU_CHOI}>Từ chối đọc (thắng mọi mục cho phép)</option>
          </select>
        </div>
        <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
          <button className="btn btn-primary" disabled={!subjectId || busy}
                  onClick={() => void them()}>
            {busy ? 'Đang ghi…' : 'Thêm'}
          </button>
        </div>
      </div>

      <div className="row-between" style={{ marginTop: 'var(--s4)' }}>
        {onExplain
          ? <button className="btn btn-ghost btn-sm" onClick={onExplain}>Ai đọc được?</button>
          : <span />}
        <button className="btn" onClick={onClose}>Đóng</button>
      </div>
    </Modal>
  )
}

function BangQuyen({ tieuDe, muc, goDuoc, onGo, busy, ghiChu }: {
  tieuDe: string
  muc: AclEntry[]
  goDuoc: boolean
  onGo: (e: AclEntry) => void
  busy: boolean
  ghiChu?: string
}) {
  return (
    <div className="stack" style={{ gap: 'var(--s2)', marginTop: 'var(--s4)' }}>
      <h4 style={{ margin: 0 }}>{tieuDe}</h4>
      {ghiChu && <p className="xsmall muted" style={{ margin: 0 }}>{ghiChu}</p>}
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Đối tượng</th><th>Loại</th><th>Cấp lúc</th><th></th></tr>
          </thead>
          <tbody>
            {muc.map(e => (
              <tr key={`${e.subjectType}-${e.subjectId}-${e.effect}`}>
                <td>
                  <span className="tag">
                    {e.subjectType === 'PHONG_BAN' ? 'Phòng ban' : 'Người dùng'}
                  </span>{' '}
                  {e.subjectName}
                </td>
                <td>
                  {e.effect === TU_CHOI
                    ? <span className="tag tag-error">Từ chối</span>
                    : <span className="tag tag-success">Cho phép</span>}
                </td>
                <td className="xsmall muted">{e.grantedAt?.slice(0, 10) ?? '—'}</td>
                <td>
                  {goDuoc && (
                    <button className="btn btn-sm btn-ghost" disabled={busy}
                            onClick={() => onGo(e)}>Gỡ</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Cấp quyền cho NHIỀU tài liệu cùng lúc ───────────────────────────────────────

export function BulkGrantDialog({ documentIds, onClose, onDone }: {
  documentIds: number[]
  onClose: () => void
  onDone: (applied: number, selected: number) => void
}) {
  const [kind, setKind] = useState<SubjectKind>('PHONG_BAN')
  const [subjectId, setSubjectId] = useState<number | null>(null)
  const [subjectName, setSubjectName] = useState('')
  const [effect, setEffect] = useState(CHO_PHEP)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function apDung() {
    if (!subjectId) return
    setBusy(true); setError(null)
    try {
      const kq = await api.post<{ applied: number; selected: number }>(
        '/api/admin/permissions/bulk-grant',
        { documentIds, subjectType: kind, subjectId, effect })
      onDone(kq.applied, kq.selected)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không cấp được quyền hàng loạt.')
      setBusy(false)
    }
  }

  return (
    <Modal onClose={onClose} rong={560}>
      <h3>Cấp quyền cho {documentIds.length} tài liệu đã chọn</h3>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      <SubjectPicker kind={kind} onKindChange={setKind} subjectId={subjectId}
                     onSubjectChange={(id, name) => { setSubjectId(id); setSubjectName(name) }} />

      <div className="field" style={{ marginTop: 'var(--s3)' }}>
        <label htmlFor="bg-eff">Loại</label>
        <select id="bg-eff" value={effect} onChange={e => setEffect(e.target.value)}>
          <option value={CHO_PHEP}>Cho phép đọc</option>
          <option value={TU_CHOI}>Từ chối đọc (thắng mọi mục cho phép)</option>
        </select>
      </div>

      {/*
        Nhắc lại việc sắp làm bằng CON SỐ và TÊN, không phải "bạn có chắc không". Cấp nhầm cho
        một phòng ban trên năm trăm tài liệu là việc phải đi gỡ từng mục.
      */}
      {subjectId && (
        <div className={`notice ${effect === TU_CHOI ? 'notice-warning' : 'notice-info'}`}>
          <span className="notice-label">Sắp làm</span>
          <span>
            {effect === TU_CHOI ? 'Từ chối' : 'Cho phép'} <strong>{subjectName}</strong> đọc{' '}
            <strong>{documentIds.length} tài liệu</strong>.
            {effect === TU_CHOI && ' Mục từ chối thắng mọi mục cho phép đang có.'}
          </span>
        </div>
      )}

      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose} disabled={busy}>Huỷ</button>
        <button className="btn btn-primary" disabled={!subjectId || busy}
                onClick={() => void apDung()}>
          {busy ? 'Đang ghi…' : 'Cấp quyền'}
        </button>
      </div>
    </Modal>
  )
}

// ── "Vì sao người này đọc được / không đọc được" ────────────────────────────────

export function ExplainAccessDialog({ documentId, documentTitle, onClose }: {
  documentId: number
  documentTitle: string
  onClose: () => void
}) {
  const [kind, setKind] = useState<SubjectKind>('NGUOI_DUNG')
  const [userId, setUserId] = useState<number | null>(null)
  const [ketQua, setKetQua] = useState<{ allowed: boolean; reason: string } | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!userId) { setKetQua(null); return }
    void (async () => {
      setError(null)
      try {
        setKetQua(await api.get<{ allowed: boolean; reason: string }>(
          `/api/admin/permissions/explain?userId=${userId}&documentId=${documentId}`))
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Không tra được.')
      }
    })()
  }, [userId, documentId])

  return (
    <Modal onClose={onClose} rong={560}>
      <h3>Ai đọc được — {documentTitle}</h3>
      <p className="small muted">
        Chọn một người để xem họ có đọc được tài liệu này không, và <strong>vì sao</strong>.
        Đây là chỗ trả lời câu "đã cấp quyền rồi mà sao vẫn không thấy".
      </p>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      <SubjectPicker kind={kind} onKindChange={setKind}
                     subjectId={userId} onSubjectChange={id => setUserId(id)} />

      {kind === 'PHONG_BAN' && (
        <p className="xsmall muted">Chỉ tra được theo từng người — quyền cuối cùng phụ thuộc cả
          mức mật của chính người đó.</p>
      )}

      {ketQua && (
        <div className={`notice ${ketQua.allowed ? 'notice-success' : 'notice-warning'}`}
             style={{ marginTop: 'var(--s4)' }}>
          <span className="notice-label">{ketQua.allowed ? 'Đọc được' : 'Không đọc được'}</span>
          <span>{ketQua.reason}</span>
        </div>
      )}

      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose}>Đóng</button>
      </div>
    </Modal>
  )
}

// ── Khung hộp thoại dùng chung ──────────────────────────────────────────────────

export function Modal({ children, onClose, rong = 560 }: {
  children: React.ReactNode
  onClose: () => void
  rong?: number
}) {
  // Đóng bằng phím Esc: hộp thoại không có đường thoát bằng bàn phím là hộp thoại bẫy người
  // dùng chỉ dùng bàn phím.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div role="dialog" aria-modal="true"
         style={{ position: 'fixed', inset: 0, background: 'rgba(22,33,43,.45)',
                  display: 'grid', placeItems: 'center', zIndex: 50, padding: 'var(--s4)' }}
         onClick={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className="card"
           style={{ maxWidth: rong, width: '100%', maxHeight: '86vh', overflowY: 'auto' }}>
        {children}
      </div>
    </div>
  )
}
