package com.trend_now.backend.config.auth;

import com.trend_now.backend.member.domain.Members;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * CustomUserDetails 클래스 역할 - Spring Security 인증 과정에서 사용자 정보를 담는 역할 - Spring Security 에서는
 * UserDetails 타입을 사용하므로 직접적인 Members 객체로 보다는 UserDetails 인터페이스의 구현체인 CustomUserDetails를 정의해서 사용하는
 * 것이 유지보수에 용이
 */

@Getter
@ToString
public class CustomUserDetails implements UserDetails {

    private final Members members;

    public CustomUserDetails(Members members) {
        this.members = members;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 기본 권한은 USER 부여
        return List.of(new SimpleGrantedAuthority(members.getRole().getAuthority()));
    }

    @Override
    public String getPassword() {
        // OAuth2를 이용한 로그인이므로 password는 미존재
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(members.getId());
    }

    /**
     * 아래 메서드들은 전부 true로 하여 정상적인 계정임을 기저로 가져갑니다. - 추후 요구사항 중 계정 정지, 비활성화 등이 생기면 구현 필요
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
