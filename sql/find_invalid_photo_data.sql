-- 이미 저장된 무효 사진 데이터를 찾는 조회 쿼리 (PostgreSQL). 조회만 하며 데이터를 바꾸지 않는다.
-- 결과를 보고 정리 방법(삭제·수정)은 사람이 정한다.

-- 1) width·height가 0이거나 비어 있는 사진
SELECT id, diary_id, photo_code, width, height, created_at
FROM photo
WHERE width IS NULL OR height IS NULL OR width < 1 OR height < 1
ORDER BY created_at DESC;

-- 2) photoCode가 업로드 경로 형식이 아닌 사진 ("string" 같은 Swagger 기본값, 전체 URL, 확장자 없음 등)
SELECT id, diary_id, photo_code, created_at
FROM photo
WHERE photo_code !~ '^travels/[0-9]+/[^/]+\.(jpg|jpeg|png|webp|avif|heic|heif)$'
ORDER BY created_at DESC;

-- 3) 예전 공용 경로(travels/0/)에 저장된 사진 수 (이번 변경에서는 옮기지 않는다)
SELECT count(*) AS legacy_travels_0
FROM photo
WHERE photo_code LIKE 'travels/0/%';
