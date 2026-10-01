import { useEffect, useState } from 'react'
import { api, ApiError, type Me } from './lib/api'
import { LoginPage } from './pages/LoginPage'
import { ChangePasswordPage } from './pages/ChangePasswordPage'
import { ChatPage } from './pages/ChatPage'
import { AdminDocumentsPage } from './pages/AdminDocumentsPage'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { AdminOverlapsPage } from './pages/AdminOverlapsPage'
import { AdminGapsPage } from './pages/AdminGapsPage'

type Route = 'chat' | 'documents' | 'users' | 'overlaps' | 'gaps'

const ROUTES: { key: Route; label: string; adminOnly: boolean }[] = [
  { key: 'chat',      label: 'Hỏi đáp',        adminOnly: false },
  { key: 'documents', label: 'Tài liệu',       adminOnly: true },
  { key: 'users',     label: 'Tài khoản',      adminOnly: true },
  { key: 'overlaps',  label: 'Chồng lấn',      adminOnly: true },
  { key: 'gaps',      label: 'Khoảng trống',   adminOnly: true },
]

/**
 * Gốc ứng dụng: giữ danh tính phiên và quyết định hiện màn nào.
 *
 * Ba trạng thái của phiên được tách rõ, vì mỗi trạng thái cần một màn khác nhau và gộp chúng
 * là nguồn của màn trắng khó chẩn đoán:
 *
 * 1. **Chưa biết** — đang hỏi máy chủ xem phiên còn sống không. Phải vẽ, không được để trống.
 * 2. **Chưa đăng nhập** — màn đăng nhập.
 * 3. **Đã đăng nhập nhưng còn mật khẩu tạm** — màn đổi mật khẩu, chưa cho vào hỏi đáp.
 */
export function App() {
  const [me, setMe] = useState<Me | null>(null)
  const [checking, setChecking] = useState(true)
  const [route, setRoute] = useState<Route>('chat')

  // Hỏi lại danh tính khi tải trang: phiên nằm ở cookie nên tải lại trang vẫn còn đăng nhập,
  // và bắt người dùng đăng nhập lại sau mỗi lần làm mới là một phiền toái không cần thiết.
  useEffect(() => {
    api.get<Me>('/api/auth/me')
      .then(setMe)
      .catch(err => {
        // Còn mật khẩu tạm thì máy chủ chặn mọi đường khác, nhưng /me vẫn qua được —
        // nên rơi vào đây chỉ có nghĩa là chưa đăng nhập.
        if (!(err instanceof ApiError) || err.isUnauthenticated) setMe(null)
      })
      .finally(() => setChecking(false))
  }, [])

  async function signOut() {
    try { await api.post('/api/auth/logout') } catch { /* đăng xuất cục bộ dù sao cũng được */ }
    setMe(null)
    setRoute('chat')
  }

  if (checking) {
    return (
      <div className="state" style={{ minHeight: '100%' }}>
        <span className="spinner" />
        <p>Đang kiểm tra phiên đăng nhập…</p>
      </div>
    )
  }

  if (!me) return <LoginPage onSignedIn={setMe} />

  if (me.mustChangePassword) {
    return <ChangePasswordPage me={me} onChanged={setMe} onSignOut={() => void signOut()} />
  }

  const visible = ROUTES.filter(r => !r.adminOnly || me.admin)
  const active = visible.some(r => r.key === route) ? route : 'chat'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <header style={{
        background: 'var(--bg-surface)', borderBottom: '1px solid var(--border)',
        flexShrink: 0,
      }}>
        <div className="container row-between wrap" style={{ paddingBlock: 'var(--s3)' }}>
          <div className="row wrap" style={{ gap: 'var(--s4)' }}>
            <strong>Tra cứu tri thức</strong>
            <nav className="row" aria-label="Điều hướng chính">
              {visible.map(r => (
                <button
                  key={r.key}
                  className={`btn btn-sm ${active === r.key ? 'btn-primary' : 'btn-ghost'}`}
                  aria-current={active === r.key ? 'page' : undefined}
                  onClick={() => setRoute(r.key)}
                >
                  {r.label}
                </button>
              ))}
            </nav>
          </div>

          <div className="row" style={{ gap: 'var(--s3)' }}>
            <div className="xsmall" style={{ textAlign: 'right', lineHeight: 1.3 }}>
              <div style={{ fontWeight: 500 }}>{me.fullName}</div>
              <div className="muted">
                {me.clearanceLabel}
                {me.departments.length > 0 && ` · ${me.departments.join(', ')}`}
              </div>
            </div>
            <button className="btn btn-sm btn-ghost" onClick={() => void signOut()}>
              Đăng xuất
            </button>
          </div>
        </div>
      </header>

      <main style={{ flex: 1, minHeight: 0, overflow: active === 'chat' ? 'hidden' : 'auto' }}>
        {active === 'chat' ? <ChatPage /> : (
          <div className="container" style={{ paddingBlock: 'var(--s6)' }}>
            {active === 'documents' && <AdminDocumentsPage />}
            {active === 'users'     && <AdminUsersPage />}
            {active === 'overlaps'  && <AdminOverlapsPage />}
            {active === 'gaps'      && <AdminGapsPage />}
          </div>
        )}
      </main>
    </div>
  )
}
