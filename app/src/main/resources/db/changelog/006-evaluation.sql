--liquibase formatted sql

--changeset tracuu:006-ground-truth
--comment Tach lam HAI bang: mot cau hoi co NHIEU vi tri dap an deu chap nhan duoc (AC-004 cua US-029). Vi tri khai bang document_id cong khoang ky tu, KHONG khai bang ma doan - chot 2026-09-09. Do la thu lam AC-010 chay duoc: cung mot bo cau anh xa sang tung tap doan, khong phai soan lai dap an.
CREATE TABLE ground_truth_questions (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_set_version VARCHAR(40)  NOT NULL,
    ordinal              INTEGER      NOT NULL,
    text                 TEXT         NOT NULL,
    complexity           VARCHAR(20)  NOT NULL DEFAULT 'DON_GIAN',
    expected_answer      TEXT,
    note                 VARCHAR(1000),
    CONSTRAINT gt_unique_ordinal UNIQUE (question_set_version, ordinal),
    CONSTRAINT gt_complexity     CHECK (complexity IN ('DON_GIAN','PHUC_TAP'))
);
CREATE TABLE ground_truth_positions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id BIGINT  NOT NULL REFERENCES ground_truth_questions(id) ON DELETE CASCADE,
    document_id BIGINT  NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    char_start  INTEGER NOT NULL,
    char_end    INTEGER NOT NULL,
    CONSTRAINT gt_position_range CHECK (char_end > char_start)
);
CREATE INDEX idx_gt_positions_question ON ground_truth_positions (question_id);
--rollback DROP TABLE ground_truth_positions;

--changeset tracuu:006-eval-runs
--comment Hien thuc truc tiep cua NFR-system-046: ket qua tu mang theo dieu kien sinh ra no. Cot config giu NGUYEN VEN tep cau hinh luc chay.
CREATE TABLE eval_runs (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label                VARCHAR(200),
    config               JSONB        NOT NULL,
    chunk_set_id         BIGINT       REFERENCES chunk_sets(id) ON DELETE SET NULL,
    question_set_version VARCHAR(40)  NOT NULL,
    retrieval_mode       VARCHAR(20)  NOT NULL,
    model_name           VARCHAR(120),
    temperature          REAL,
    seed                 BIGINT,
    top_k                INTEGER,
    context_max_tokens   INTEGER,
    status               VARCHAR(20)  NOT NULL DEFAULT 'DANG_CHAY',
    started_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at          TIMESTAMPTZ,
    CONSTRAINT eval_run_status CHECK (status IN ('DANG_CHAY','XONG','LOI','DA_DUNG'))
);
--rollback DROP TABLE eval_runs;

--changeset tracuu:006-eval-results
--comment Mot dong cho moi cau hoi trong moi luot chay, nen AC-009 cua US-030 (chay tiep phan do dang) chi la truy van tim cau chua co ket qua.
CREATE TABLE eval_results (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id           BIGINT  NOT NULL REFERENCES eval_runs(id) ON DELETE CASCADE,
    question_id      BIGINT  NOT NULL REFERENCES ground_truth_questions(id) ON DELETE CASCADE,
    answer           TEXT,
    refused          BOOLEAN NOT NULL DEFAULT FALSE,
    refusal_reason   VARCHAR(40),
    retrieved_ids    BIGINT[],
    hit_ranks        INTEGER[],
    precision_at_k   REAL,
    recall_at_k      REAL,
    reciprocal_rank  REAL,
    auto_score       REAL,
    latency_ms       INTEGER,
    error_reason     VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT eval_result_unique UNIQUE (run_id, question_id)
);
CREATE INDEX idx_eval_results_run ON eval_results (run_id, question_id);
--rollback DROP TABLE eval_results;

--changeset tracuu:006-eval-metrics
CREATE TABLE eval_metrics (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id      BIGINT      NOT NULL REFERENCES eval_runs(id) ON DELETE CASCADE,
    metric_name VARCHAR(60) NOT NULL,
    k           INTEGER,
    value       REAL        NOT NULL,
    CONSTRAINT eval_metric_unique UNIQUE (run_id, metric_name, k)
);
--rollback DROP TABLE eval_metrics;

--changeset tracuu:006-human-grades
--comment Tach KHOI eval_results chu khong ghi de len no. AC-008 cua US-032: giu ca hai ket qua, vi SO SANH giua may cham va nguoi cham moi la thu co gia tri, khong phai ket qua cuoi.
CREATE TABLE human_grades (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    result_id BIGINT       NOT NULL REFERENCES eval_results(id) ON DELETE CASCADE,
    grader    VARCHAR(120) NOT NULL,
    score     REAL         NOT NULL,
    note      VARCHAR(1000),
    graded_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT human_grade_unique UNIQUE (result_id, grader)
);
--rollback DROP TABLE human_grades;
