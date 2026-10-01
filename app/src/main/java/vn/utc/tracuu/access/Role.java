package vn.utc.tracuu.access;

/** Vai trò chức năng. Quyền trên CHỨC NĂNG, khác hẳn quyền trên từng tài liệu. */
public enum Role {
    NHAN_VIEN,
    QUAN_TRI;

    public boolean isAdmin() {
        return this == QUAN_TRI;
    }
}
