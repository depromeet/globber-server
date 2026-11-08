package backend.globber.profile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.RefreshToken;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.RefreshTokenRedisRepository;
import backend.globber.auth.util.JwtTokenProvider;
import backend.globber.city.domain.City;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.common.service.UploadedImageValidator;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.PostgresTestConfig;
import backend.globber.support.RedisTestConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = RedisTestConfig.Initializer.class)
@Import({PostgresTestConfig.class, RedisTestConfig.class})
@DirtiesContext
class MemberS3CleanupTest {

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @MockitoBean
    private UploadedImageValidator uploadedImageValidator;

    @MockitoBean
    private S3Client s3Client;

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRepository;

    private Long memberId;
    private String email;
    private String token;
    private String photoKey;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        String id = UUID.randomUUID().toString();
        email = "cleanup-" + id + "@example.com";
        transactionTemplate.executeWithoutResult(status -> {
            Member member = Member.of(email, "테스터", "password", AuthProvider.KAKAO,
                List.of(Role.ROLE_USER), id.substring(0, 6));
            em.persist(member);
            memberId = member.getId();
            member.changeProfileImage("profiles/" + memberId + "/old.jpg");

            City city = City.builder().cityName("서울" + id).countryName("대한민국")
                .countryCode("KOR").lat(37.5).lng(127.0).build();
            MemberTravel travel = MemberTravel.builder().member(member).build();
            MemberTravelCity mtc = MemberTravelCity.builder().memberTravel(travel).city(city).build();
            em.persist(city);
            em.persist(travel);
            em.persist(mtc);
            Diary diary = Diary.builder().memberTravelCity(mtc).text("기록").build();
            em.persist(diary);
            photoKey = "travels/" + memberId + "/photo.jpg";
            em.persist(Photo.builder().photoCode(photoKey).width(1L).height(1L).displayOrder(0).diary(diary).build());
        });
        token = jwtTokenProvider.createAccessToken(email, List.of(Role.ROLE_USER.name()));
    }

    private List<String> deletedKeys() {
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, atLeast(0)).deleteObject(captor.capture());
        return captor.getAllValues().stream().map(DeleteObjectRequest::key).toList();
    }

    @Test
    @DisplayName("프로필 이미지를 바꾸면 이전 이미지의 S3 객체를 커밋 뒤 삭제한다")
    void changeProfileImage_deletesPreviousObject() {
        // when
        RestAssured.given().log().all()
            .header("Authorization", token)
            .contentType(ContentType.JSON)
            .body("{\"s3Key\": \"profiles/" + memberId + "/new.jpg\"}")
            .when()
            .patch("/api/v1/profiles/me/image")
            .then().log().all()
            .statusCode(200);

        // then
        assertThat(deletedKeys()).containsExactly("profiles/" + memberId + "/old.jpg");
    }

    @Test
    @DisplayName("탈퇴하면 그 회원의 기록 사진과 프로필 이미지 S3 객체를 커밋 뒤 삭제한다")
    void withdraw_deletesMembersObjects() {
        // given
        refreshTokenRepository.save(RefreshToken.of(email, jwtTokenProvider.createRefreshToken()));

        // when
        RestAssured.given().log().all()
            .header("Authorization", token)
            .when()
            .delete("/withdraw")
            .then().log().all()
            .statusCode(200);

        // then
        assertThat(deletedKeys()).containsExactlyInAnyOrder(photoKey, "profiles/" + memberId + "/old.jpg");
    }
}
