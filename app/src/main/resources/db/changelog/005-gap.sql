--liquibase formatted sql

--changeset tracuu:005-refusal-log
--comment Chup lai phong ban TAI THOI DIEM HOI: nguoi dung co the doi phong ban sau do, va bao cao US-028 can biet phong ban LUC HOI de tra loi dung cau "nen cap quyen cho phong nao".
CREATE TABLE refusal_log (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turn_id              BIGINT      REFERENCES turns(id) ON DELETE SET NULL,
    user_id              BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    question             TEXT        NOT NULL,
    rewritten            TEXT,
    reason               VARCHAR(40) NOT NULL,
    department_snapshot  VARCHAR(600),
    clearance_snapshot   INTEGER,
    embedding            VECTOR(768),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT refusal_reason_valid CHECK (reason IN ('THIEU_TAI_LIEU','THIEU_QUYEN','CAN_CU_MONG','NGOAI_PHAM_VI'))
);
CREATE INDEX idx_refusal_created ON refusal_log (created_at DESC);
CREATE INDEX idx_refusal_reason  ON refusal_log (reason, created_at DESC);
--rollback DROP TABLE refusal_log;

--changeset tracuu:005-gap-clusters
--comment Nhom khoang trong. AC-005 cua US-028 doi trang thai da xu ly de khong hien lai dung nhom ay.
CREATE TABLE gap_clusters (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label        VARCHAR(500) NOT NULL,
    reason       VARCHAR(40)  NOT NULL,
    centroid     VECTOR(768),
    member_count INTEGER      NOT NULL DEFAULT 0,
    first_seen   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status       VARCHAR(20)  NOT NULL DEFAULT 'CHO',
    resolved_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    resolved_at  TIMESTAMPTZ,
    resolve_note VARCHAR(1000),
    CONSTRAINT cluster_status_valid CHECK (status IN ('CHO','DA_XU_LY','BO_QUA'))
);
CREATE INDEX idx_gap_clusters_status ON gap_clusters (status, last_seen DESC);
--rollback DROP TABLE gap_clusters;

--changeset tracuu:005-gap-members
CREATE TABLE gap_members (
    cluster_id BIGINT NOT NULL REFERENCES gap_clusters(id) ON DELETE CASCADE,
    refusal_id BIGINT NOT NULL REFERENCES refusal_log(id) ON DELETE CASCADE,
    PRIMARY KEY (cluster_id, refusal_id)
);
CREATE INDEX idx_gap_members_refusal ON gap_members (refusal_id);
--rollback DROP TABLE gap_members;
