import { useEffect, useRef, useState } from 'react'
import { api, ApiError } from '../lib/api'

interface ChunkView {
  id: number
  ordinal: number
  breadcrumb: string | null
  content: string
  table: boolean
  tablePart: number | null
  tablePartsTotal: number | null
  highlighted: boolean
}

interface DocumentView {
  id: number
  title: string
  originalFilename: string
  category: string | null
  effectiveFrom: string | null
  effectiveTo: string | null
  hasNoExpiry: boolean
  expired: boolean
  chunks: ChunkView[]
}

/**
 * Mở tài liệu gốc quanh một đoạn được trích dẫn.
 *
 * Quyền được kiểm **tại thời điểm mở**, không dựa vào việc trích dẫn đã hiện trước đó. Một
 * trích dẫn trong lịch sử hội thoại từ hôm qua không phải giấy phép đọc hôm nay — quyền có
 * thể đã bị thu hồi trong khoảng giữa. Vì vậy máy chủ có thể trả *không tìm thấy* ở đây dù
 * trích dẫn vẫn hiện trên màn hình, và giao diện phải nói điều đó cho tử tế thay vì báo lỗi kỹ thuật.
 */
export function DocumentViewer({ chunkId, onClose }: { chunkId: number; onClose: () => void }) {
  const [doc, setDoc] = useState<DocumentView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const closeRef = useRef<HTMLButtonElement>(null)
  const highlightRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let alive = true
    setLoading(true)
    setError(null)

    api.get<DocumentView>(`/api/documents/by-chunk/${chunkId}`)
      .then(d => { if (alive) { setDoc(d); setLoading(false) } })
      .catch(err => {
        if (!alive) return
        setError(err instanceof ApiError ? err.message
          : 'Không mở được tài liệu. Thử lại sau ít phút.')
        setLoading(false)
      })

    return () => { alive = false }
  }, [chunkId])

  useEffect(() => { closeRef.current?.focus() }, [])

  useEffect(() => {
    if (doc) highlightRef.current?.scrollIntoView({ block: 'center' })
  }, [doc])

  useEffect(() => {
    function onKey(e: KeyboardEvent) { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div
      role="dialog" aria-modal="true" aria-label="Nội dung tài liệu"
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
      style={{
        position: 'fixed', inset: 0, background: 'rgba(22, 33, 43, .45)',
        display: 'flex', justifyContent: 'flex-end', zIndex: 50,
      }}
    >
      <div style={{
        background: 'var(--bg-surface)', width: 'min(680px, 100%)', height: '100%',
        display: 'flex', flexDirection: 'column', boxShadow: 'var(--shadow-lg)',
      }}>
        <header style={{
          padding: 'var(--s4) var(--s5)', borderBottom: '1px solid var(--border)',
          display: 'flex', gap: 'var(--s3)', alignItems: 'flex-start',
        }}>
          <div className="grow">
            <h3>{doc?.title ?? 'Đang mở tài liệu'}</h3>
            {doc && (
              <div className="row wrap xsmall muted" style={{ marginTop: 'var(--s1)' }}>
                {doc.category && <span className="tag">{doc.category}</span>}
                {doc.expired && <span className="tag tag-warning">Đã hết hiệu lực</span>}
                {doc.hasNoExpiry && <span className="tag">Không có thời hạn</span>}
                {doc.effectiveTo && !doc.hasNoExpiry && (
                  <span>Hiệu lực đến {doc.effectiveTo}</span>
                )}
              </div>
            )}
          </div>
          <button ref={closeRef} className="btn btn-sm btn-ghost" onClick={onClose}
                  aria-label="Đóng">Đóng</button>
        </header>

        <div style={{ overflowY: 'auto', padding: 'var(--s5)' }}>
          {loading && (
            <div className="state">
              <span className="spinner" />
              <p>Đang mở tài liệu…</p>
            </div>
          )}

          {error && (
            <div className="state">
              <h3>Không mở được</h3>
              <p>{error}</p>
            </div>
          )}

          {doc && doc.chunks.map(c => (
            <div
              key={c.id}
              ref={c.highlighted ? highlightRef : undefined}
              style={{
                marginBottom: 'var(--s4)',
                padding: 'var(--s3) var(--s4)',
                borderRadius: 'var(--radius-sm)',
                // Đoạn được trích dẫn nổi bật bằng nền VÀ viền trái, không chỉ bằng màu —
                // người mù màu vẫn phải nhận ra được đoạn nào đang được chỉ tới.
                background: c.highlighted ? 'var(--accent-soft)' : 'transparent',
                borderLeft: c.highlighted
                  ? '3px solid var(--accent)' : '3px solid transparent',
              }}
            >
              {c.breadcrumb && (
                <div className="xsmall muted" style={{ marginBottom: 'var(--s2)' }}>
                  {c.breadcrumb}
                </div>
              )}
              {c.table && c.tablePart && (
                <div className="xsmall muted" style={{ marginBottom: 'var(--s2)' }}>
                  Bảng — mảnh {c.tablePart}/{c.tablePartsTotal}, đã lặp dòng tiêu đề
                </div>
              )}
              <div style={{
                whiteSpace: 'pre-wrap',
                fontFamily: c.table ? 'var(--font-mono)' : undefined,
                fontSize: c.table ? 'var(--text-sm)' : undefined,
                overflowX: c.table ? 'auto' : undefined,
              }}>
                {c.content}
              </div>
            </div>
          ))}
        </div>

        {doc && (
          <footer style={{
            padding: 'var(--s3) var(--s5)', borderTop: '1px solid var(--border)',
          }}>
            <a className="btn btn-sm" href={`/api/documents/${doc.id}/file`}>
              Tải tệp gốc ({doc.originalFilename})
            </a>
          </footer>
        )}
      </div>
    </div>
  )
}
