package backend.globber.diary.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.city.domain.City;
import backend.globber.city.service.RedisWarmUp;
import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.DiaryEmoji;
import backend.globber.diary.repository.DiaryEmojiRepository;
import backend.globber.diary.service.DiaryEmojiService;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.InvalidEmojiCodeException;
import backend.globber.exception.spec.InvalidEmojiException;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.PostgresTestConfig;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(PostgresTestConfig.class)
class DiaryEmojiServiceIntegrationTest {

    private static final String FIRE = "1f525";
    private static final String HEART = "2764-fe0f";

    @MockitoBean
    private RedisWarmUp redisWarmUp;

    @Autowired
    private DiaryEmojiRepository emojiRepository;

    @Autowired
    private DiaryEmojiService emojiService;

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long diaryId;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            Member owner = member();
            City city = City.builder().cityName("서울" + UUID.randomUUID()).countryName("대한민국")
                .countryCode("KOR").lat(37.5665).lng(126.9780).build();
            MemberTravel travel = MemberTravel.builder().member(owner).build();
            MemberTravelCity mtc = MemberTravelCity.builder().memberTravel(travel).city(city).build();
            em.persist(owner);
            em.persist(city);
            em.persist(travel);
            em.persist(mtc);
            Diary diary = Diary.builder().memberTravelCity(mtc).text("테스트 여행기록").build();
            em.persist(diary);
            diaryId = diary.getId();
            ownerId = owner.getId();
        });
    }

    private Member member() {
        String id = UUID.randomUUID().toString();
        return Member.of("emoji-" + id + "@globber.com", "테스터", "password",
            AuthProvider.KAKAO, List.of(Role.ROLE_USER), id.substring(0, 6));
    }

    private Long newMemberId() {
        return transactionTemplate.execute(status -> {
            Member member = member();
            em.persist(member);
            return member.getId();
        });
    }

    private long count(String code) {
        em.clear();
        return emojiRepository.findByDiaryIdAndCode(diaryId, code).map(DiaryEmoji::getCount).orElseThrow();
    }

    @Test
    @DisplayName("이모지를 등록하면 서버가 glyph를 만들고, 등록한 사람이 누른 것으로 count 1이 된다")
    void register_countsRegistrantAsPressed() {
        // when
        EmojiResponse response = emojiService.registerEmoji(ownerId, diaryId, FIRE);

        // then
        assertThat(response.code()).isEqualTo(FIRE);
        assertThat(response.glyph()).isEqualTo("🔥");
        assertThat(response.count()).isEqualTo(1L);
        assertThat(response.pressedByMe()).isTrue();
    }

    @Test
    @DisplayName("이미 있는 이모지를 다른 사람이 등록하면 오류 없이 누른 상태가 되고, 이미 누른 사람이면 바뀌지 않는다")
    void register_existingCode_pressesOnce() {
        // given
        emojiService.registerEmoji(ownerId, diaryId, FIRE);
        Long other = newMemberId();

        // when
        EmojiResponse byOther = emojiService.registerEmoji(other, diaryId, FIRE);
        EmojiResponse again = emojiService.registerEmoji(other, diaryId, FIRE);

        // then
        assertThat(byOther.count()).isEqualTo(2L);
        assertThat(byOther.pressedByMe()).isTrue();
        assertThat(again.count()).isEqualTo(2L);
        assertThat(count(FIRE)).isEqualTo(2L);
    }

    @Test
    @DisplayName("누르기는 토글이다: 안 눌렀으면 +1, 눌렀으면 -1")
    void press_toggles() {
        // given
        emojiService.registerEmoji(ownerId, diaryId, FIRE);
        Long other = newMemberId();

        // when & then
        EmojiResponse on = emojiService.pressEmoji(other, diaryId, FIRE);
        assertThat(on.count()).isEqualTo(2L);
        assertThat(on.pressedByMe()).isTrue();

        EmojiResponse off = emojiService.pressEmoji(other, diaryId, FIRE);
        assertThat(off.count()).isEqualTo(1L);
        assertThat(off.pressedByMe()).isFalse();
    }

    @Test
    @DisplayName("count가 0이 되어도 이모지 행은 남는다")
    void press_toZero_keepsRow() {
        // given
        emojiService.registerEmoji(ownerId, diaryId, FIRE);

        // when
        EmojiResponse off = emojiService.pressEmoji(ownerId, diaryId, FIRE);

        // then
        assertThat(off.count()).isZero();
        assertThat(count(FIRE)).isZero();
    }

    @Test
    @DisplayName("누가 눌렀는지 기록이 없는 기존 count는 유지하고, 새로 누른 것만 더하고 뺀다")
    void legacyCount_isKept() {
        // given: 예전 방식으로 쌓인 count 5 (반응 기록 없음)
        emojiService.registerEmoji(ownerId, diaryId, FIRE);
        jdbcTemplate.update("update diary_emoji set count = 5 where diary_id = ? and code = ?", diaryId, FIRE);
        Long other = newMemberId();

        // when & then
        assertThat(emojiService.pressEmoji(other, diaryId, FIRE).count()).isEqualTo(6L);
        assertThat(emojiService.pressEmoji(other, diaryId, FIRE).count()).isEqualTo(5L);
    }

    @Test
    @DisplayName("등록되지 않은 이모지를 누르면 InvalidEmojiException")
    void press_unregistered_throws() {
        // when & then
        assertThatThrownBy(() -> emojiService.pressEmoji(ownerId, diaryId, FIRE))
            .isInstanceOf(InvalidEmojiException.class);
    }

    @Test
    @DisplayName("없는 기록에는 등록할 수 없다")
    void register_unknownDiary_throws() {
        // when & then
        assertThatThrownBy(() -> emojiService.registerEmoji(ownerId, 999_999_999L, FIRE))
            .isInstanceOf(DiaryNotFoundException.class);
    }

    @Test
    @DisplayName("잘못된 code는 400 예외")
    void register_invalidCode_throws() {
        // when & then
        assertThatThrownBy(() -> emojiService.registerEmoji(ownerId, diaryId, "smile"))
            .isInstanceOf(InvalidEmojiCodeException.class);
    }

    @Test
    @DisplayName("목록은 count 내림차순, 등록순이고 조회한 사람이 눌렀는지 표시한다")
    void getEmojis_ordersAndMarksPressed() {
        // given
        emojiService.registerEmoji(ownerId, diaryId, FIRE);
        Long other = newMemberId();
        emojiService.registerEmoji(other, diaryId, HEART);
        emojiService.pressEmoji(other, diaryId, FIRE);

        // when
        List<EmojiResponse> forOther = emojiService.getEmojis(other, diaryId);
        List<EmojiResponse> forOwner = emojiService.getEmojis(ownerId, diaryId);
        List<EmojiResponse> anonymous = emojiService.getEmojis(null, diaryId);

        // then
        assertThat(forOther).extracting(EmojiResponse::code).containsExactly(FIRE, HEART);
        assertThat(forOther).extracting(EmojiResponse::pressedByMe).containsExactly(true, true);
        assertThat(forOwner).extracting(EmojiResponse::pressedByMe).containsExactly(true, false);
        assertThat(anonymous).extracting(EmojiResponse::pressedByMe).containsExactly(false, false);
    }

    @Test
    @DisplayName("서로 다른 100명이 동시에 누르면 count가 정확히 1(등록) + 100이 된다")
    void press_concurrentDifferentMembers() throws InterruptedException {
        // given
        emojiService.registerEmoji(ownerId, diaryId, FIRE);
        List<Long> members = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            members.add(newMemberId());
        }
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(members.size());

        // when
        for (Long memberId : members) {
            executor.submit(() -> {
                try {
                    emojiService.pressEmoji(memberId, diaryId, FIRE);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        assertThat(count(FIRE)).isEqualTo(101L);
    }

    @Test
    @DisplayName("같은 사람이 동시에 여러 번 등록해도 count는 1만 늘어난다")
    void register_concurrentSameMember_countsOnce() throws InterruptedException {
        // given
        Long other = newMemberId();
        emojiService.registerEmoji(ownerId, diaryId, FIRE);
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threads);

        // when
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    emojiService.registerEmoji(other, diaryId, FIRE);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        assertThat(count(FIRE)).isEqualTo(2L);
    }
}
