import { useCallback, useEffect, useRef, useState } from 'react'
import { api, ApiError, type DocumentRow, type Page } from '../lib/api'
import { MetadataReview } from '../components/MetadataReview'
import { BulkGrantDialog, DocumentPermissionsDialog, ExplainAccessDialog }
  from '../components/PermissionPanel'
import { UploadDialog } from '../components/UploadDialog'

interface BatchProgress {
  trackingId: string
  status: string
  total: number
  done: number
  failed: number
  files: {
    itemId: number; filename: string; documentId: number | null
    status: string; stage: string | null
    errorCode: string | null; errorReason: string | null
  }[]
}

/**
 * Màn nạp và xem xét tài liệu.
 *
 * Ba nhóm ngày tháng được tách riêng chứ không gộp thành một bộ lọc "có vấn đề": *đã hết hiệu
 * lực* cần thay văn bản, *sắp hết hiệu lực* cần chuẩn bị trước, còn *chưa khai thời hạn* chỉ là
 * thiếu thông tin cần bổ sung. Gộp ba thứ ấy lại thì việc gấp lẫn với việc không gấp, và danh
 * sách mất tác dụng.
 */
export function AdminDocumentsPage() {
  const [rows, setRows] = useState<DocumentRow[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [expiring, setExpiring] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [batch, setBatch] = useState<BatchProgress | null>(null)
  const [reviewing, setReviewing] = useState<DocumentRow | null>(null)
  const [quyenCua, setQuyenCua] = useState<DocumentRow | null>(null)
  const [aiDocDuoc, setAiDocDuoc] = useState<DocumentRow | null>(null)
  const [daChon, setDaChon] = useState<Set<number>>(new Set())
  const [vuaCap, setVuaCap] = useState<string | null>(null)
  const [dangCapHangLoat, setDangCapHangLoat] = useState(false)
  const [tepDangChon, setTepDangChon] = useState<File[] | null>(null)
  // Lựa chọn quyền của đợt nạp, giữ tới khi đợt chạy xong mới áp — lúc bấm nạp thì tài liệu
  // chưa có mã, nên chưa cấp quyền được.
  const [quyenChoLoNap, setQuyenChoLoNap] =
    useState<{ departmentIds: number[]; sensitivity: number | null } | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)
  const pollRef = useRef<number | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = new URLSearchParams({ page: String(page), size: '20' })
      if (q.trim()) params.set('q', q.trim())
      if (status) params.set('status', status)
      if (expiring) params.set('expiringInDays', expiring)
      const data = await api.get<Page<DocumentRow>>(`/api/admin/documents?${params}`)
      setRows(data.content)
      setTotal(data.totalElements)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được danh sách tài liệu.')
    } finally {
      setLoading(false)
    }
  }, [page, q, status, expiring])

  useEffect(() => { void load() }, [load])

  // Xoá lựa chọn khi danh sách đổi. Giữ lại id của trang trước là giữ một lựa chọn mà người
  // dùng không còn nhìn thấy — rồi một cú bấm "cấp quyền" đụng vào tài liệu họ không định chọn.
  useEffect(() => { setDaChon(new Set()) }, [page, q, status, expiring])

  function doiChon(id: number) {
    setDaChon(truoc => {
      const sau = new Set(truoc)
      if (sau.has(id)) sau.delete(id); else sau.add(id)
      return sau
    })
  }

  const chonHet = rows.length > 0 && rows.every(d => daChon.has(d.id))

  /**
   * Áp quyền và nhãn mật đã chọn lúc nạp, SAU KHI đợt nạp xong.
   *
   * Phải chờ vì lúc bấm nạp tài liệu chưa có mã. Chỉ áp cho tệp nạp thành công: cấp quyền trên
   * một tài liệu nạp lỗi là để lại một mục quyền trỏ vào chỗ trống.
   */
  const apQuyenSauNap = useCallback(async (xong: BatchProgress) => {
    const chon = quyenChoLoNap
    if (!chon) return
    setQuyenChoLoNap(null)
    const ids = xong.files.filter(f => f.status === 'XONG' && f.documentId != null)
                          .map(f => f.documentId as number)
    if (ids.length === 0) return

    try {
      if (chon.sensitivity != null) {
        for (const id of ids) {
          await api.put(`/api/admin/documents/${id}/sensitivity`, { sensitivity: chon.sensitivity })
        }
      }
      let tong = 0
      for (const phongId of chon.departmentIds) {
        const kq = await api.post<{ applied: number }>('/api/admin/permissions/bulk-grant',
          { documentIds: ids, subjectType: 'PHONG_BAN', subjectId: phongId, effect: 'CHO_PHEP' })
        tong += kq.applied
      }
      if (tong > 0 || chon.sensitivity != null) {
        setVuaCap(`Đã áp cho ${ids.length} tài liệu vừa nạp`
          + (tong > 0 ? `: ${tong} mục quyền` : '')
          + (chon.sensitivity != null ? ', và nhãn mật đã chọn.' : '.'))
        window.setTimeout(() => setVuaCap(null), 10000)
      }
      void load()
    } catch (err) {
      setError(err instanceof ApiError
        ? `Nạp xong nhưng chưa cấp được quyền: ${err.message}`
        : 'Nạp xong nhưng chưa cấp được quyền. Cấp lại ở màn Tài liệu.')
    }
  }, [quyenChoLoNap, load])

  // Theo dõi tiến độ đợt nạp. Tiến độ nằm ở máy chủ chứ không nằm trong bộ nhớ trang, nên
  // đóng tab rồi mở lại vẫn thấy — việc nạp không phụ thuộc người dùng còn mở màn hình.
  useEffect(() => {
    if (!batch || batch.status !== 'DANG_CHAY') return
    pollRef.current = window.setInterval(async () => {
      try {
        const next = await api.get<BatchProgress>(`/api/admin/documents/batches/${batch.trackingId}`)
        setBatch(next)
        if (next.status !== 'DANG_CHAY') {
          void load()
          void apQuyenSauNap(next)
        }
      } catch { /* mất mạng tạm: vòng sau thử lại */ }
    }, 2000)
    return () => { if (pollRef.current) window.clearInterval(pollRef.current) }
  }, [batch, load, apQuyenSauNap])

  async function upload(files: File[]) {
    if (files.length === 0) return
    try {
      const accepted = await api.upload<{ trackingId: string; fileCount: number }>(
        '/api/admin/documents/upload', files)
      setBatch({ trackingId: accepted.trackingId, status: 'DANG_CHAY',
                 total: accepted.fileCount, done: 0, failed: 0, files: [] })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải tệp lên được.')
    } finally {
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  return (
    <div className="stack">
      <div className="row-between wrap">
        <h1>Tài liệu</h1>
        <div className="row">
          <input ref={fileInput} type="file" multiple hidden
                 accept=".docx,.doc,.pdf,.xlsx,.txt,.md"
                 onChange={e => {
                   const fs = e.target.files ? Array.from(e.target.files) : []
                   if (fs.length > 0) setTepDangChon(fs)
                 }} />
          <button className="btn btn-primary" onClick={() => fileInput.current?.click()}>
            Nạp tài liệu
          </button>
        </div>
      </div>

      {batch && <BatchPanel batch={batch} onDismiss={() => setBatch(null)} />}

      {vuaCap && (
        <div className="notice notice-success" role="status">
          <span className="notice-label">Đã cấp quyền</span><span>{vuaCap}</span>
        </div>
      )}

      {daChon.size > 0 && (
        <div className="card row-between wrap" style={{ gap: 'var(--s3)' }}>
          <span><strong>{daChon.size}</strong> tài liệu đã chọn</span>
          <div className="row">
            <button className="btn btn-ghost btn-sm" onClick={() => setDaChon(new Set())}>
              Bỏ chọn
            </button>
            <button className="btn btn-primary btn-sm"
                    onClick={() => setDangCapHangLoat(true)}>
              Cấp quyền cho {daChon.size} tài liệu
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="notice notice-error" role="alert">
          <span className="notice-label">Lỗi</span><span>{error}</span>
        </div>
      )}

      <div className="card">
        <div className="row wrap" style={{ gap: 'var(--s3)' }}>
          <div className="field grow" style={{ marginBottom: 0, minWidth: 220 }}>
            <label htmlFor="q">Tìm theo tên</label>
            <input id="q" type="text" value={q}
                   onChange={e => { setQ(e.target.value); setPage(0) }} />
          </div>
          <div className="field" style={{ marginBottom: 0, minWidth: 180 }}>
            <label htmlFor="st">Trạng thái</label>
            <select id="st" value={status} onChange={e => { setStatus(e.target.value); setPage(0) }}>
              <option value="">Tất cả</option>
              <option value="DA_LAP_CHI_MUC">Đã lập chỉ mục</option>
              <option value="CHO_XAC_NHAN">Chờ xác nhận</option>
              <option value="DANG_XU_LY">Đang xử lý</option>
              <option value="LOI">Nạp lỗi</option>
              <option value="DA_GO">Đã gỡ</option>
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, minWidth: 200 }}>
            <label htmlFor="ex">Hiệu lực</label>
            <select id="ex" value={expiring} onChange={e => { setExpiring(e.target.value); setPage(0) }}>
              <option value="">Tất cả</option>
              <option value="0">Đã hết hiệu lực</option>
              <option value="30">Hết hạn trong 30 ngày</option>
              <option value="90">Hết hạn trong 90 ngày</option>
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="state"><span className="spinner" /><p>Đang tải danh sách…</p></div>
      ) : rows.length === 0 ? (
        <div className="state">
          <h3>Chưa có tài liệu nào khớp bộ lọc</h3>
          <p>Bỏ bớt điều kiện lọc, hoặc nạp tài liệu mới bằng nút ở góc trên.</p>
        </div>
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ width: 36 }}>
                    <input type="checkbox" aria-label="Chọn tất cả tài liệu trên trang"
                           checked={chonHet}
                           onChange={() => setDaChon(chonHet ? new Set()
                             : new Set(rows.map(d => d.id)))} />
                  </th>
                  <th>Tên tài liệu</th>
                  <th>Danh mục</th>
                  <th>Nhãn mật</th>
                  <th>Hiệu lực</th>
                  <th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(d => (
                  <DocumentRowView key={d.id} doc={d} onChanged={load}
                                   onReview={() => setReviewing(d)}
                                   daChon={daChon.has(d.id)} onDoiChon={() => doiChon(d.id)}
                                   onQuyen={() => setQuyenCua(d)} />
                ))}
              </tbody>
            </table>
          </div>

          <div className="row-between small muted">
            <span>{total} tài liệu</span>
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

      {reviewing && (
        <MetadataReview doc={reviewing} onClose={() => setReviewing(null)}
                        onConfirmed={load} />
      )}

      {quyenCua && (
        <DocumentPermissionsDialog documentId={quyenCua.id} documentTitle={quyenCua.title}
                                   onClose={() => setQuyenCua(null)}
                                   onExplain={() => { setAiDocDuoc(quyenCua); setQuyenCua(null) }} />
      )}

      {aiDocDuoc && (
        <ExplainAccessDialog documentId={aiDocDuoc.id} documentTitle={aiDocDuoc.title}
                             onClose={() => setAiDocDuoc(null)} />
      )}

      {tepDangChon && (
        <UploadDialog files={tepDangChon}
                      onClose={() => {
                        setTepDangChon(null)
                        if (fileInput.current) fileInput.current.value = ''
                      }}
                      onConfirm={chon => {
                        setQuyenChoLoNap(chon)
                        const fs = tepDangChon
                        setTepDangChon(null)
                        void upload(fs)
                      }} />
      )}

      {dangCapHangLoat && (
        <BulkGrantDialog documentIds={[...daChon]}
                         onClose={() => setDangCapHangLoat(false)}
                         onDone={(applied, selected) => {
                           setDangCapHangLoat(false)
                           setDaChon(new Set())
                           setVuaCap(applied === selected
                             ? `Đã ghi ${applied} mục quyền.`
                             : `Đã ghi ${applied}/${selected} mục; ${selected - applied} mục đã có sẵn nên bỏ qua.`)
                           window.setTimeout(() => setVuaCap(null), 8000)
                         }} />
      )}
    </div>
  )
}

function DocumentRowView({ doc, onChanged, onReview, daChon, onDoiChon, onQuyen }: {
  doc: DocumentRow
  onChanged: () => void
  onReview: () => void
  daChon: boolean
  onDoiChon: () => void
  onQuyen: () => void
}) {
  const [busy, setBusy] = useState(false)

  async function withdraw() {
    setBusy(true)
    try {
      await api.post(`/api/admin/documents/${doc.id}/withdraw`)
      onChanged()
    } finally { setBusy(false) }
  }

  return (
    <tr>
      <td>
        <input type="checkbox" checked={daChon} onChange={onDoiChon}
               aria-label={`Chọn ${doc.title}`} />
      </td>
      <td>
        <div style={{ fontWeight: 500 }}>{doc.title}</div>
        <div className="xsmall muted">{doc.originalFilename}</div>
        {doc.errorReason && (
          <div className="xsmall" style={{ color: 'var(--error)', marginTop: 'var(--s1)' }}>
            {doc.errorReason}
          </div>
        )}
      </td>
      <td>{doc.category ?? <span className="muted">—</span>}</td>
      <td><span className="tag">{doc.sensitivityLabel}</span></td>
      <td className="small">
        {/* Ba trạng thái ngày tháng hiện KHÁC NHAU, vì chúng đòi ba hành động khác nhau. */}
        {doc.hasNoExpiry ? <span className="muted">Không có thời hạn</span>
          : doc.expired ? <span className="tag tag-warning">Đã hết hiệu lực</span>
          : doc.expiringSoon ? <span className="tag tag-warning">Sắp hết: {doc.effectiveTo}</span>
          : doc.unknownExpiry ? <span className="muted">Chưa khai thời hạn</span>
          : <span>Đến {doc.effectiveTo}</span>}
      </td>
      <td>
        <div className="row" style={{ gap: 'var(--s2)' }}>
          <StatusTag status={doc.status} />
          {/*
            Nút xem xét metadata hiện ở MỌI tài liệu đã nạp, không chỉ tài liệu chờ xác nhận:
            quản trị cần đọc lại căn cứ cả sau khi đã xác nhận, ví dụ khi một cảnh báo hết hiệu
            lực trông sai và họ muốn biết ngày ấy suy ra từ câu nào.
          */}
          {(doc.status === 'DA_LAP_CHI_MUC' || doc.status === 'CHO_XAC_NHAN') && (
            <>
              <button className="btn btn-sm btn-ghost" onClick={onReview}>
                {doc.metadataConfirmed ? 'Xem căn cứ' : 'Xem xét'}
              </button>
              <button className="btn btn-sm btn-ghost" disabled={busy} onClick={withdraw}>Gỡ</button>
            </>
          )}
          {/*
            Nút quyền hiện ở MỌI tài liệu, kể cả tài liệu đang xử lý hay nạp lỗi: quản trị
            thường muốn chuẩn bị sẵn quyền trước khi việc nạp xong, và một tài liệu nạp lỗi vẫn
            cần xem lại xem đã lỡ cấp quyền cho ai chưa.
          */}
          <button className="btn btn-sm btn-ghost" onClick={onQuyen}>Quyền</button>
        </div>
      </td>
    </tr>
  )
}

function StatusTag({ status }: { status: string }) {
  const map: Record<string, { label: string; cls: string }> = {
    DA_LAP_CHI_MUC: { label: 'Đã lập chỉ mục', cls: 'tag-success' },
    CHO_XAC_NHAN:   { label: 'Chờ xác nhận',   cls: 'tag-warning' },
    DANG_XU_LY:     { label: 'Đang xử lý',     cls: '' },
    LOI:            { label: 'Nạp lỗi',        cls: 'tag-error' },
    DA_GO:          { label: 'Đã gỡ',          cls: '' },
  }
  const entry = map[status] ?? { label: status, cls: '' }
  return <span className={`tag ${entry.cls}`}>{entry.label}</span>
}

function BatchPanel({ batch, onDismiss }: { batch: BatchProgress; onDismiss: () => void }) {
  const running = batch.status === 'DANG_CHAY'
  return (
    <div className="card">
      <div className="row-between">
        <h3>
          {running ? 'Đang nạp tài liệu' : 'Đợt nạp đã xong'}
          {' '}
          <span className="small muted">
            {batch.done}/{batch.total} xong{batch.failed > 0 && `, ${batch.failed} lỗi`}
          </span>
        </h3>
        {!running && <button className="btn btn-sm btn-ghost" onClick={onDismiss}>Ẩn</button>}
      </div>

      <p className="xsmall muted">
        Việc nạp chạy ở máy chủ. Bạn đóng tab hay chuyển màn thì nó vẫn chạy tiếp.
      </p>

      {batch.files.length > 0 && (
        <div className="stack small" style={{ gap: 'var(--s2)', marginTop: 'var(--s3)' }}>
          {batch.files.map(f => (
            <div key={f.itemId} className="row-between">
              <span className="grow" style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {f.filename}
              </span>
              {f.status === 'LOI'
                ? <span className="tag tag-error">{f.errorReason ?? 'Lỗi'}</span>
                : f.status === 'XONG'
                ? <span className="tag tag-success">{f.stage ?? 'Xong'}</span>
                : <span className="muted xsmall">{f.stage ?? 'Đang chờ'}…</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
