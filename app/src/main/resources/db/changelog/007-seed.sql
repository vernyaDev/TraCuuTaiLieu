--liquibase formatted sql

--changeset tracuu:007-seed-departments
--comment Phong ban mau de he thong dung duoc ngay sau khi cai. Quan tri sua lai theo to chuc that.
INSERT INTO departments (name, description) VALUES
    ('Nhan su',   'Quan ly nhan su, che do, tuyen dung'),
    ('Ke toan',   'Tai chinh, ke toan, thue'),
    ('Ky thuat',  'Van hanh va phat trien he thong'),
    ('Kinh doanh','Ban hang va cham soc khach hang'),
    ('Ban giam doc', 'Dieu hanh')
ON CONFLICT (name) DO NOTHING;
--rollback DELETE FROM departments;

--changeset tracuu:007-seed-categories
INSERT INTO categories (name, description) VALUES
    ('Quy che noi bo',   'Quy che, quy dinh ap dung toan cong ty'),
    ('Quy trinh',        'Quy trinh nghiep vu tung bo phan'),
    ('Bieu mau',         'Bieu mau va huong dan dien'),
    ('Van ban phap quy', 'Van ban phap luat lien quan'),
    ('Tai lieu ky thuat','Huong dan van hanh he thong'),
    ('Khac',             'Chua phan loai')
ON CONFLICT (name) DO NOTHING;
--rollback DELETE FROM categories;

--changeset tracuu:007-seed-chunkset
--comment Tap doan dang hoat dong cua ban trien khai. Ban trien khai doanh nghiep chi co MOT tap active; nhieu tap la cong cu cua tac gia khi chay ma tran thi nghiem.
INSERT INTO chunk_sets (strategy_name, params, is_active)
SELECT 'structural', '{"maxTokens":512,"overlapTokens":64,"minTokens":80}'::jsonb, TRUE
WHERE NOT EXISTS (SELECT 1 FROM chunk_sets WHERE is_active);
INSERT INTO bm25_corpus (chunk_set_id, chunk_count, avg_length)
SELECT id, 0, 0 FROM chunk_sets WHERE is_active
ON CONFLICT (chunk_set_id) DO NOTHING;
--rollback DELETE FROM chunk_sets;
