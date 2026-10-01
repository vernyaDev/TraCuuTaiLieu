import { useEffect, useRef, useState, type FormEvent } from 'react'
import { ask } from '../lib/ask'
import { api, type Citation, type PlanItem, type Warning } from '../lib/api'
import { CitationList } from '../components/CitationList'
import { WarningList } from '../components/WarningList'
import { DocumentViewer } from '../components/DocumentViewer'

/** Một lượt trên màn hình. Lượt đang chạy và lượt đã xong dùng chung kiểu này. */
interface TurnState {
  id: number
  question: string
  rewritten?: string
  plan: PlanItem[]
  warnings: Warning[]
  answer: string
  citations: Citation[]
  refusal?: { stage: string; message: string }
  error?: { code: string; message: string }
  status?: string
  queuePosition?: number
  running: boolean
  firstTokenMs?: number
  totalMs?: number
}

export function ChatPage() {
  const [turns, setTurns] = useState<TurnState[]>([])
  const [question, setQuestion] = useState('')
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [openChunkId, setOpenChunkId] = useState<number | null>(null)
  const abortRef = useRef<AbortController | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)
  const running = turns.some(t => t.running)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [turns])

  function update(id: number, patch: Partial<TurnState> | ((t: TurnState) => Partial<TurnState>)) {
    setTurns(prev => prev.map(t =>
      t.id === id ? { ...t, ...(typeof patch === 'function' ? patch(t) : patch) } : t))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    const text = question.trim()
    if (!text || running) return

    const id = Date.now()
    setTurns(prev => [...prev, {
      id, question: text, plan: [], warnings: [], answer: '', citations: [], running: true,
    }])
    setQuestion('')

    // Mở hội thoại TRƯỚC khi hỏi câu đầu tiên. Luồng sự kiện không mang mã hội thoại về,
    // nên không có bước này thì mọi câu đều là câu đầu và tính năng hỏi tiếp nối chết lặng.
    let cid = conversationId
    if (cid === null) {
      try {
        const created = await api.post<{ id: number }>('/api/qa/conversations')
        cid = created.id
        setConversationId(cid)
      } catch {
        update(id, {
          running: false,
          error: { code: 'LOI_HE_THONG',
                   message: 'Không mở được phiên hội thoại. Tải lại trang rồi thử lại.' },
        })
        return
      }
    }

    abortRef.current = ask(text, cid, {
      onQueued: (position, ahead) => update(id, { queuePosition: position,
        status: ahead > 0
          ? `Đang xếp hàng, còn khoảng ${ahead} câu phía trước`
          : 'Đang xếp hàng' }),
      onStatus: step => update(id, { status: step, queuePosition: undefined }),
      onRewritten: q => update(id, { rewritten: q }),
      onPlan: steps => update(id, { plan: steps }),
      onWarning: w => update(id, t => ({ warnings: [...t.warnings, w] })),
      onToken: text => update(id, t => ({ answer: t.answer + text, status: undefined })),
      onCitations: items => update(id, { citations: items }),

      // Từ chối ở tầng hai đến SAU khi chữ đã hiện. Xoá phần đã hiện là bắt buộc:
      // một lượt hoặc trả lời hoặc từ chối, không có lượt trả lời nửa vời rồi rút lại.
      onRefusal: (stage, message) => update(id, {
        refusal: { stage, message }, answer: '', citations: [], status: undefined,
      }),

      onError: (code, message) => update(id, {
        error: { code, message }, running: false, status: undefined,
      }),
      onDone: (_turnId, firstTokenMs, totalMs) =>
        update(id, { running: false, status: undefined, firstTokenMs, totalMs }),
    })
  }

  function stop() {
    abortRef.current?.abort()
    setTurns(prev => prev.map(t => t.running
      ? { ...t, running: false, status: undefined,
          error: { code: 'DA_DUNG', message: 'Bạn đã dừng câu hỏi này.' } }
      : t))
  }

  function newConversation() {
    abortRef.current?.abort()
    setTurns([])
    setConversationId(null)
    setQuestion('')
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="container grow" style={{ overflowY: 'auto', paddingBlock: 'var(--s5)' }}>
        {turns.length === 0 ? <EmptyState /> : (
          <div className="stack" style={{ gap: 'var(--s6)' }}>
            {turns.map(t => (
              <TurnView key={t.id} turn={t} onOpenCitation={setOpenChunkId} />
            ))}
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div style={{
        borderTop: '1px solid var(--border)', background: 'var(--bg-surface)',
        paddingBlock: 'var(--s4)',
      }}>
        <form className="container" onSubmit={submit}>
          <div className="row" style={{ alignItems: 'flex-end' }}>
            <div className="field grow" style={{ marginBottom: 0 }}>
              <label htmlFor="question" className="sr-only">Câu hỏi</label>
              <textarea
                id="question" rows={2} value={question}
                placeholder="Ví dụ: thủ tục xin nghỉ phép dài ngày gồm những bước nào?"
                onChange={e => setQuestion(e.target.value)}
                onKeyDown={e => {
                  // Enter gửi, Shift+Enter xuống dòng — quen thuộc với người dùng.
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    submit(e as unknown as FormEvent)
                  }
                }}
                style={{ resize: 'vertical', minHeight: 56 }}
              />
            </div>
            {running
              ? <button className="btn" type="button" onClick={stop}>Dừng</button>
              : <button className="btn btn-primary" type="submit" disabled={!question.trim()}>
                  Gửi câu hỏi
                </button>}
            <button className="btn btn-ghost" type="button" onClick={newConversation}
                    disabled={turns.length === 0}>
              Hội thoại mới
            </button>
          </div>
          <p className="xsmall muted" style={{ margin: 'var(--s2) 0 0' }}>
            Câu trả lời chỉ dựa trên tài liệu bạn được cấp quyền đọc. Nhấn Enter để gửi,
            Shift+Enter để xuống dòng.
          </p>
        </form>
      </div>

      {openChunkId !== null && (
        <DocumentViewer chunkId={openChunkId} onClose={() => setOpenChunkId(null)} />
      )}
    </div>
  )
}

function EmptyState() {
  return (
    <div className="state">
      <h3>Bạn muốn tra cứu điều gì?</h3>
      <p>
        Đặt câu hỏi bằng tiếng Việt về quy chế, quy trình hoặc biểu mẫu nội bộ.
        Mỗi câu trả lời đều kèm trích dẫn để bạn mở đúng đoạn trong tài liệu gốc.
      </p>
      <div className="stack small" style={{ gap: 'var(--s2)', color: 'var(--text-secondary)' }}>
        <span>Thử: “Nghỉ phép năm được bao nhiêu ngày?”</span>
        <span>Thử: “Quy trình duyệt chi phí công tác gồm những bước nào?”</span>
      </div>
    </div>
  )
}

function TurnView({ turn, onOpenCitation }: {
  turn: TurnState
  onOpenCitation: (chunkId: number) => void
}) {
  return (
    <div className="stack" style={{ gap: 'var(--s3)' }}>
      {/* Câu hỏi của người dùng */}
      <div style={{
        alignSelf: 'flex-end', maxWidth: '85%',
        background: 'var(--accent-soft)', border: '1px solid var(--accent-border)',
        borderRadius: 'var(--radius)', padding: 'var(--s3) var(--s4)',
      }}>
        {turn.question}
      </div>

      {turn.rewritten && (
        <p className="xsmall muted measure" style={{ margin: 0 }}>
          Đã hiểu thành câu đầy đủ: <em>{turn.rewritten}</em>
        </p>
      )}

      {turn.plan.length > 1 && (
        <details className="card" style={{ padding: 'var(--s3) var(--s4)' }}>
          <summary className="small" style={{ cursor: 'pointer', fontWeight: 500 }}>
            Các bước đã thực hiện ({turn.plan.length})
          </summary>
          <ol className="small muted" style={{ margin: 'var(--s2) 0 0', paddingLeft: 'var(--s5)' }}>
            {turn.plan.map(step => (
              <li key={step.ordinal}>
                {step.text}
                {step.dependsOn != null && (
                  <span className="xsmall"> (dùng kết quả bước {step.dependsOn})</span>
                )}
              </li>
            ))}
          </ol>
        </details>
      )}

      {turn.warnings.length > 0 && <WarningList warnings={turn.warnings} />}

      {turn.status && (
        <div className="row small muted">
          <span className="spinner" aria-hidden="true" />
          <span role="status">{turn.status}…</span>
        </div>
      )}

      {turn.answer && (
        <div className="measure" style={{ whiteSpace: 'pre-wrap' }}>{turn.answer}</div>
      )}

      {/* Trích dẫn LUÔN đến sau mẩu chữ cuối — danh sách nguồn chỉ chốt được khi đã xong. */}
      {turn.citations.length > 0 && (
        <CitationList citations={turn.citations} onOpen={onOpenCitation} />
      )}

      {turn.refusal && (
        <div className="notice notice-warning measure" role="status"
             style={{ whiteSpace: 'pre-wrap', display: 'block' }}>
          <strong className="notice-label">Chưa trả lời được</strong>
          <div style={{ marginTop: 'var(--s2)' }}>{turn.refusal.message}</div>
        </div>
      )}

      {turn.error && (
        <div className="notice notice-error measure" role="alert">
          <span className="notice-label">Lỗi</span>
          <span>{turn.error.message}</span>
        </div>
      )}

      {turn.totalMs != null && !turn.refusal && !turn.error && (
        <p className="xsmall muted" style={{ margin: 0 }}>
          Chữ đầu sau {formatMs(turn.firstTokenMs)} · xong sau {formatMs(turn.totalMs)}
        </p>
      )}
    </div>
  )
}

function formatMs(ms?: number): string {
  if (ms == null || ms < 0) return '—'
  return ms < 1000 ? `${ms} mili giây` : `${(ms / 1000).toFixed(1)} giây`
}
