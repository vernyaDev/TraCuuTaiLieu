package vn.utc.tracuu.access;

/**
 * Hiệu lực của một mục kiểm soát truy cập.
 *
 * <p>Cho phép và từ chối nằm trong CÙNG một bảng, vì luật "từ chối thắng mọi cho phép"
 * chỉ được viết ở đúng một chỗ. Tách hai bảng thì luật ấy phải viết hai lần, và hai lần
 * sẽ lệch nhau.
 */
public enum AclEffect {
    CHO_PHEP,
    TU_CHOI
}
