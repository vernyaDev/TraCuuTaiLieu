/**
 * Lớp gọi máy chủ.
 *
 * Hai điều lớp này chịu trách nhiệm, và cả hai đều dễ làm sai ở chỗ khác:
 *
 * 1. **Mọi lỗi ra ngoài dưới cùng MỘT hình dạng.** Máy chủ trả `{code, message, field}`;
 *    lớp này biến nó thành `ApiError` có đủ ba trường. Thành phần giao diện không bao giờ
 *    phải đoán xem lỗi là chuỗi hay đối tượng.
 * 2. **Thẻ chống giả mạo yêu cầu** đọc từ cookie và gắn vào mọi yêu cầu ghi. Quên một chỗ
 *    là một nút im lặng không hoạt động, và thông báo lỗi sẽ không nói gì về nguyên nhân.
 */

export class ApiError extends Error {
  code: string
  field?: string
  status: number
  fieldErrors?: { field: string; message: string }[]

  constructor(status: number, code: string, message: string,
              field?: string, fieldErrors?: { field: string; message: string }[]) {
    super(message)
    this.status = status
    this.code = code
    this.field = field
    this.fieldErrors = fieldErrors
  }

  /** Phiên hết hạn — giao diện cần đưa người dùng về màn đăng nhập, không hiện lỗi đỏ. */
  get isUnauthenticated() { return this.status === 401 }

  /** Còn mật khẩu tạm — phải đổi trước khi dùng chức năng khác. */
  get needsPasswordChange() { return this.code === 'PHAI_DOI_MAT_KHAU' }
}

function csrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const isWrite = method !== 'GET'

  if (body !== undefined && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  if (isWrite) {
    const token = csrfToken()
    if (token) headers['X-XSRF-TOKEN'] = token
  }

  const res = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined
        : body instanceof FormData ? body
        : JSON.stringify(body),
  })

  if (res.status === 204) return undefined as T

  const text = await res.text()
  const data = text ? safeParse(text) : null

  if (!res.ok) {
    const code = data?.code ?? 'LOI_HE_THONG'
    const message = data?.message
        ?? 'Hệ thống gặp sự cố khi xử lý yêu cầu. Thử lại sau ít phút.'
    throw new ApiError(res.status, code, message, data?.field, data?.errors)
  }
  return data as T
}

function safeParse(text: string): any {
  try { return JSON.parse(text) } catch { return null }
}

export const api = {
  get:  <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put:  <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  del:  <T>(path: string, body?: unknown) => request<T>('DELETE', path, body),

  /** Tải tệp lên. Không đặt Content-Type — trình duyệt tự đặt kèm ranh giới phần. */
  upload: <T>(path: string, files: File[], fieldName = 'files') => {
    const form = new FormData()
    files.forEach(f => form.append(fieldName, f))
    return request<T>('POST', path, form)
  },
}

// ── Kiểu dữ liệu dùng chung ────────────────────────────────────────────────────

export interface Me {
  id: number
  username: string
  fullName: string
  role: string
  clearanceLevel: number
  clearanceLabel: string
  departments: string[]
  mustChangePassword: boolean
  admin: boolean
}

export interface Citation {
  rank: number
  chunkId: number
  documentId: number
  documentTitle: string
  breadcrumb: string | null
  excerpt: string
  scoreBm25: number | null
  scoreVector: number | null
  scoreRrf: number | null
}

export interface PlanItem { ordinal: number; text: string; dependsOn: number | null }

export interface Warning {
  warningType: string
  documentId: number | null
  documentTitle: string
  detail: string
}

export interface DocumentRow {
  id: number
  title: string
  originalFilename: string
  status: string
  category: string | null
  sensitivity: number
  sensitivityLabel: string
  effectiveFrom: string | null
  effectiveTo: string | null
  hasNoExpiry: boolean
  expired: boolean
  expiringSoon: boolean
  unknownExpiry: boolean
  supersedesDocumentId: number | null
  metadataConfirmed: boolean
  errorReason: string | null
  uploadedAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
