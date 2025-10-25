package backend.globber.bookmark.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.bookmark.controller.dto.response.BookmarkedFriendResponse;
import backend.globber.bookmark.domain.Bookmark;
import backend.globber.bookmark.repository.BookmarkRepository;
import backend.globber.bookmark.service.constant.BookmarkSortType;
import backend.globber.exception.spec.BookmarkException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    @Transactional
    public void addBookmark(final Long memberId, final Long targetMemberId) {
        if (memberId.equals(targetMemberId)) {
            throw new BookmarkException("자기 자신은 북마크할 수 없습니다.");
        }

        Member me = memberRepository.findById(memberId)
            .orElseThrow(() -> new BookmarkException("사용자를 찾을 수 없습니다."));
        Member target = memberRepository.findById(targetMemberId)
            .orElseThrow(() -> new BookmarkException("북마크 대상 사용자를 찾을 수 없습니다."));

        Bookmark bookmark = Bookmark.builder()
            .member(me)
            .targetMember(target)
            .build();

        try {
            bookmarkRepository.save(bookmark);
        } catch (DataIntegrityViolationException e) {
            throw new BookmarkException("이미 북마크한 사용자입니다.");
        }
    }

    @Transactional
    public void removeBookmark(final Long memberId, final Long targetMemberId) {
        int deletedCount = bookmarkRepository.deleteByMember_IdAndTargetMember_Id(memberId, targetMemberId);

        if (deletedCount == 0) {
            throw new BookmarkException("북마크가 존재하지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<BookmarkedFriendResponse> getBookmarkedFriends(Long memberId,
        BookmarkSortType sortType) {
        List<Bookmark> bookmarks;

        switch (sortType) {
            case NAME ->
                bookmarks = bookmarkRepository.findAllByMember_IdOrderByTargetMember_NameAsc(
                    memberId);
            case LATEST ->
                bookmarks = bookmarkRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId);
            default -> throw new BookmarkException("지원하지 않는 정렬 방식입니다.");
        }

        return bookmarks.stream()
            .map(bookmark -> {
                Member target = bookmark.getTargetMember();
                return BookmarkedFriendResponse.builder()
                    .memberId(target.getId())
                    .nickname(target.getName())
                    .profileImageUrl(target.getProfileImageUrl(s3BaseUrl))
                    .bookmarked(true)
                    .build();
            })
            .toList();
    }
}