-- 사진 순서 유지를 위한 display_order 컬럼 추가 마이그레이션
-- 실행 일자: 2025-11-14
-- 설명: Photo 테이블에 display_order 컬럼을 추가하여 사진의 등록 순서를 유지합니다.

-- 1. display_order 컬럼 추가 (nullable로 먼저 추가)
ALTER TABLE photo ADD COLUMN IF NOT EXISTS display_order INTEGER;

-- 2. 기존 데이터에 대한 display_order 설정
-- 각 diary_id별로 id 순서대로 1, 2, 3 값을 할당
WITH photo_with_row_number AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY diary_id ORDER BY id) AS rn
    FROM photo
)
UPDATE photo
SET display_order = photo_with_row_number.rn
FROM photo_with_row_number
WHERE photo.id = photo_with_row_number.id
AND photo.display_order IS NULL;

-- 3. NOT NULL 제약조건 추가
ALTER TABLE photo ALTER COLUMN display_order SET NOT NULL;

-- 4. 인덱스 추가 (조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_photo_diary_id_display_order
ON photo(diary_id, display_order);
