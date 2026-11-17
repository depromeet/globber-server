package backend.globber.diary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import backend.globber.diary.controller.dto.DiaryResponse;
import backend.globber.diary.domain.Photo;
import backend.globber.support.TestEntityFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DiaryDisplayResponseTest {
    @Test
    void publicDiaryUsesFullDisplayKeyButKeepsSmallThumbnailAndStoredOriginal() {
        var member = TestEntityFactory.createMember("display@test", "display");
        var travel = TestEntityFactory.createMemberTravel(member);
        var city = TestEntityFactory.createCity("test", "test", "JPN", 1.0, 1.0);
        var diary = TestEntityFactory.createDiary(TestEntityFactory.createMemberTravelCity(travel, city), "test");
        ReflectionTestUtils.setField(diary, "createdAt", java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(diary, "updatedAt", java.time.LocalDateTime.now());
        diary.getPhotos().add(Photo.builder().id(1L).photoCode("travels/0/source.heic")
            .thumbnailCode("thumbnails/small.webp").displayCode("thumbnails/heic-display/full.webp")
            .diary(diary).build());
        var service = mock(DiaryServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "s3UrlResolver", new backend.globber.common.util.S3UrlResolver("https://bucket.example"));
        DiaryResponse response = ReflectionTestUtils.invokeMethod(service, "toDiaryResponse", diary, List.of(), true);
        assertThat(response.photos().getFirst().photoCode()).isEqualTo("thumbnails/heic-display/full.webp");
        assertThat(response.photos().getFirst().thumbnailCode()).isEqualTo("thumbnails/small.webp");
        assertThat(response.photos().getFirst().photoUrl()).isEqualTo("https://bucket.example/thumbnails/heic-display/full.webp");
        assertThat(response.photos().getFirst().thumbnailUrl()).isEqualTo("https://bucket.example/thumbnails/small.webp");
        assertThat(diary.getPhotos().getFirst().getPhotoCode()).isEqualTo("travels/0/source.heic");
    }
}
