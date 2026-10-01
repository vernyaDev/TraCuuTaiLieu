--liquibase formatted sql

--changeset tracuu:001-extensions
--comment Phần mở rộng vector cho nhánh truy hồi ngữ nghĩa.
CREATE EXTENSION IF NOT EXISTS vector;
--rollback DROP EXTENSION IF EXISTS vector;
