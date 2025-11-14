package backend.globber.bookmark.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.bookmark.controller.dto.response.BookmarkedFriendResponse;
import backend.globber.bookmark.domain.Bookmark;
import backend.globber.bookmark.repository.BookmarkRepository;
import backend.globber.bookmark.service.constant.BookmarkSortType;
import backend.globber.exception.spec.BookmarkException;
import backend.globber.support.PostgresTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({PostgresTestConfig.class})
class BookmarkServiceIntegrationTest {

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private Member targetMember1;
    private Member targetMember2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        bookmarkRepository.deleteAll();
        memberRepository.truncateAll();

        // 회원 생성
        member = Member.of(
                "test@kakao.com",
                "테스트유저",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER),
                "123456"
        );
        memberRepository.save(member);

        targetMember1 = Member.of(
                "target1@kakao.com",
                "가나다",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER),
                "12345f"
        );
        memberRepository.save(targetMember1);

        targetMember2 = Member.of(
                "target2@kakao.com",
                "타겟유저2",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER),
                "12345d"
        );
        memberRepository.save(targetMember2);
    }

    @Test
    @DisplayName("북마크를 추가한다")
    void addBookmark() {
        // when
        bookmarkService.addBookmark(member.getId(), targetMember1.getId());

        // then
        List<Bookmark> bookmarks = bookmarkRepository.findAll();
        assertThat(bookmarks).hasSize(1);
        assertThat(bookmarks.get(0).getMember().getId()).isEqualTo(member.getId());
        assertThat(bookmarks.get(0).getTargetMember().getId()).isEqualTo(targetMember1.getId());
    }

    @Test
    @DisplayName("자기 자신을 북마크하면 예외가 발생한다")
    void addBookmarkSelf() {
        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(member.getId(), member.getId()))
                .isInstanceOf(BookmarkException.class)
                .hasMessage("자기 자신은 북마크할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 북마크한 사용자를 다시 북마크하면 예외가 발생한다")
    void addBookmarkDuplicate() {
        // given
        bookmarkService.addBookmark(member.getId(), targetMember1.getId());

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(member.getId(), targetMember1.getId()))
                .isInstanceOf(BookmarkException.class)
                .hasMessage("이미 북마크한 사용자입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 북마크하면 예외가 발생한다")
    void addBookmarkMemberNotFound() {
        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(member.getId(), 99999L))
                .isInstanceOf(BookmarkException.class)
                .hasMessage("북마크 대상 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("북마크를 삭제한다")
    void removeBookmark() {
        // given
        bookmarkService.addBookmark(member.getId(), targetMember1.getId());

        // when
        bookmarkService.removeBookmark(member.getId(), targetMember1.getId());

        // then
        List<Bookmark> bookmarks = bookmarkRepository.findAll();
        assertThat(bookmarks).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 북마크를 삭제하면 예외가 발생한다")
    void removeBookmarkNotFound() {
        // when & then
        assertThatThrownBy(() -> bookmarkService.removeBookmark(member.getId(), targetMember1.getId()))
                .isInstanceOf(BookmarkException.class)
                .hasMessage("북마크가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("북마크 목록을 최신순으로 조회한다")
    void getBookmarkedFriendsLatest() {
        // given
        bookmarkService.addBookmark(member.getId(), targetMember1.getId());
        bookmarkService.addBookmark(member.getId(), targetMember2.getId());

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(
                member.getId(),
                BookmarkSortType.LATEST
        );

        // then
        assertThat(result).hasSize(2);
        // 최신순이므로 targetMember2가 먼저
        assertThat(result.get(0).memberId()).isEqualTo(targetMember2.getId());
        assertThat(result.get(1).memberId()).isEqualTo(targetMember1.getId());
        assertThat(result.get(0).bookmarked()).isTrue();
        assertThat(result.get(1).bookmarked()).isTrue();
    }

    @Test
    @DisplayName("북마크 목록을 이름순으로 조회한다")
    void getBookmarkedFriendsName() {
        // given
        bookmarkService.addBookmark(member.getId(), targetMember2.getId()); // 타겟유저2
        bookmarkService.addBookmark(member.getId(), targetMember1.getId()); // 가나다

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(
                member.getId(),
                BookmarkSortType.NAME
        );

        // then
        assertThat(result).hasSize(2);
        // 이름순이므로 '가나다'가 먼저
        assertThat(result.get(0).nickname()).isEqualTo("가나다");
        assertThat(result.get(1).nickname()).isEqualTo("타겟유저2");
    }

    @Test
    @DisplayName("프로필 이미지가 있는 북마크 목록을 조회한다")
    void getBookmarkedFriendsWithProfileImage() {
        // given
        String s3Key = "profiles/" + targetMember1.getId() + "/test-image.jpg";
        targetMember1.changeProfileImage(s3Key);
        memberRepository.save(targetMember1);

        bookmarkService.addBookmark(member.getId(), targetMember1.getId());

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(
                member.getId(),
                BookmarkSortType.LATEST
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).profileImageUrl()).contains(s3Key);
    }

    @Test
    @DisplayName("북마크 목록이 비어있으면 빈 리스트를 반환한다")
    void getBookmarkedFriendsEmpty() {
        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(
                member.getId(),
                BookmarkSortType.LATEST
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 사용자가 각각 북마크를 추가할 수 있다")
    void multipleUsersCanBookmark() {
        // given
        Member anotherMember = Member.of(
                "another@kakao.com",
                "다른유저",
                "password",
                AuthProvider.KAKAO,
                List.of(Role.ROLE_USER),
                "123def"
        );
        memberRepository.save(anotherMember);

        // when
        bookmarkService.addBookmark(member.getId(), targetMember1.getId());
        bookmarkService.addBookmark(anotherMember.getId(), targetMember1.getId());

        // then
        List<BookmarkedFriendResponse> memberBookmarks = bookmarkService.getBookmarkedFriends(
                member.getId(),
                BookmarkSortType.LATEST
        );
        List<BookmarkedFriendResponse> anotherMemberBookmarks = bookmarkService.getBookmarkedFriends(
                anotherMember.getId(),
                BookmarkSortType.LATEST
        );

        assertThat(memberBookmarks).hasSize(1);
        assertThat(anotherMemberBookmarks).hasSize(1);
    }
}