import { useState, type FormEvent } from 'react'
import { api, ApiError, type Me } from '../lib/api'

/**
 * Màn đổi mật khẩu.
 *
 * Đổi xong thì **vào thẳng màn hỏi đáp, không bắt đăng nhập lại** — máy chủ dựng lại phiên
 * ngay trong lời gọi này. Bắt đăng nhập lại sau khi vừa đổi là bắt người dùng gõ lại đúng mật
 * khẩu họ vừa đặt, và đó là một bước thừa mà ai cũng thấy phiền.
 */
export function ChangePasswordPage({ me, onChanged, onSignOut }: {
  me: Me
  onChanged: (me: Me) => void
  onSignOut: () => void
}) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const mismatch = confirm.length > 0 && next !== confirm
  const tooWeak = next.length > 0 && !meetsPolicy(next)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (mismatch || tooWeak) return
    setError(null)
    setBusy(true)
    try {
      const updated = await api.post<Me>('/api/auth/change-password', {
        currentPassword: current, newPassword: next,
      })
      onChanged(updated)
    } catch (err) {
      setError(err instanceof ApiError ? err.message
        : 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ minHeight: '100%', display: 'grid', placeItems: 'center', padding: 'var(--s5)' }}>
      <div style={{ width: '100%', maxWidth: 440 }}>
        <div style={{ textAlign: 'center', marginBottom: 'var(--s5)' }}>
          <h1>Đổi mật khẩu</h1>
        </div>

        <form className="card" onSubmit={submit} noValidate>
          <div className="notice notice-info">
            <span className="notice-label">Cần đổi</span>
            <span>
              Tài khoản <strong>{me.username}</strong> đang dùng mật khẩu tạm do quản trị đặt.
              Đặt mật khẩu riêng của bạn để tiếp tục.
            </span>
          </div>

          {error && (
            <div className="notice notice-error" role="alert">
              <span className="notice-label">Chưa đổi được</span>
              <span>{error}</span>
            </div>
          )}

          <div className="field">
            <label htmlFor="current">Mật khẩu hiện tại</label>
            <input id="current" type="password" autoComplete="current-password"
                   value={current} onChange={e => setCurrent(e.target.value)} autoFocus required />
          </div>

          <div className="field">
            <label htmlFor="next">Mật khẩu mới</label>
            <input id="next" type="password" autoComplete="new-password"
                   aria-invalid={tooWeak} aria-describedby="policy"
                   value={next} onChange={e => setNext(e.target.value)} required />
            <span id="policy" className={tooWeak ? 'hint' : 'hint'}
                  style={{ color: tooWeak ? 'var(--error)' : undefined }}>
              Ít nhất 8 ký tự, có cả chữ và số.
            </span>
          </div>

          <div className="field">
            <label htmlFor="confirm">Nhập lại mật khẩu mới</label>
            <input id="confirm" type="password" autoComplete="new-password"
                   aria-invalid={mismatch}
                   value={confirm} onChange={e => setConfirm(e.target.value)} required />
            {mismatch && (
              <span className="hint" style={{ color: 'var(--error)' }}>
                Hai lần nhập chưa khớp nhau.
              </span>
            )}
          </div>

          <button className="btn btn-primary" style={{ width: '100%' }} type="submit"
                  disabled={busy || !current || !next || mismatch || tooWeak}>
            {busy ? <><span className="spinner" /> Đang lưu…</> : 'Đổi mật khẩu và tiếp tục'}
          </button>

          <button className="btn btn-ghost" style={{ width: '100%', marginTop: 'var(--s2)' }}
                  type="button" onClick={onSignOut}>
            Đăng xuất
          </button>
        </form>
      </div>
    </div>
  )
}

/** Cùng một luật với máy chủ. Kiểm ở đây chỉ để báo sớm, máy chủ vẫn là nơi quyết định. */
function meetsPolicy(password: string): boolean {
  return password.length >= 8
      && /\d/.test(password)
      && /[a-zA-ZÀ-ỹ]/.test(password)
}
