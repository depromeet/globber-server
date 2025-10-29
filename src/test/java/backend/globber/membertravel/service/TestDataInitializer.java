package backend.globber.membertravel.service;

import backend.globber.auth.domain.Member;
import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;
import backend.globber.support.TestEntityFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TestDataInitializer {

    private final EntityManager em;

    public TestDataInitializer(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public Long initTravelData() {
        Member member = TestEntityFactory.createMember("test@kakao", "테스트유저");
        em.persist(member);

        City tokyo = TestEntityFactory.createCity("도쿄", "일본", "JPN", 35.6895, 139.6917);
        City osaka = TestEntityFactory.createCity("오사카", "일본", "JPN", 34.6937, 135.5023);
        em.persist(tokyo);
        em.persist(osaka);

        MemberTravel memberTravel = TestEntityFactory.createMemberTravel(member);
        em.persist(memberTravel);

        MemberTravelCity tokyoMtc = TestEntityFactory.createMemberTravelCity(memberTravel, tokyo);
        MemberTravelCity osakaMtc = TestEntityFactory.createMemberTravelCity(memberTravel, osaka);
        em.persist(tokyoMtc);
        em.persist(osakaMtc);

        Diary diary = TestEntityFactory.createDiary(tokyoMtc, "도쿄 여행기");
        em.persist(diary);

        List.of("tokyo_1", "tokyo_2")
                .forEach(code -> em.persist(TestEntityFactory.createPhoto(diary, code)));

        em.flush();
        em.clear();

        return member.getId();
    }
}
