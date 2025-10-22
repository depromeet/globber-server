package backend.globber.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.bookmark.controller.dto.response.BookmarkedFriendResponse;
import backend.globber.bookmark.domain.Bookmark;
import backend.globber.bookmark.repository.BookmarkRepository;
import backend.globber.bookmark.service.constant.BookmarkSortType;
import backend.globber.exception.spec.BookmarkException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    private Member member;
    private Member targetMember;
    private final String s3BaseUrl = "https://test-bucket.s3.amazonaws.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookmarkService, "s3BaseUrl", s3BaseUrl);

        member = Member.of(
            "test@kakao",
            "테스트유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER)
        );
        ReflectionTestUtils.setField(member, "id", 1L);

        targetMember = Member.of(
            "target@kakao",
            "타겟유저",
            "password",
            AuthProvider.KAKAO,
            List.of(Role.ROLE_USER)
        );
        ReflectionTestUtils.setField(targetMember, "id", 2L);
    }

    @Test
    @DisplayName("북마크를 추가한다")
    void addBookmark() {
        // given
        Long memberId = 1L;
        Long targetMemberId = 2L;
        given(bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.findById(targetMemberId)).willReturn(Optional.of(targetMember));

        // when
        bookmarkService.addBookmark(memberId, targetMemberId);

        // then
        verify(bookmarkRepository, times(1)).existsByMember_IdAndTargetMember_Id(memberId, targetMemberId);
        verify(memberRepository, times(1)).findById(memberId);
        verify(memberRepository, times(1)).findById(targetMemberId);
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("자기 자신을 북마크하면 예외가 발생한다")
    void addBookmarkSelf() {
        // given
        Long memberId = 1L;

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, memberId))
            .isInstanceOf(BookmarkException.class)
            .hasMessage("자기 자신은 북마크할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 북마크한 사용자를 다시 북마크하면 예외가 발생한다")
    void addBookmarkDuplicate() {
        // given
        Long memberId = 1L;
        Long targetMemberId = 2L;
        given(bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId))
            .willReturn(true);

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, targetMemberId))
            .isInstanceOf(BookmarkException.class)
            .hasMessage("이미 북마크한 사용자입니다.");
        verify(bookmarkRepository, times(1)).existsByMember_IdAndTargetMember_Id(memberId,
            targetMemberId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 북마크하면 예외가 발생한다")
    void addBookmarkMemberNotFound() {
        // given
        Long memberId = 1L;
        Long targetMemberId = 999L;
        given(bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId)).willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.findById(targetMemberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, targetMemberId))
            .isInstanceOf(BookmarkException.class)
            .hasMessage("북마크 대상 사용자를 찾을 수 없습니다.");
        verify(memberRepository, times(1)).findById(targetMemberId);
    }

    @Test
    @DisplayName("본인 계정이 존재하지 않으면 예외가 발생한다")
    void addBookmarkSelfNotFound() {
        // given
        Long memberId = 999L;
        Long targetMemberId = 2L;
        given(bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId))
            .willReturn(false);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.addBookmark(memberId, targetMemberId))
            .isInstanceOf(BookmarkException.class)
            .hasMessage("사용자를 찾을 수 없습니다.");
        verify(memberRepository, times(1)).findById(memberId);
    }

    @Test
    @DisplayName("북마크를 삭제한다")
    void removeBookmark() {
        // given
        Long memberId = 1L;
        Long targetMemberId = 2L;
        given(bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId))
            .willReturn(true);

        // when
        bookmarkService.removeBookmark(memberId, targetMemberId);

        // then
        verify(bookmarkRepository, times(1)).existsByMember_IdAndTargetMember_Id(memberId,
            targetMemberId);
        verify(bookmarkRepository, times(1)).deleteByMember_IdAndTargetMember_Id(memberId,
            targetMemberId);
    }

    @Test
    @DisplayName("존재하지 않는 북마크를 삭제하면 예외가 발생한다")
    void removeBookmarkNotFound() {
        // given
        Long memberId = 1L;
        Long targetMemberId = 2L;
        given(bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId))
            .willReturn(false);

        // when & then
        assertThatThrownBy(() -> bookmarkService.removeBookmark(memberId, targetMemberId))
            .isInstanceOf(BookmarkException.class)
            .hasMessage("북마크가 존재하지 않습니다.");
        verify(bookmarkRepository, times(1)).existsByMember_IdAndTargetMember_Id(memberId,
            targetMemberId);
    }

    @Test
    @DisplayName("북마크 목록을 최신순으로 조회한다")
    void getBookmarkedFriendsLatest() {
        // given
        Long memberId = 1L;
        Bookmark bookmark1 = createBookmark(member, targetMember);
        List<Bookmark> bookmarks = List.of(bookmark1);
        given(bookmarkRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId))
            .willReturn(bookmarks);

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(memberId,
            BookmarkSortType.LATEST);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().memberId()).isEqualTo(2L);
        assertThat(result.getFirst().nickname()).isEqualTo("타겟유저");
        assertThat(result.getFirst().bookmarked()).isTrue();
        verify(bookmarkRepository, times(1)).findAllByMember_IdOrderByCreatedAtDesc(memberId);
    }

    @Test
    @DisplayName("북마크 목록을 이름순으로 조회한다")
    void getBookmarkedFriendsName() {
        // given
        Long memberId = 1L;
        Bookmark bookmark1 = createBookmark(member, targetMember);
        List<Bookmark> bookmarks = List.of(bookmark1);
        given(bookmarkRepository.findAllByMember_IdOrderByTargetMember_NameAsc(memberId))
            .willReturn(bookmarks);

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(memberId,
            BookmarkSortType.NAME);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().memberId()).isEqualTo(2L);
        assertThat(result.getFirst().nickname()).isEqualTo("타겟유저");
        assertThat(result.getFirst().bookmarked()).isTrue();
        verify(bookmarkRepository, times(1)).findAllByMember_IdOrderByTargetMember_NameAsc(
            memberId);
    }

    @Test
    @DisplayName("프로필 이미지가 있는 북마크 목록을 조회한다")
    void getBookmarkedFriendsWithProfileImage() {
        // given
        Long memberId = 1L;
        String s3Key = "profiles/2/test-image.jpg";
        targetMember.changeProfileImage(s3Key);
        Bookmark bookmark1 = createBookmark(member, targetMember);
        List<Bookmark> bookmarks = List.of(bookmark1);
        given(bookmarkRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId))
            .willReturn(bookmarks);

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(memberId,
            BookmarkSortType.LATEST);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().profileImageUrl()).isEqualTo(s3BaseUrl + "/" + s3Key);
        verify(bookmarkRepository, times(1)).findAllByMember_IdOrderByCreatedAtDesc(memberId);
    }

    @Test
    @DisplayName("북마크 목록이 비어있으면 빈 리스트를 반환한다")
    void getBookmarkedFriendsEmpty() {
        // given
        Long memberId = 1L;
        given(bookmarkRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId))
            .willReturn(List.of());

        // when
        List<BookmarkedFriendResponse> result = bookmarkService.getBookmarkedFriends(memberId,
            BookmarkSortType.LATEST);

        // then
        assertThat(result).isEmpty();
        verify(bookmarkRepository, times(1)).findAllByMember_IdOrderByCreatedAtDesc(memberId);
    }

    private Bookmark createBookmark(Member member, Member targetMember) {
        Bookmark bookmark = Bookmark.builder()
            .member(member)
            .targetMember(targetMember)
            .build();
        ReflectionTestUtils.setField(bookmark, "id", 1L);
        return bookmark;
    }
}