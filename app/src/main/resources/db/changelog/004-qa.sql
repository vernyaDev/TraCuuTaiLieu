--liquibase formatted sql

--changeset tracuu:004-conversations
--comment Hoi thoai theo PHIEN, khong giu qua phien (ranh gioi pham vi ADR-0002). Cot summary phuc vu US-021.
CREATE TABLE conversations (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    summary          TEXT,
    turn_count       INTEGER     NOT NULL DEFAULT 0
);
CREATE INDEX idx_conversations_user ON conversations (user_id, last_activity_at DESC);
--rollback DROP TABLE conversations;

--changeset tracuu:004-turns
--comment raw_question VA rewritten_question luu RIENG. AC-002 cua US-020 doi nguoi dung xem duoc cau da viet lai; va khi mot cau bi tu choi, AC-004 cua US-027 doi biet viec viet lai co phai nguyen nhan that khong.
CREATE TABLE turns (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id        BIGINT      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    ordinal                INTEGER     NOT NULL,
    raw_question           TEXT        NOT NULL,
    rewritten_question     TEXT,
    answer                 TEXT,
    complexity             VARCHAR(20),
    status                 VARCHAR(20) NOT NULL DEFAULT 'DANG_CHAY',
    refusal_reason         VARCHAR(40),
    refusal_stage          VARCHAR(20),
    chunk_set_id           BIGINT      REFERENCES chunk_sets(id) ON DELETE SET NULL,
    latency_first_token_ms INTEGER,
    latency_total_ms       INTEGER,
    latency_retrieval_ms   INTEGER,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT turns_unique_ordinal UNIQUE (conversation_id, ordinal),
    CONSTRAINT turns_status_valid   CHECK (status IN ('DANG_CHAY','XONG','TU_CHOI','LOI','DA_DUNG')),
    CONSTRAINT turns_complexity     CHECK (complexity IS NULL OR complexity IN ('DON_GIAN','PHUC_TAP')),
    CONSTRAINT turns_refusal_stage  CHECK (refusal_stage IS NULL OR refusal_stage IN ('TANG_MOT','TANG_HAI'))
);
CREATE INDEX idx_turns_conversation ON turns (conversation_id, ordinal);
CREATE INDEX idx_turns_created      ON turns (created_at DESC);
--rollback DROP TABLE turns;

--changeset tracuu:004-turn-subqueries
--comment Chuoi phu thuoc cua US-022, va la nguon cho khoi "cac buoc da thuc hien".
CREATE TABLE turn_subqueries (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turn_id            BIGINT  NOT NULL REFERENCES turns(id) ON DELETE CASCADE,
    ordinal            INTEGER NOT NULL,
    depends_on_ordinal INTEGER,
    text               TEXT    NOT NULL,
    hit_count          INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT subquery_unique_ordinal UNIQUE (turn_id, ordinal),
    CONSTRAINT subquery_no_self_dep    CHECK (depends_on_ordinal IS NULL OR depends_on_ordinal < ordinal)
);
--rollback DROP TABLE turn_subqueries;

--changeset tracuu:004-turn-citations
--comment BA diem luu RIENG, khong luu moi diem cuoi. Khong tach ba thi khong phan tich duoc vi sao nhanh lai thang hay thua.
CREATE TABLE turn_citations (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turn_id      BIGINT  NOT NULL REFERENCES turns(id) ON DELETE CASCADE,
    chunk_id     BIGINT  NOT NULL REFERENCES chunks(id) ON DELETE CASCADE,
    rank         INTEGER NOT NULL,
    score_bm25   REAL,
    score_vector REAL,
    score_rrf    REAL,
    used_in_answer BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT citation_unique_rank UNIQUE (turn_id, rank)
);
CREATE INDEX idx_citations_turn ON turn_citations (turn_id);
--rollback DROP TABLE turn_citations;

--changeset tracuu:004-turn-warnings
--comment Canh bao het hieu luc hoac chong lan cua US-025.
CREATE TABLE turn_warnings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turn_id     BIGINT      NOT NULL REFERENCES turns(id) ON DELETE CASCADE,
    type        VARCHAR(40) NOT NULL,
    document_id BIGINT      REFERENCES documents(id) ON DELETE CASCADE,
    detail      VARCHAR(500),
    CONSTRAINT warning_type_valid CHECK (type IN ('HET_HIEU_LUC','SAP_HET_HIEU_LUC','DA_BI_THAY_THE','CHONG_LAN'))
);
CREATE INDEX idx_warnings_turn ON turn_warnings (turn_id);
--rollback DROP TABLE turn_warnings;
