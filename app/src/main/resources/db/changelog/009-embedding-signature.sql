--liquibase formatted sql

--changeset tracuu:009-embedding-signature
--comment Chu ky mo hinh sinh bieu dien, ghi kem TAP DOAN. Khong co cot nay thi khong cach nao phat hien viec tron hai khong gian vector: nua kho nap bang mot mo hinh, nua kia bang mo hinh khac, khoang cach giua chung van tinh ra so dep nhung vo nghia, va khong loi nao bao ra. Cot dimension di kem vi hai mo hinh cung ten khac so chieu cung la hai khong gian khac nhau.
ALTER TABLE chunk_sets ADD COLUMN embedding_signature VARCHAR(200);
ALTER TABLE chunk_sets ADD COLUMN embedding_dimension INTEGER;

--comment De trong voi tap da co: chua biet chung duoc nap bang mo hinh nao. Lan ghi doan ke tiep se dong dau chu ky dang chay vao tap con trong, con tap da co chu ky thi phai khop.
--rollback ALTER TABLE chunk_sets DROP COLUMN embedding_signature;
--rollback ALTER TABLE chunk_sets DROP COLUMN embedding_dimension;
