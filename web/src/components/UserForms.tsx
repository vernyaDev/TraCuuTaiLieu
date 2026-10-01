import { useEffect, useState } from 'react'
import { api, ApiError } from '../lib/api'
import { Modal, type DepartmentLite } from './PermissionPanel'

/**
 * Tạo và sửa tài khoản, tạo phòng ban, nhập tài khoản hàng loạt từ tệp.
 *
 * Trước những màn này, việc duy nhất giao diện làm được với tài khoản là bật/tắt. Tạo người,
 * gán phòng ban, đặt mức mật, nhập danh sách — tất cả chỉ gọi được bằng API bằng tay, tức là
 * không triển khai được cho một công ty hai trăm người.
 */

export interface ClearanceLevel { level: number; label: string }

/** Thang mức mật lấy từ máy chủ, không ghi cứng: thêm một mức là sửa cấu hình, không sửa mã. */
export function useClearanceLevels(): ClearanceLevel[] {
  const [levels, setLevels] = useState<ClearanceLevel[]>([])
  useEffect(() => {
    void (async () => {
      try { setLevels(await api.get<ClearanceLevel[]>('/api/admin/clearance-levels')) }
      catch { setLevels([]) }
    })()
  }, [])
  return levels
}

export function useDepartments(reloadKey = 0): DepartmentLite[] {
  const [ds, setDs] = useState<DepartmentLite[]>([])
  useEffect(() => {
    void (async () => {
      try { setDs(await api.get<DepartmentLite[]>('/api/admin/departments')) }
      catch { setDs([]) }
    })()
  }, [reloadKey])
  return ds
}

interface UserForm {
  username: string
  fullName: string
  email: string
  password: string
  clearanceLevel: number
  role: string
  departmentIds: number[]
}

// ── Tạo hoặc sửa một tài khoản ──────────────────────────────────────────────────

export function UserDialog({ dangSua, onClose, onDone }: {
  /** Có giá trị là sửa, rỗng là tạo mới. */
  dangSua: {
    id: number; username: string; fullName: string; email: string | null
    role: string; clearanceLevel: number; departments: string[]
  } | null
  onClose: () => void
  onDone: (thongBao: string) => void
}) {
  const levels = useClearanceLevels()
  const phongBan = useDepartments()
  const [f, setF] = useState<UserForm>({
    username: dangSua?.username ?? '',
    fullName: dangSua?.fullName ?? '',
    email: dangSua?.email ?? '',
    password: '',
    clearanceLevel: dangSua?.clearanceLevel ?? 1,
    role: dangSua?.role ?? 'NHAN_VIEN',
    departmentIds: [],
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Khi sửa: phòng ban trả về dưới dạng TÊN, nên phải đối chiếu ngược ra mã. Làm sau khi danh
  // sách phòng ban về, nếu không mọi ô tích đều trống dù người đó có phòng ban.
  useEffect(() => {
    if (!dangSua || phongBan.length === 0) return
    const ids = phongBan.filter(d => dangSua.departments.includes(d.name)).map(d => d.id)
    setF(truoc => ({ ...truoc, departmentIds: ids }))
  }, [dangSua, phongBan])

  function doiPhong(id: number) {
    setF(t => ({
      ...t,
      departmentIds: t.departmentIds.includes(id)
        ? t.departmentIds.filter(x => x !== id)
        : [...t.departmentIds, id],
    }))
  }

  async function luu() {
    setBusy(true); setError(null)
    try {
      const than = {
        fullName: f.fullName.trim(),
        email: f.email.trim() || null,
        clearanceLevel: f.clearanceLevel,
        role: f.role,
        departmentIds: f.departmentIds,
      }
      if (dangSua) {
        await api.put(`/api/admin/users/${dangSua.id}`, than)
        onDone(`Đã cập nhật tài khoản ${dangSua.username}.`)
      } else {
        await api.post('/api/admin/users',
          { ...than, username: f.username.trim(), password: f.password })
        onDone(`Đã tạo tài khoản ${f.username.trim()}. Người này phải đổi mật khẩu ở lần đăng nhập đầu.`)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không lưu được tài khoản.')
      setBusy(false)
    }
  }

  const duDieuKien = dangSua
    ? f.fullName.trim().length > 0
    : f.username.trim().length >= 3 && f.fullName.trim().length > 0 && f.password.length >= 12

  return (
    <Modal onClose={onClose} rong={600}>
      <h3>{dangSua ? `Sửa tài khoản ${dangSua.username}` : 'Tạo tài khoản mới'}</h3>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      {!dangSua && (
        <div className="field">
          <label htmlFor="u-username">Tên đăng nhập</label>
          <input id="u-username" type="text" value={f.username} autoComplete="off"
                 onChange={e => setF({ ...f, username: e.target.value })} />
          <p className="xsmall muted">Ít nhất 3 ký tự. Không đổi được sau khi tạo.</p>
        </div>
      )}

      <div className="field">
        <label htmlFor="u-fullname">Họ và tên</label>
        <input id="u-fullname" type="text" value={f.fullName}
               onChange={e => setF({ ...f, fullName: e.target.value })} />
      </div>

      <div className="field">
        <label htmlFor="u-email">Thư điện tử (không bắt buộc)</label>
        <input id="u-email" type="email" value={f.email}
               onChange={e => setF({ ...f, email: e.target.value })} />
      </div>

      {!dangSua && (
        <div className="field">
          <label htmlFor="u-pass">Mật khẩu tạm</label>
          <input id="u-pass" type="text" value={f.password} autoComplete="off"
                 onChange={e => setF({ ...f, password: e.target.value })} />
          <p className="xsmall muted">
            Ít nhất 12 ký tự, không nằm trong danh sách mật khẩu phổ biến. Người dùng
            <strong> bắt buộc đổi</strong> ở lần đăng nhập đầu — quản trị đặt mật khẩu nào thì
            quản trị cũng biết nó, nên nó chưa phải bí mật của riêng ai.
          </p>
        </div>
      )}

      <div className="field">
        <label htmlFor="u-clearance">Mức mật</label>
        <select id="u-clearance" value={f.clearanceLevel}
                onChange={e => setF({ ...f, clearanceLevel: Number(e.target.value) })}>
          {levels.map(l => <option key={l.level} value={l.level}>{l.level} — {l.label}</option>)}
        </select>
        <p className="xsmall muted">
          Người chỉ đọc được tài liệu có nhãn mật <strong>không cao hơn</strong> mức này, kể cả
          khi đã được cấp quyền trên tài liệu đó.
        </p>
      </div>

      <div className="field">
        <label htmlFor="u-role">Vai trò</label>
        <select id="u-role" value={f.role} onChange={e => setF({ ...f, role: e.target.value })}>
          <option value="NHAN_VIEN">Nhân viên</option>
          <option value="QUAN_TRI">Quản trị</option>
        </select>
        {f.role === 'QUAN_TRI' && (
          <p className="xsmall muted">
            Quản trị quản lý được tài liệu và tài khoản, nhưng <strong>không</strong> tự đọc
            được nội dung tài liệu — vẫn phải được cấp quyền như mọi người.
          </p>
        )}
      </div>

      <div className="field">
        <label>Phòng ban</label>
        {phongBan.length === 0 ? (
          <p className="xsmall muted">Chưa có phòng ban nào.</p>
        ) : (
          <div className="stack" style={{ gap: 'var(--s2)' }}>
            {phongBan.map(d => (
              <label key={d.id} className="row" style={{ gap: 'var(--s2)', cursor: 'pointer' }}>
                <input type="checkbox" checked={f.departmentIds.includes(d.id)}
                       onChange={() => doiPhong(d.id)} />
                <span>{d.name}</span>
              </label>
            ))}
          </div>
        )}
        <p className="xsmall muted">
          Quyền cấp cho phòng ban áp cho mọi thành viên, nên đây là cách mở quyền nhanh nhất.
        </p>
      </div>

      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose} disabled={busy}>Huỷ</button>
        <button className="btn btn-primary" disabled={!duDieuKien || busy} onClick={() => void luu()}>
          {busy ? 'Đang lưu…' : dangSua ? 'Lưu' : 'Tạo tài khoản'}
        </button>
      </div>
    </Modal>
  )
}

// ── Đặt lại mật khẩu ────────────────────────────────────────────────────────────

export function ResetPasswordDialog({ userId, username, onClose, onDone }: {
  userId: number; username: string; onClose: () => void; onDone: (tb: string) => void
}) {
  const [mk, setMk] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function dat() {
    setBusy(true); setError(null)
    try {
      await api.post(`/api/admin/users/${userId}/reset-password`, { newPassword: mk })
      onDone(`Đã đặt lại mật khẩu cho ${username}. Báo họ mật khẩu tạm này; hệ thống sẽ bắt đổi khi đăng nhập.`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không đặt lại được mật khẩu.')
      setBusy(false)
    }
  }

  return (
    <Modal onClose={onClose} rong={480}>
      <h3>Đặt lại mật khẩu — {username}</h3>
      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}
      <div className="field">
        <label htmlFor="rp">Mật khẩu tạm mới</label>
        <input id="rp" type="text" value={mk} autoComplete="off"
               onChange={e => setMk(e.target.value)} />
        <p className="xsmall muted">Ít nhất 12 ký tự. Người dùng bắt buộc đổi ở lần đăng nhập kế tiếp.</p>
      </div>
      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose} disabled={busy}>Huỷ</button>
        <button className="btn btn-primary" disabled={mk.length < 12 || busy} onClick={() => void dat()}>
          {busy ? 'Đang đặt…' : 'Đặt lại'}
        </button>
      </div>
    </Modal>
  )
}

// ── Tạo phòng ban ───────────────────────────────────────────────────────────────

export function DepartmentDialog({ onClose, onDone }: {
  onClose: () => void; onDone: (tb: string) => void
}) {
  const [ten, setTen] = useState('')
  const [moTa, setMoTa] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function tao() {
    setBusy(true); setError(null)
    try {
      await api.post('/api/admin/departments', { name: ten.trim(), description: moTa.trim() || null })
      onDone(`Đã tạo phòng ban “${ten.trim()}”.`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tạo được phòng ban.')
      setBusy(false)
    }
  }

  return (
    <Modal onClose={onClose} rong={480}>
      <h3>Tạo phòng ban</h3>
      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}
      <div className="field">
        <label htmlFor="d-ten">Tên phòng ban</label>
        <input id="d-ten" type="text" value={ten} onChange={e => setTen(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="d-mota">Mô tả (không bắt buộc)</label>
        <input id="d-mota" type="text" value={moTa} onChange={e => setMoTa(e.target.value)} />
      </div>
      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose} disabled={busy}>Huỷ</button>
        <button className="btn btn-primary" disabled={ten.trim().length === 0 || busy}
                onClick={() => void tao()}>
          {busy ? 'Đang tạo…' : 'Tạo'}
        </button>
      </div>
    </Modal>
  )
}

// ── Nhập tài khoản hàng loạt ────────────────────────────────────────────────────

interface ImportRow {
  username: string; fullName: string; email: string | null
  clearanceLevel: number | null; departments: string | null
}
interface PreviewRow { line: number; username: string; fullName: string; status: string; message: string }
interface Preview { total: number; valid: number; invalid: number; rows: PreviewRow[] }

/**
 * Nhập từ tệp CSV, <strong>xem trước rồi mới ghi</strong>.
 *
 * Hai bước không phải nghi thức: ghi thẳng một tệp trăm dòng rồi báo "có bảy dòng lỗi" là đẩy
 * người quản trị vào thế phải dọn hậu quả thủ công. Xem trước tách việc kiểm tra khỏi việc ghi.
 */
export function ImportUsersDialog({ onClose, onDone }: {
  onClose: () => void; onDone: (tb: string) => void
}) {
  const [text, setText] = useState('')
  const [preview, setPreview] = useState<Preview | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  /** Đọc CSV rất đơn giản: tách theo dấu phẩy, bỏ dòng trống, bỏ dòng tiêu đề nếu có. */
  function phanTich(): ImportRow[] {
    return text.split(/\r?\n/)
      .map(d => d.trim())
      .filter(d => d.length > 0)
      .filter((d, i) => !(i === 0 && /t[êe]n\s*đ[ăa]ng\s*nh[ậa]p|username/i.test(d)))
      .map(d => {
        const c = d.split(',').map(x => x.trim())
        const muc = c[3] ? Number(c[3]) : null
        return {
          username: c[0] ?? '',
          fullName: c[1] ?? '',
          email: c[2] || null,
          clearanceLevel: muc !== null && Number.isFinite(muc) ? muc : null,
          departments: c[4] || null,
        }
      })
  }

  async function xemTruoc() {
    setBusy(true); setError(null)
    try {
      setPreview(await api.post<Preview>('/api/admin/users/import/preview', phanTich()))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không xem trước được.')
    } finally { setBusy(false) }
  }

  async function ghiThat() {
    setBusy(true); setError(null)
    try {
      const kq = await api.post<{ created: number; skipped: number; messages: string[] }>(
        '/api/admin/users/import/commit', phanTich())
      onDone(`Đã tạo ${kq.created} tài khoản, bỏ qua ${kq.skipped} dòng.`
        + (kq.messages.length > 0 ? ` Lưu ý: ${kq.messages.join('; ')}` : ''))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không nhập được.')
      setBusy(false)
    }
  }

  return (
    <Modal onClose={onClose} rong={720}>
      <h3>Nhập tài khoản từ danh sách</h3>
      <p className="small muted">
        Mỗi dòng một người, các cột cách nhau bằng dấu phẩy:{' '}
        <code>tên đăng nhập, họ tên, email, mức mật, phòng ban</code>. Nhiều phòng ban thì ngăn
        bằng dấu chấm phẩy. Ba cột cuối bỏ trống được. Mật khẩu tạm dùng chung
        là <code>Doimatkhau@1</code> và ai cũng phải đổi ở lần đăng nhập đầu.
      </p>

      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      <div className="field">
        <label htmlFor="imp">Danh sách</label>
        <textarea id="imp" rows={8} value={text}
                  placeholder={'an.nguyen, Nguyễn Văn An, an@congty.vn, 2, Nhân sự\nbinh.tran, Trần Thị Bình, , 1, Kế toán;Hành chính'}
                  onChange={e => { setText(e.target.value); setPreview(null) }} />
      </div>

      {preview && (
        <div className="stack" style={{ gap: 'var(--s2)' }}>
          <div className={`notice ${preview.invalid > 0 ? 'notice-warning' : 'notice-success'}`}>
            <span className="notice-label">Xem trước</span>
            <span>
              {preview.total} dòng · <strong>{preview.valid} sẽ tạo</strong>
              {preview.invalid > 0 && <> · {preview.invalid} bị bỏ qua</>}
            </span>
          </div>
          <div className="table-wrap" style={{ maxHeight: 260, overflowY: 'auto' }}>
            <table>
              <thead><tr><th className="num">Dòng</th><th>Tên đăng nhập</th><th>Họ tên</th>
                <th>Kết quả</th></tr></thead>
              <tbody>
                {preview.rows.map(r => (
                  <tr key={r.line}>
                    <td className="num">{r.line}</td>
                    <td className="small">{r.username || <span className="muted">—</span>}</td>
                    <td className="small">{r.fullName || <span className="muted">—</span>}</td>
                    <td className="small">
                      {r.status === 'HOP_LE' ? <span className="tag tag-success">{r.message}</span>
                        : r.status === 'TRUNG' ? <span className="tag tag-warning">{r.message}</span>
                        : <span className="tag tag-error">{r.message}</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose} disabled={busy}>Đóng</button>
        <button className="btn" disabled={text.trim().length === 0 || busy}
                onClick={() => void xemTruoc()}>
          {busy && !preview ? 'Đang kiểm…' : 'Xem trước'}
        </button>
        <button className="btn btn-primary"
                disabled={!preview || preview.valid === 0 || busy}
                onClick={() => void ghiThat()}>
          {preview ? `Tạo ${preview.valid} tài khoản` : 'Tạo tài khoản'}
        </button>
      </div>
    </Modal>
  )
}
