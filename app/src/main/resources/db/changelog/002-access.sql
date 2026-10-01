--liquibase formatted sql

--changeset tracuu:002-departments
--comment Danh sách PHẲNG, cố ý không có cột cha (US-004): kế thừa quyền theo cây là nguồn lỗi phân quyền phổ biến nhất.
CREATE TABLE departments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE departments;

--changeset tracuu:002-users
--comment clearance_level là SỐ THỨ TỰ chứ không phải chuỗi liệt kê — thêm mức mật thứ tư về sau là thay đổi rẻ (US-007).
CREATE TABLE users (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username              VARCHAR(100) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(200) NOT NULL,
    email                 VARCHAR(200),
    clearance_level       INTEGER      NOT NULL DEFAULT 1,
    role                  VARCHAR(30)  NOT NULL DEFAULT 'NHAN_VIEN',
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_count    INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_role_valid      CHECK (role IN ('NHAN_VIEN','QUAN_TRI')),
    CONSTRAINT users_clearance_valid CHECK (clearance_level >= 1)
);
CREATE INDEX idx_users_active ON users (is_active);
--rollback DROP TABLE users;

--changeset tracuu:002-user-departments
--comment Một người thuộc nhiều phòng ban là hợp lệ.
CREATE TABLE user_departments (
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, department_id)
);
CREATE INDEX idx_user_departments_dept ON user_departments (department_id);
--rollback DROP TABLE user_departments;

--changeset tracuu:002-permission-version
--comment Số hiệu phiên bản quyền (mô hình dữ liệu Mục 8). Tăng ở MỌI đường ghi quyền; quên một đường là tạo lỗ rò rỉ chỉ hiện sau khi cấu hình đổi.
CREATE TABLE permission_version (
    id      SMALLINT PRIMARY KEY DEFAULT 1,
    version BIGINT   NOT NULL DEFAULT 1,
    CONSTRAINT permission_version_single_row CHECK (id = 1)
);
INSERT INTO permission_version (id, version) VALUES (1, 1);
--rollback DROP TABLE permission_version;

--changeset tracuu:002-permission-audit
--comment NFR-system-029 đòi giữ >= 12 tháng. Đây là thứ trả lời được "ai đã mở quyền tài liệu này".
CREATE TABLE permission_audit (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(60)  NOT NULL,
    target_type VARCHAR(40)  NOT NULL,
    target_id   BIGINT,
    before_json JSONB,
    after_json  JSONB,
    at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_permission_audit_at     ON permission_audit (at DESC);
CREATE INDEX idx_permission_audit_target ON permission_audit (target_type, target_id);
--rollback DROP TABLE permission_audit;
