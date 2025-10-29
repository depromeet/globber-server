package backend.globber.support;

import backend.globber.auth.domain.Member;
import backend.globber.auth.domain.constant.AuthProvider;
import backend.globber.auth.domain.constant.Role;
import backend.globber.city.domain.City;
import backend.globber.diary.domain.Diary;
import backend.globber.diary.domain.Photo;
import backend.globber.membertravel.domain.MemberTravel;
import backend.globber.membertravel.domain.MemberTravelCity;

import java.util.Collections;
import java.util.List;

public class TestEntityFactory {

    public static Member createMember(String email, String name) {
        return Member.of(email, name, "password", AuthProvider.KAKAO, List.of(Role.ROLE_USER));
    }

    public static City createCity(String name, String country, String code, double lat, double lng) {
        return City.builder()
                .cityName(name)
                .countryName(country)
                .countryCode(code)
                .lat(lat)
                .lng(lng)
                .build();
    }

    public static MemberTravel createMemberTravel(Member member) {
        return MemberTravel.builder()
                .member(member)
                .build();
    }

    public static MemberTravelCity createMemberTravelCity(MemberTravel memberTravel, City city) {
        return MemberTravelCity.builder()
                .memberTravel(memberTravel)
                .city(city)
                .build();
    }

    public static Diary createDiary(MemberTravelCity mtc, String text) {
        return Diary.builder()
                .memberTravelCity(mtc)
                .text(text)
                .emojis(Collections.emptyList())
                .build();
    }

    public static Photo createPhoto(Diary diary, String code) {
        return Photo.builder()
                .photoCode(code)
                .diary(diary)
                .build();
    }
}
