--liquibase formatted sql

--changeset tracuu:003-categories
CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE categories;

--changeset tracuu:003-documents
--comment has_no_expiry la cot RIENG: US-013 doi phan biet "khong co thoi han" voi "chua xac dinh".
CREATE TABLE documents (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                   VARCHAR(500) NOT NULL,
    original_filename       VARCHAR(400) NOT NULL,
    storage_path            VARCHAR(700) NOT NULL,
    content_hash            VARCHAR(64),
    file_size_bytes         BIGINT,
    sensitivity             INTEGER      NOT NULL DEFAULT 1,
    category_id             BIGINT       REFERENCES categories(id) ON DELETE SET NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'DANG_XU_LY',
    effective_from          DATE,
    effective_to            DATE,
    has_no_expiry           BOOLEAN      NOT NULL DEFAULT FALSE,
    supersedes_document_id  BIGINT       REFERENCES documents(id) ON DELETE SET NULL,
    topic_cluster_id        BIGINT,
    uploaded_by             BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    indexed_at              TIMESTAMPTZ,
    error_reason            VARCHAR(1000),
    metadata_confirmed      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT documents_status_valid CHECK (status IN
        ('DANG_XU_LY','DA_LAP_CHI_MUC','LOI','DA_GO','CHO_XAC_NHAN')),
    CONSTRAINT documents_sensitivity_valid CHECK (sensitivity >= 1),
    CONSTRAINT documents_expiry_coherent   CHECK (NOT (has_no_expiry AND effective_to IS NOT NULL))
);
CREATE INDEX idx_documents_status       ON documents (status);
CREATE INDEX idx_documents_category     ON documents (category_id);
CREATE INDEX idx_documents_effective_to ON documents (effective_to) WHERE effective_to IS NOT NULL;
CREATE INDEX idx_documents_supersedes   ON documents (supersedes_document_id);
CREATE INDEX idx_documents_cluster      ON documents (topic_cluster_id);
--rollback DROP TABLE documents;

--changeset tracuu:003-document-acl
--comment MOT bang cho ca cho phep lan tu choi. Tach hai bang thi luat "tu choi thang moi cho phep" phai viet o hai cho, va hai cho se lech.
CREATE TABLE document_acl (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  BIGINT      NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    subject_type VARCHAR(20) NOT NULL,
    subject_id   BIGINT      NOT NULL,
    effect       VARCHAR(12) NOT NULL,
    granted_by   BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    granted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT acl_subject_type_valid CHECK (subject_type IN ('NGUOI_DUNG','PHONG_BAN')),
    CONSTRAINT acl_effect_valid       CHECK (effect IN ('CHO_PHEP','TU_CHOI')),
    CONSTRAINT acl_unique_entry       UNIQUE (document_id, subject_type, subject_id, effect)
);
CREATE INDEX idx_acl_document ON document_acl (document_id);
CREATE INDEX idx_acl_subject  ON document_acl (subject_type, subject_id);
--rollback DROP TABLE document_acl;

--changeset tracuu:003-overlap-pairs
--comment Chong lan la quan he theo CAP. Quy uoc id nho hon dung truoc de cung mot cap khong sinh hai ban ghi nguoc chieu.
CREATE TABLE document_overlap_pairs (
    doc_a_id    BIGINT      NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    doc_b_id    BIGINT      NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    similarity  REAL        NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decision    VARCHAR(20) NOT NULL DEFAULT 'CHO',
    decided_by  BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    decided_at  TIMESTAMPTZ,
    PRIMARY KEY (doc_a_id, doc_b_id),
    CONSTRAINT overlap_order    CHECK (doc_a_id < doc_b_id),
    CONSTRAINT overlap_decision CHECK (decision IN ('CHO','THAY_THE','CUNG_TON_TAI','BAO_DONG_GIA'))
);
CREATE INDEX idx_overlap_pending ON document_overlap_pairs (decision, similarity DESC);
--rollback DROP TABLE document_overlap_pairs;

--changeset tracuu:003-chunk-sets
--comment Giu nhieu tap doan song song de nap moi chien luoc DUNG MOT LAN. Ban trien khai doanh nghiep chi co mot tap dang hoat dong.
CREATE TABLE chunk_sets (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    strategy_name VARCHAR(60) NOT NULL,
    params        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    chunk_count   INTEGER     NOT NULL DEFAULT 0,
    is_active     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_chunk_sets_one_active ON chunk_sets (is_active) WHERE is_active;
--rollback DROP TABLE chunk_sets;

--changeset tracuu:003-chunks
--comment breadcrumb giu duong dan phan cap - thu lam trich dan chi dung cho thay vi chi ten tai lieu. tsv sinh tu content_segmented (da tach tu), KHONG tu content.
CREATE TABLE chunks (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chunk_set_id      BIGINT  NOT NULL REFERENCES chunk_sets(id) ON DELETE CASCADE,
    document_id       BIGINT  NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    ordinal           INTEGER NOT NULL,
    breadcrumb        VARCHAR(700),
    content           TEXT    NOT NULL,
    content_segmented TEXT    NOT NULL,
    tsv               TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content_segmented)) STORED,
    embedding         VECTOR(768),
    token_count       INTEGER NOT NULL DEFAULT 0,
    is_table          BOOLEAN NOT NULL DEFAULT FALSE,
    table_part        INTEGER,
    table_parts_total INTEGER,
    char_start        INTEGER,
    char_end          INTEGER,
    CONSTRAINT chunks_unique_ordinal UNIQUE (chunk_set_id, document_id, ordinal)
);
CREATE INDEX idx_chunks_set_document ON chunks (chunk_set_id, document_id);
CREATE INDEX idx_chunks_tsv          ON chunks USING GIN (tsv);
CREATE INDEX idx_chunks_embedding    ON chunks USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
--rollback DROP TABLE chunks;

--changeset tracuu:003-bm25-stats
--comment BM25 tinh trong tang ung dung nen can tan suat tai lieu va do dai trung binh. Thong ke gan voi TUNG tap doan.
CREATE TABLE bm25_term_df (
    chunk_set_id BIGINT       NOT NULL REFERENCES chunk_sets(id) ON DELETE CASCADE,
    term         VARCHAR(200) NOT NULL,
    df           INTEGER      NOT NULL,
    PRIMARY KEY (chunk_set_id, term)
);
CREATE TABLE bm25_corpus (
    chunk_set_id BIGINT PRIMARY KEY REFERENCES chunk_sets(id) ON DELETE CASCADE,
    chunk_count  INTEGER NOT NULL DEFAULT 0,
    avg_length   REAL    NOT NULL DEFAULT 0
);
--rollback DROP TABLE bm25_term_df;

--changeset tracuu:003-ingestion
--comment Nap tai lieu la thao tac KHONG DONG BO: goi xong tra ma theo doi ngay, tien do doc qua duong rieng.
CREATE TABLE ingestion_batches (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tracking_id UUID         NOT NULL UNIQUE,
    created_by  BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DANG_CHAY',
    total_files INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    CONSTRAINT batch_status_valid CHECK (status IN ('DANG_CHAY','XONG','DA_HUY'))
);
CREATE TABLE ingestion_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id     BIGINT       NOT NULL REFERENCES ingestion_batches(id) ON DELETE CASCADE,
    document_id  BIGINT       REFERENCES documents(id) ON DELETE SET NULL,
    filename     VARCHAR(400) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'CHO',
    stage        VARCHAR(40),
    error_code   VARCHAR(40),
    error_reason VARCHAR(1000),
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    CONSTRAINT item_status_valid CHECK (status IN ('CHO','DANG_CHAY','XONG','LOI','DA_HUY'))
);
CREATE INDEX idx_ingestion_items_batch ON ingestion_items (batch_id);
--rollback DROP TABLE ingestion_items;
