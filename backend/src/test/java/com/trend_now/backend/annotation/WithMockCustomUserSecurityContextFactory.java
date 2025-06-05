package com.trend_now.backend.annotation;

import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Service;

@Service
public class WithMockCustomUserSecurityContextFactory implements
        WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Members member = Members.builder().name(annotation.username()).email("testEmail")
                .snsId("testSnsId")
                .provider(
                        Provider.TEST).build();

        CustomUserDetails principal = new CustomUserDetails(member);

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password",
                principal.getAuthorities());
        context.setAuthentication(auth);
        return context;
    }
}
