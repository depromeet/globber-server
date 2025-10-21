package backend.globber.bookmark.service;

import backend.globber.auth.domain.Member;
import backend.globber.auth.repository.MemberRepository;
import backend.globber.bookmark.domain.Bookmark;
import backend.globber.bookmark.repository.BookmarkRepository;
import backend.globber.exception.BookmarkNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private BookmarkRepository bookmarkRepository;
    private MemberRepository memberRepository;

    @Transactional
    public void addBookmark(final Long memberId, final Long targetMemberId) {
        if (memberId.equals(targetMemberId)) {
            throw new BookmarkNotFoundException("본인은 북마크 할 수 없습니다");
        }

        if (bookmarkRepository.existsByMember_IdAndTargetMember_Id(memberId, targetMemberId)) {
            throw new BookmarkNotFoundException("이미 북마크 했습니다.");
        }

        Member me = memberRepository.findById(memberId).orElseThrow(() -> new IllegalArgumentException("내 계정이 존재하지 않습니다."));
        Member target = memberRepository.findById(targetMemberId).orElseThrow(() -> new IllegalArgumentException("대상 회원이 존재하지 않습니다."));

        Bookmark bookmark = Bookmark.builder()
            .member(me)
            .targetMember(target)
            .build();

        bookmarkRepository.save(bookmark);
    }
}
