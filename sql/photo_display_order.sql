-- 사진 표시 순서(display_order) 컬럼 추가 (PostgreSQL)
-- 수동 적용. 적용 순서:
--   개발: 이 SQL을 개발 DB에 먼저 실행 → 그다음 develop 머지(개발 서버 자동 배포)
--   운영: 이 SQL을 운영 DB에 먼저 실행 → 그다음 main 머지(운영 배포)
-- DEFAULT 0으로 추가하므로, SQL 적용 뒤 새 버전이 배포되기 전에도 기존 앱의 사진 저장이 실패하지 않는다.
-- 여러 번 실행해도 결과가 같다(컬럼이 이미 있으면 추가를 건너뛰고, 백필은 같은 값으로 다시 채운다).
-- 기존 사진은 기록별로 created_at 순서(같으면 id 순서)로 0부터 채운다.

BEGIN;

ALTER TABLE photo ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;

UPDATE photo p
SET display_order = ordered.rn
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY diary_id ORDER BY created_at ASC NULLS LAST, id ASC) - 1 AS rn
    FROM photo
) ordered
WHERE p.id = ordered.id;

COMMIT;
