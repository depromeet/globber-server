package backend.globber.auth.repository;

import backend.globber.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE member RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateAll();

    @Query(value = "SELECT * FROM member WHERE email = :email", nativeQuery = true)
    Optional<Member> findByEmailIncludingDeleted(@Param("email") String email);
}
