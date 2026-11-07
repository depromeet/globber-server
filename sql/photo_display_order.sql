-- 사진 표시 순서(display_order) 컬럼 추가 (PostgreSQL)
-- 배포 전에 수동 적용한다. 적용 순서: 이 SQL 먼저 실행 → 그다음 애플리케이션 배포
-- 기존 사진은 기록별로 created_at 순서(같으면 id 순서)로 0부터 채운다.

BEGIN;

ALTER TABLE photo ADD COLUMN display_order INTEGER;

UPDATE photo p
SET display_order = ordered.rn
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY diary_id ORDER BY created_at ASC NULLS LAST, id ASC) - 1 AS rn
    FROM photo
) ordered
WHERE p.id = ordered.id;

ALTER TABLE photo ALTER COLUMN display_order SET NOT NULL;

COMMIT;
