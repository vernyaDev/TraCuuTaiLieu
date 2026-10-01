import type { Citation } from '../lib/api'

/**
 * Khối nguồn dưới câu trả lời.
 *
 * Trích dẫn phải chỉ tới **đúng đoạn**, không chỉ tới tên tài liệu — đó là lý do đường dẫn
 * phân cấp (*Chương VII › Điều 113 › Khoản 2*) được hiện ngay cạnh tên. Không có nó, người
 * đọc phải mở tệp gốc rồi tự dò, và giá trị của trích dẫn gần như mất hết.
 *
 * Khối này phân biệt với nội dung bằng **nền và viền, không bằng cỡ chữ** — trích dẫn là thứ
 * người đọc cần đọc kỹ chứ không phải chú thích cuối trang.
 */
export function CitationList({ citations, onOpen }: {
  citations: Citation[]
  onOpen: (chunkId: number) => void
}) {
  return (
    <section aria-label="Nguồn trích dẫn" className="stack" style={{ gap: 'var(--s2)' }}>
      <h4 className="xsmall muted" style={{ margin: 0, fontWeight: 600 }}>
        Nguồn ({citations.length})
      </h4>

      {citations.map(c => (
        <button
          key={c.chunkId}
          type="button"
          onClick={() => onOpen(c.chunkId)}
          className="measure"
          style={{
            textAlign: 'left', font: 'inherit', cursor: 'pointer',
            background: 'var(--bg-subtle)',
            border: '1px solid var(--border)',
            borderLeft: '3px solid var(--accent)',
            borderRadius: 'var(--radius-sm)',
            padding: 'var(--s3) var(--s4)',
            width: '100%',
          }}
        >
          <div className="row" style={{ gap: 'var(--s2)', alignItems: 'baseline' }}>
            <span className="tag tag-accent">{c.rank}</span>
            <strong style={{ fontSize: 'var(--text-sm)' }}>{c.documentTitle}</strong>
          </div>

          {c.breadcrumb && (
            <div className="xsmall muted" style={{ marginTop: 'var(--s1)' }}>
              {c.breadcrumb}
            </div>
          )}

          <p className="small" style={{
            margin: 'var(--s2) 0 0', color: 'var(--text-secondary)',
          }}>
            {c.excerpt}
          </p>

          <span className="xsmall" style={{ color: 'var(--accent)' }}>
            Mở đoạn này trong tài liệu →
          </span>
        </button>
      ))}
    </section>
  )
}
