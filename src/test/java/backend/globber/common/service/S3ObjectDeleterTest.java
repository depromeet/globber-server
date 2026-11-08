package backend.globber.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3ObjectDeleterTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3ObjectDeleter deleter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deleter, "bucketName", "test-bucket");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("트랜잭션 안에서는 바로 지우지 않고 커밋된 뒤에 지운다")
    void deleteAfterCommit_waitsForCommit() {
        // given
        TransactionSynchronizationManager.initSynchronization();

        // when
        deleter.deleteAfterCommit(List.of("travels/1/a.jpg"));

        // then: 아직 지우지 않음
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));

        // when: 커밋
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        // then
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().key()).isEqualTo("travels/1/a.jpg");
    }

    @Test
    @DisplayName("트랜잭션이 없으면 바로 지운다")
    void deleteAfterCommit_withoutTransaction_deletesNow() {
        // when
        deleter.deleteAfterCommit(List.of("profiles/1/old.jpg"));

        // then
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("우리 업로드 경로가 아닌 키는 지우지 않는다")
    void deleteAfterCommit_skipsUnknownPrefix() {
        // when
        deleter.deleteAfterCommit(List.of("string", "https://example.com/a.jpg", "", "other/1/a.jpg"));

        // then
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("S3 삭제가 실패해도 예외를 던지지 않는다")
    void deleteAfterCommit_failureIsSwallowed() {
        // given
        willThrow(S3Exception.builder().statusCode(500).build())
            .given(s3Client).deleteObject(any(DeleteObjectRequest.class));

        // when & then
        assertThatCode(() -> deleter.deleteAfterCommit(List.of("travels/1/a.jpg", "travels/1/b.jpg")))
            .doesNotThrowAnyException();
    }
}
