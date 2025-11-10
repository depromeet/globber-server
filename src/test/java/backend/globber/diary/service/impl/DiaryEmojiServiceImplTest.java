package backend.globber.diary.service.impl;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.city.domain.City;
import backend.globber.diary.controller.dto.EmojiResponse;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.DiaryEmoji;
import backend.globber.diary.repository.DiaryEmojiRepository;
import backend.globber.diary.service.DiaryEmojiService;
import backend.globber.exception.spec.DiaryNotFoundException;
import backend.globber.exception.spec.DuplicateEmojiException;
import backend.globber.exception.spec.InvalidEmojiException;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.PostgresTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresTestConfig.class)
class DiaryEmojiServiceIntegrationTest {

    @Autowired
    private DiaryEmojiRepository emojiRepository;

    @Autowired
    private DiaryEmojiService emojiService;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Diary diary;

    @BeforeEach
    void setUp() {
        truncateDatabase();
        databaseInitialize();
    }

    private void databaseInitialize() {
        TransactionStatus tx2 = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Member member = Member.of("test@globber.com", "테스터", "password",
                    AuthProvider.KAKAO, List.of(Role.ROLE_USER), "123456");

            City city = City.builder()
                    .cityName("서울")
                    .countryName("대한민국")
                    .countryCode("KOR")
                    .lat(37.5665)
                    .lng(126.9780)
                    .build();

            MemberTravel travel = MemberTravel.builder().member(member).build();
            MemberTravelCity mtc = MemberTravelCity.builder().memberTravel(travel).city(city).build();

            em.persist(member);
            em.persist(city);
            em.persist(travel);
            em.persist(mtc);

            diary = Diary.builder()
                    .memberTravelCity(mtc)
                    .text("테스트 여행기록")
                    .build();
            em.persist(diary);
            em.flush();

            transactionManager.commit(tx2);
        } catch (Exception e) {
            transactionManager.rollback(tx2);
            throw e;
        }
    }

    private void truncateDatabase() {
        TransactionStatus tx1 = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            em.createNativeQuery("""
                        TRUNCATE TABLE diary_emoji, diary, member_travel_city, member_travel, city, member
                        RESTART IDENTITY CASCADE
                    """).executeUpdate();
            transactionManager.commit(tx1);
        } catch (Exception e) {
            transactionManager.rollback(tx1);
            throw e;
        }
    }

    @Test
    @DisplayName("정상적으로 이모지를 등록할 수 있다.")
    void registerEmoji_success() {
        EmojiResponse emoji = emojiService.registerEmoji(diary.getId(), "smile", "😊");

        assertThat(emoji.count()).isEqualTo(0L);
        assertThat(emoji.code()).isEqualTo("smile");
        assertThat(emoji.glyph()).isEqualTo("😊");
    }

    @Test
    @DisplayName("존재하지 않는 다이어리에는 이모지를 등록할 수 없다.")
    void registerEmoji_diaryNotFound() {
        assertThatThrownBy(() ->
                emojiService.registerEmoji(999L, "cry", "😭")
        ).isInstanceOf(DiaryNotFoundException.class);
    }

    @Test
    @DisplayName("중복된 이모지 등록 시 DuplicateEmojiException 발생")
    void registerEmoji_duplicate() {
        emojiService.registerEmoji(diary.getId(), "smile", "😊");

        assertThatThrownBy(() ->
                emojiService.registerEmoji(diary.getId(), "smile", "😊")
        ).isInstanceOf(DuplicateEmojiException.class);
    }

    @Test
    @DisplayName("등록된 이모지의 count를 1 증가시킬 수 있다.")
    void pressEmoji_success() {
        emojiService.registerEmoji(diary.getId(), "heart", "❤️");
        emojiService.pressEmoji(diary.getId(), "heart");

        DiaryEmoji updated = emojiRepository.findByDiaryIdAndCode(diary.getId(), "heart").orElseThrow();
        assertThat(updated.getCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 이모지에 리액션 시 InvalidEmojiException 발생")
    void pressEmoji_invalid() {
        assertThatThrownBy(() ->
                emojiService.pressEmoji(diary.getId(), "ghost")
        ).isInstanceOf(InvalidEmojiException.class);
    }

    @Test
    @DisplayName("이모지 목록은 count 내림차순, 생성순 오름차순으로 조회된다.")
    void getEmojis_orderedList() {
        emojiService.registerEmoji(diary.getId(), "smile", "😊");
        emojiService.registerEmoji(diary.getId(), "cry", "😭");

        emojiService.pressEmoji(diary.getId(), "cry");
        emojiService.pressEmoji(diary.getId(), "cry");

        List<EmojiResponse> emojis = emojiService.getEmojis(diary.getId());

        assertThat(emojis).hasSize(2);
        assertThat(emojis.get(0).code()).isEqualTo("cry");
        assertThat(emojis.get(1).code()).isEqualTo("smile");
    }

    @Test
    @DisplayName("100개의 동시에 누르기 요청이 들어와도 count가 정확히 100이 되어야 한다.")
    void pressEmoji_concurrency() throws InterruptedException {
        emojiService.registerEmoji(diary.getId(), "fire", "🔥");

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    emojiService.pressEmoji(diary.getId(), "fire");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Thread.sleep(300);
        em.clear();

        DiaryEmoji emoji = emojiRepository.findByDiaryIdAndCode(diary.getId(), "fire").orElseThrow();
        assertThat(emoji.getCount()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("여러 이모지를 동시에 누를 때 각각의 count가 정확히 누적된다.")
    void pressEmoji_multipleConcurrency() throws InterruptedException {
        emojiService.registerEmoji(diary.getId(), "heart", "❤️");
        emojiService.registerEmoji(diary.getId(), "cry", "😭");
        emojiService.registerEmoji(diary.getId(), "fire", "🔥");

        int threadCount = 150;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String code = switch (i % 3) {
                case 0 -> "heart";
                case 1 -> "cry";
                default -> "fire";
            };
            executor.submit(() -> {
                try {
                    emojiService.pressEmoji(diary.getId(), code);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Thread.sleep(300);
        em.clear();

        List<DiaryEmoji> emojis = emojiRepository.findAllByDiaryIdOrderByCountDescCreatedAtAsc(diary.getId());
        long total = emojis.stream().mapToLong(DiaryEmoji::getCount).sum();

        assertThat(total).isEqualTo(150);
        assertThat(emojis).extracting(DiaryEmoji::getCount).allMatch(c -> c == 50);
    }
}
