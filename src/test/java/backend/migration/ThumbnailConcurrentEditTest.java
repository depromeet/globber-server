package backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import backend.globber.GlobberApplication;
import backend.globber.diary.domain.Photo;
import backend.globber.support.TestEntityFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@ContextConfiguration(classes = GlobberApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ThumbnailConcurrentEditTest {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void staleUserEditMustNotEraseThumbnailAppliedByBatch() {
        Long id = new TransactionTemplate(transactions).execute(status -> {
            var member = TestEntityFactory.createMember("thumbnail-race@test", "race");
            em.persist(member);
            var city = TestEntityFactory.createCity("test", "test", "JPN", 1.0, 1.0);
            em.persist(city);
            var travel = TestEntityFactory.createMemberTravel(member);
            em.persist(travel);
            var travelCity = TestEntityFactory.createMemberTravelCity(travel, city);
            em.persist(travelCity);
            var diary = TestEntityFactory.createDiary(travelCity, "test");
            em.persist(diary);
            var photo = TestEntityFactory.createPhoto(diary, "travels/0/race.heic");
            em.persist(photo);
            return photo.getId();
        });
        try (var userSession = emf.createEntityManager()) {
            userSession.getTransaction().begin();
            Photo stale = userSession.find(Photo.class, id);
            assertThat(stale.getThumbnailCode()).isNull();
            // The batch commits after the user's request has loaded the old null value.
            jdbc.update("update photo set thumbnail_code='thumbnails/race.webp' where id=?", id);
            jdbc.update("update photo set display_code='thumbnails/full-race.webp' where id=?", id);
            stale.updateMetadata(stale.getLat(), stale.getLng(), stale.getWidth(), stale.getHeight(),
                stale.getTakenMonth(), stale.getTag(), "edited by user");
            userSession.getTransaction().commit();
        }
        assertThat(jdbc.queryForObject("select thumbnail_code from photo where id=?", String.class, id))
            .isEqualTo("thumbnails/race.webp");
        assertThat(jdbc.queryForObject("select place_name from photo where id=?", String.class, id))
            .isEqualTo("edited by user");
        assertThat(jdbc.queryForObject("select photo_code from photo where id=?", String.class, id))
            .isEqualTo("travels/0/race.heic");
        assertThat(jdbc.queryForObject("select display_code from photo where id=?", String.class, id))
            .isEqualTo("thumbnails/full-race.webp");
    }
}
