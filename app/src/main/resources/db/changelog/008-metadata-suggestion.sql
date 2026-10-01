--liquibase formatted sql

--changeset tracuu:008-metadata-suggestion
--comment Metadata he thong de xuat, moi gia tri kem trich doan lam can cu. Luu ca co "da doi chieu" de quan tri biet can cu nao da tim thay trong tai lieu that va can cu nao chua.
ALTER TABLE documents ADD COLUMN metadata_suggestion JSONB;

--comment Chi muc cho man "cho xac nhan": loc nhanh tai lieu con truong chua doi chieu duoc.
CREATE INDEX idx_documents_pending_metadata
    ON documents (metadata_confirmed)
    WHERE metadata_confirmed = FALSE;
--rollback ALTER TABLE documents DROP COLUMN metadata_suggestion;
