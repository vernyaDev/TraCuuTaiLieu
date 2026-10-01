import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, type Page } from '../lib/api'
import { DepartmentDialog, ImportUsersDialog, ResetPasswordDialog, UserDialog }
  from '../components/UserForms'

interface UserView {
  id: number; username: string; fullName: string; email: string | null
  role: string; clearanceLevel: number; clearanceLabel: string
  active: boolean; mustChangePassword: boolean
  departments: string[]; lastLoginAt: string | null
}

interface DepartmentView {
  id: number; name: string; description: string | null
  memberCount: number; aclCount: number
}

export function AdminUsersPage() {
  const [tab, setTab] = useState<'users' | 'departments'>('users')
  return (
    <div className="stack">
      <h1>Tài khoản và phòng ban</h1>
      <div className="row" role="tablist">
        <button role="tab" aria-selected={tab === 'users'}
                className={`btn ${tab === 'users' ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => setTab('users')}>Tài khoản</button>
        <button role="tab" aria-selected={tab === 'departments'}
                className={`btn ${tab === 'departments' ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => setTab('departments')}>Phòng ban</button>
      </div>
      {tab === 'users' ? <UsersTab /> : <DepartmentsTab />}
    </div>
  )
}

function UsersTab() {
  const [rows, setRows] = useState<UserView[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [dangTao, setDangTao] = useState(false)
  const [dangSua, setDangSua] = useState<UserView | null>(null)
  const [datLaiMk, setDatLaiMk] = useState<UserView | null>(null)
  const [dangNhapTep, setDangNhapTep] = useState(false)
  const [thongBao, setThongBao] = useState<string | null>(null)

  function xong(tb: string) {
    setDangTao(false); setDangSua(null); setDatLaiMk(null); setDangNhapTep(false)
    setThongBao(tb)
    window.setTimeout(() => setThongBao(null), 10000)
    void load()
  }

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const params = new URLSearchParams({ page: String(page), size: '20' })
      if (q.trim()) params.set('q', q.trim())
      const data = await api.get<Page<UserView>>(`/api/admin/users?${params}`)
      setRows(data.content); setTotal(data.totalElements)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được danh sách tài khoản.')
    } finally { setLoading(false) }
  }, [page, q])

  useEffect(() => { void load() }, [load])

  async function toggleActive(u: UserView) {
    try {
      await api.post(`/api/admin/users/${u.id}/${u.active ? 'deactivate' : 'activate'}`)
      void load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không đổi được trạng thái tài khoản.')
    }
  }

  return (
    <div className="stack">
      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      {thongBao && <div className="notice notice-success" role="status">
        <span className="notice-label">Xong</span><span>{thongBao}</span></div>}

      <div className="card">
        <div className="row-between wrap" style={{ gap: 'var(--s3)' }}>
          <div className="field grow" style={{ marginBottom: 0, maxWidth: 360, minWidth: 220 }}>
            <label htmlFor="uq">Tìm theo tên hoặc tên đăng nhập</label>
            <input id="uq" type="text" value={q}
                   onChange={e => { setQ(e.target.value); setPage(0) }} />
          </div>
          <div className="row">
            <button className="btn" onClick={() => setDangNhapTep(true)}>Nhập từ danh sách</button>
            <button className="btn btn-primary" onClick={() => setDangTao(true)}>Tạo tài khoản</button>
          </div>
        </div>
      </div>

      {loading ? <div className="state"><span className="spinner" /><p>Đang tải…</p></div>
        : rows.length === 0 ? (
          <div className="state">
            <h3>Không có tài khoản nào khớp</h3>
            <p>Thử bỏ bớt từ khoá tìm kiếm.</p>
          </div>
        ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Họ tên</th><th>Tên đăng nhập</th><th>Vai trò</th>
                  <th>Mức mật</th><th>Phòng ban</th><th>Trạng thái</th><th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map(u => (
                  <tr key={u.id}>
                    <td style={{ fontWeight: 500 }}>{u.fullName}</td>
                    <td className="small">{u.username}</td>
                    <td>
                      <span className={`tag ${u.role === 'QUAN_TRI' ? 'tag-accent' : ''}`}>
                        {u.role === 'QUAN_TRI' ? 'Quản trị' : 'Nhân viên'}
                      </span>
                    </td>
                    <td><span className="tag">{u.clearanceLabel}</span></td>
                    <td className="small">
                      {u.departments.length > 0
                        ? u.departments.join(', ')
                        : <span className="muted">Chưa thuộc phòng ban nào</span>}
                    </td>
                    <td>
                      <div className="row" style={{ gap: 'var(--s2)' }}>
                        {u.active
                          ? <span className="tag tag-success">Đang dùng</span>
                          : <span className="tag tag-error">Đã vô hiệu hoá</span>}
                        {u.mustChangePassword && (
                          <span className="tag tag-warning">Mật khẩu tạm</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <div className="row" style={{ gap: 'var(--s2)' }}>
                        <button className="btn btn-sm btn-ghost"
                                onClick={() => setDangSua(u)}>Sửa</button>
                        <button className="btn btn-sm btn-ghost"
                                onClick={() => setDatLaiMk(u)}>Đặt lại mật khẩu</button>
                        <button className="btn btn-sm btn-ghost" onClick={() => void toggleActive(u)}>
                          {u.active ? 'Vô hiệu hoá' : 'Mở lại'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="row-between small muted">
            <span>{total} tài khoản</span>
            <div className="row">
              <button className="btn btn-sm" disabled={page === 0}
                      onClick={() => setPage(p => p - 1)}>Trang trước</button>
              <span>Trang {page + 1}</span>
              <button className="btn btn-sm" disabled={(page + 1) * 20 >= total}
                      onClick={() => setPage(p => p + 1)}>Trang sau</button>
            </div>
          </div>
        </>
      )}

      {(dangTao || dangSua) && (
        <UserDialog dangSua={dangSua} onClose={() => { setDangTao(false); setDangSua(null) }}
                    onDone={xong} />
      )}
      {datLaiMk && (
        <ResetPasswordDialog userId={datLaiMk.id} username={datLaiMk.username}
                             onClose={() => setDatLaiMk(null)} onDone={xong} />
      )}
      {dangNhapTep && (
        <ImportUsersDialog onClose={() => setDangNhapTep(false)} onDone={xong} />
      )}
    </div>
  )
}

function DepartmentsTab() {
  const [rows, setRows] = useState<DepartmentView[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [confirming, setConfirming] = useState<DepartmentView | null>(null)
  const [dangTao, setDangTao] = useState(false)
  const [thongBao, setThongBao] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      setRows(await api.get<DepartmentView[]>('/api/admin/departments'))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được danh sách phòng ban.')
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { void load() }, [load])

  async function remove(d: DepartmentView) {
    try {
      await api.del(`/api/admin/departments/${d.id}`)
      setConfirming(null)
      void load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không xoá được phòng ban.')
    }
  }

  if (loading) return <div className="state"><span className="spinner" /><p>Đang tải…</p></div>

  return (
    <div className="stack">
      {error && <div className="notice notice-error" role="alert">
        <span className="notice-label">Lỗi</span><span>{error}</span></div>}

      {thongBao && <div className="notice notice-success" role="status">
        <span className="notice-label">Xong</span><span>{thongBao}</span></div>}

      <div className="row-between wrap">
        <p className="small muted" style={{ margin: 0 }}>
          Cấp quyền cho cả phòng ban là cách mở quyền nhanh nhất: mọi thành viên được hưởng ngay,
          và người vào sau cũng tự có.
        </p>
        <button className="btn btn-primary" onClick={() => setDangTao(true)}>Tạo phòng ban</button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Phòng ban</th><th>Mô tả</th><th className="num">Thành viên</th>
                <th className="num">Quyền cấp qua</th><th></th></tr>
          </thead>
          <tbody>
            {rows.map(d => (
              <tr key={d.id}>
                <td style={{ fontWeight: 500 }}>{d.name}</td>
                <td className="small muted">{d.description ?? '—'}</td>
                <td className="num">{d.memberCount}</td>
                <td className="num">{d.aclCount}</td>
                <td>
                  <button className="btn btn-sm btn-danger" onClick={() => setConfirming(d)}>
                    Xoá
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {dangTao && (
        <DepartmentDialog onClose={() => setDangTao(false)}
                          onDone={tb => {
                            setDangTao(false); setThongBao(tb)
                            window.setTimeout(() => setThongBao(null), 8000)
                            void load()
                          }} />
      )}

      {/*
        Hộp xác nhận mang CON SỐ HẬU QUẢ thật, không phải câu hỏi chung chung. "Bạn có chắc
        không" mà không kèm số thì người quản trị bấm đồng ý theo phản xạ.
      */}
      {confirming && (
        <div role="dialog" aria-modal="true"
             style={{ position: 'fixed', inset: 0, background: 'rgba(22,33,43,.45)',
                      display: 'grid', placeItems: 'center', zIndex: 50 }}>
          <div className="card" style={{ maxWidth: 480, margin: 'var(--s5)' }}>
            <h3>Xoá phòng ban “{confirming.name}”?</h3>
            <div className="notice notice-warning" style={{ marginTop: 'var(--s4)' }}>
              <span className="notice-label">Hệ quả</span>
              <span>
                {confirming.memberCount} người sẽ mất liên kết phòng ban, và{' '}
                <strong>{confirming.aclCount} mục quyền</strong> cấp qua phòng ban này sẽ bị gỡ.
                Những người chỉ đọc được tài liệu nhờ phòng ban này sẽ mất quyền ngay.
              </span>
            </div>
            <div className="row" style={{ justifyContent: 'flex-end' }}>
              <button className="btn" onClick={() => setConfirming(null)}>Huỷ</button>
              <button className="btn btn-danger" onClick={() => void remove(confirming)}>
                Xoá phòng ban
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
