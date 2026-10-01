import type { Citation, PlanItem, Warning } from './api'

/**
 * Nhận luồng sự kiện của một lượt hỏi.
 *
 * Dùng `fetch` kèm đọc dòng chứ không dùng `EventSource`, vì hai lý do: `EventSource` chỉ
 * gửi được yêu cầu dạng GET nên câu hỏi phải nhét vào địa chỉ (giới hạn độ dài, lại lọt vào
 * nhật ký máy chủ), và nó không gắn được thẻ chống giả mạo yêu cầu.
 *
 * `AbortController` là phần quan trọng không kém: người dùng đóng tab hoặc bấm dừng thì máy
 * chủ **ngừng sinh chữ** thay vì chạy tiếp rồi vứt kết quả. Với một mô hình dùng chung bộ
 * nhớ đồ hoạ, một lượt bị bỏ mà vẫn chạy là lấy mất lượt của người đang xếp hàng.
 */

export interface AskHandlers {
  onQueued?: (position: number, ahead: number) => void
  onStatus?: (step: string) => void
  onRewritten?: (question: string) => void
  onPlan?: (steps: PlanItem[]) => void
  onToken?: (text: string) => void
  onWarning?: (warning: Warning) => void
  onCitations?: (items: Citation[]) => void
  onRefusal?: (stage: string, message: string) => void
  onDone?: (turnId: number, firstTokenMs: number, totalMs: number) => void
  onError?: (code: string, message: string) => void
}

function csrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

export function ask(
  question: string,
  conversationId: number | null,
  handlers: AskHandlers,
): AbortController {
  const controller = new AbortController()
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = csrfToken()
  if (token) headers['X-XSRF-TOKEN'] = token

  ;(async () => {
    try {
      const res = await fetch('/api/qa/ask', {
        method: 'POST',
        headers,
        credentials: 'same-origin',
        body: JSON.stringify({ question, conversationId }),
        signal: controller.signal,
      })

      if (!res.ok || !res.body) {
        const text = await res.text().catch(() => '')
        let code = 'LOI_HE_THONG'
        let message = 'Hệ thống gặp sự cố khi xử lý câu hỏi. Thử lại sau ít phút.'
        try {
          const parsed = JSON.parse(text)
          code = parsed.code ?? code
          message = parsed.message ?? message
        } catch { /* phản hồi không phải JSON: giữ câu mặc định */ }
        handlers.onError?.(code, message)
        return
      }

      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // Mỗi sự kiện kết thúc bằng một dòng trống. Giữ phần dư lại cho vòng sau —
        // một mẩu chữ tiếng Việt có thể bị cắt giữa hai gói tin.
        let sep: number
        while ((sep = buffer.indexOf('\n\n')) >= 0) {
          const raw = buffer.slice(0, sep)
          buffer = buffer.slice(sep + 2)
          dispatch(raw, handlers)
        }
      }
    } catch (err) {
      if ((err as Error).name === 'AbortError') return   // người dùng chủ động dừng
      handlers.onError?.('MAT_KET_NOI',
        'Mất kết nối tới máy chủ giữa chừng. Kiểm tra mạng rồi gửi lại câu hỏi.')
    }
  })()

  return controller
}

function dispatch(raw: string, h: AskHandlers) {
  let event = 'message'
  const dataLines: string[] = []

  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
  }
  if (dataLines.length === 0) return

  let payload: any
  try {
    payload = JSON.parse(dataLines.join('\n'))
  } catch {
    return
  }

  switch (event) {
    case 'queued':    h.onQueued?.(payload.position, payload.ahead); break
    case 'status':    h.onStatus?.(payload.step); break
    case 'rewritten': h.onRewritten?.(payload.question); break
    case 'plan':      h.onPlan?.(payload.steps ?? []); break
    case 'token':     h.onToken?.(payload.text ?? ''); break
    case 'warning':   h.onWarning?.(payload); break
    case 'citations': h.onCitations?.(payload.items ?? []); break
    case 'refusal':   h.onRefusal?.(payload.stage, payload.message); break
    case 'done':      h.onDone?.(payload.turnId, payload.firstTokenMs, payload.totalMs); break
    case 'error':     h.onError?.(payload.code, payload.message); break
  }
}
