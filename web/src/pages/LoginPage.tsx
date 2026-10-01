import { useState, type FormEvent } from 'react'
import { api, ApiError, type Me } from '../lib/api'

/**
 * Màn đăng nhập.
 *
 * Câu báo lỗi cố ý **không phân biệt sai tên đăng nhập với sai mật khẩu** — phân biệt hai ca
 * ấy là xác nhận tài khoản có tồn tại, tức biến màn này thành công cụ dò danh sách nhân sự.
 * Máy chủ trả về đúng một câu cho cả hai; phần giao diện chỉ hiển thị lại.
 *
 * Tài khoản bị vô hiệu hoá là ngoại lệ có chủ đích và có câu riêng: người bị vô hiệu hoá cần
 * biết để đi hỏi quản trị, và việc tài khoản từng tồn tại thì chính họ đã biết.
 */
export function LoginPage({ onSignedIn }: { onSignedIn: (me: Me) => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      const me = await api.post<Me>('/api/auth/login', { username, password })
      onSignedIn(me)
    } catch (err) {
      setError(err instanceof ApiError ? err.message
        : 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{
      minHeight: '100%', display: 'grid', placeItems: 'center', padding: 'var(--s5)',
    }}>
      {/* Form không trải rộng hết khung: khối hẹp căn giữa đọc dễ hơn và giống màn thật. */}
      <div style={{ width: '100%', maxWidth: 420 }}>
        <div style={{ textAlign: 'center', marginBottom: 'var(--s5)' }}>
          <h1>Tra cứu tri thức nội bộ</h1>
          <p className="muted small" style={{ marginTop: 'var(--s2)' }}>
            Hỏi bằng tiếng Việt, nhận câu trả lời kèm trích dẫn tài liệu bạn được phép đọc.
          </p>
        </div>

        <form className="card" onSubmit={submit} noValidate>
          {error && (
            <div className="notice notice-error" role="alert">
              <span className="notice-label">Không đăng nhập được</span>
              <span>{error}</span>
            </div>
          )}

          <div className="field">
            <label htmlFor="username">Tên đăng nhập</label>
            <input
              id="username" name="username" type="text" autoComplete="username"
              value={username} onChange={e => setUsername(e.target.value)}
              autoFocus required
            />
          </div>

          <div className="field">
            <label htmlFor="password">Mật khẩu</label>
            <input
              id="password" name="password" type="password" autoComplete="current-password"
              value={password} onChange={e => setPassword(e.target.value)}
              required
            />
          </div>

          <button className="btn btn-primary" style={{ width: '100%' }}
                  type="submit" disabled={busy || !username || !password}>
            {busy ? <><span className="spinner" /> Đang kiểm tra…</> : 'Đăng nhập'}
          </button>

          <p className="xsmall muted" style={{ marginTop: 'var(--s4)', marginBottom: 0 }}>
            Quên mật khẩu thì liên hệ quản trị hệ thống để được đặt lại.
          </p>
        </form>
      </div>
    </div>
  )
}
