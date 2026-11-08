package backend.globber.common.service;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * 더 이상 쓰지 않는 업로드 이미지를 S3에서 지운다.
 * - DB 트랜잭션이 커밋된 뒤에 지운다(롤백되면 지우지 않는다). 트랜잭션이 없으면 바로 지운다.
 * - S3 삭제가 실패해도 요청은 성공으로 두고 ERROR 로그만 남긴다.
 * - 우리 업로드 경로(travels/, profiles/, thumbnails/)로 시작하는 키만 지운다. 예전에 잘못 저장된 값
 *   ("string", 전체 URL 등) 때문에 다른 객체를 지우지 않기 위함이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3ObjectDeleter {

    private static final List<String> DELETABLE_PREFIXES = List.of("travels/", "profiles/", "thumbnails/");

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public void deleteAfterCommit(Collection<String> keys) {
        List<String> targets = keys.stream()
            .filter(key -> key != null && DELETABLE_PREFIXES.stream().anyMatch(key::startsWith))
            .distinct()
            .toList();
        if (targets.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteAll(targets);
                }
            });
            return;
        }
        deleteAll(targets);
    }

    private void deleteAll(List<String> keys) {
        for (String key : keys) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
            } catch (RuntimeException e) {
                log.error("S3 객체 삭제 실패 - key: {}", key, e);
            }
        }
    }
}
