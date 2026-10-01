import { useState } from 'react'
import { Modal, type DepartmentLite } from './PermissionPanel'
import { useClearanceLevels, useDepartments } from './UserForms'

/**
 * Hỏi "ai được đọc lô này" NGAY LÚC nạp, trước khi tệp lên máy chủ.
 *
 * Quyền là từ chối mặc định, nên một đợt nạp không kèm cấp quyền tạo ra một kho mà
 * <strong>không ai đọc được</strong> — kể cả người vừa nạp. Trước đây việc cấp quyền là một
 * thao tác riêng, làm sau, và dễ quên; hậu quả không hiện ra ở màn quản trị mà hiện ra ở màn
 * hỏi đáp của nhân viên, dưới dạng "tôi không tìm thấy đủ căn cứ" — một triệu chứng trông
 * giống hệt lỗi chất lượng.
 *
 * Hỏi ở đây biến việc hay quên thành một bước của chính việc nạp.
 */
export function UploadDialog({ files, onClose, onConfirm }: {
  files: File[]
  onClose: () => void
  onConfirm: (chon: { departmentIds: number[]; sensitivity: number | null }) => void
}) {
  const phongBan: DepartmentLite[] = useDepartments()
  const levels = useClearanceLevels()
  const [chonPhong, setChonPhong] = useState<number[]>([])
  const [nhanMat, setNhanMat] = useState<string>('')

  function doiPhong(id: number) {
    setChonPhong(t => t.includes(id) ? t.filter(x => x !== id) : [...t, id])
  }

  const tongDungLuong = files.reduce((s, f) => s + f.size, 0)

  return (
    <Modal onClose={onClose} rong={560}>
      <h3>Nạp {files.length} tệp</h3>

      <div className="stack small" style={{ gap: 'var(--s1)', maxHeight: 140, overflowY: 'auto' }}>
        {files.slice(0, 12).map(f => (
          <div key={f.name} className="row-between">
            <span className="grow" style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {f.name}
            </span>
            <span className="muted xsmall">{Math.max(1, Math.round(f.size / 1024))} KB</span>
          </div>
        ))}
        {files.length > 12 && (
          <span className="muted xsmall">… và {files.length - 12} tệp nữa</span>
        )}
      </div>
      <p className="xsmall muted">Tổng {Math.max(1, Math.round(tongDungLuong / 1024))} KB</p>

      <div className="field" style={{ marginTop: 'var(--s4)' }}>
        <label>Phòng ban nào được đọc lô này?</label>
        {phongBan.length === 0 ? (
          <div className="notice notice-warning">
            <span className="notice-label">Chưa có phòng ban</span>
            <span>
              Tạo phòng ban ở màn <strong>Tài khoản → Phòng ban</strong> rồi nạp lại, hoặc nạp
              trước và cấp quyền sau ở màn Tài liệu.
            </span>
          </div>
        ) : (
          <div className="stack" style={{ gap: 'var(--s2)' }}>
            {phongBan.map(d => (
              <label key={d.id} className="row" style={{ gap: 'var(--s2)', cursor: 'pointer' }}>
                <input type="checkbox" checked={chonPhong.includes(d.id)}
                       onChange={() => doiPhong(d.id)} />
                <span>{d.name} <span className="muted xsmall">({d.memberCount} người)</span></span>
              </label>
            ))}
          </div>
        )}
      </div>

      <div className="field">
        <label htmlFor="up-mat">Nhãn mật cho cả lô (không bắt buộc)</label>
        <select id="up-mat" value={nhanMat} onChange={e => setNhanMat(e.target.value)}>
          <option value="">— giữ mặc định —</option>
          {levels.map(l => <option key={l.level} value={l.level}>{l.level} — {l.label}</option>)}
        </select>
        <p className="xsmall muted">
          Người có mức mật thấp hơn nhãn này sẽ không đọc được, <strong>kể cả khi đã được cấp
          quyền</strong>.
        </p>
      </div>

      {chonPhong.length === 0 && (
        <div className="notice notice-warning">
          <span className="notice-label">Chưa chọn ai</span>
          <span>
            Nạp mà không cấp quyền thì <strong>không ai tra cứu được</strong> những tài liệu này
            cho tới khi có người cấp quyền ở màn Tài liệu. Vẫn nạp được — chỉ cần biết trước.
          </span>
        </div>
      )}

      <div className="row" style={{ justifyContent: 'flex-end', marginTop: 'var(--s4)' }}>
        <button className="btn" onClick={onClose}>Huỷ</button>
        <button className="btn btn-primary"
                onClick={() => onConfirm({
                  departmentIds: chonPhong,
                  sensitivity: nhanMat ? Number(nhanMat) : null,
                })}>
          Nạp {files.length} tệp
        </button>
      </div>
    </Modal>
  )
}
