package com.scse.curriculum.auditlog.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.auditlog.entity.AuditLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-01.7: kiem tra logic xac dinh actor trong AuditLogHibernateListener.
 *
 * Bug cu: getCurrentUserId() kiem tra `auth.getPrincipal() instanceof UserAccount`,
 * nhung principal thuc te la Spring Security User -> luon false -> actor luon null.
 * Sau khi fix: chi can lay authentication.getName() (username), khong quan tam kieu
 * cua principal.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogHibernateListenerTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AuditLogHibernateListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "objectMapper", new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUsername_shouldReturnUsername_whenAuthenticated() {
        // Gia lap dung nhu thuc te: principal la Spring Security User (khong phai UserAccount)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username("nvminh")
                        .password("x")
                        .roles("INSTRUCTOR")
                        .build(),
                null,
                java.util.List.of()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        String username = (String) ReflectionTestUtils.invokeMethod(listener, "getCurrentUsername");

        // FR-01.7 fix: truoc day luon tra ve null do sai kieu cast, gio phai ra dung username
        assertThat(username).isEqualTo("nvminh");
    }

    @Test
    void getCurrentUsername_shouldReturnNull_whenNoAuthentication() {
        String username = (String) ReflectionTestUtils.invokeMethod(listener, "getCurrentUsername");
        assertThat(username).isNull();
    }

    @Test
    void isAuditable_shouldExcludeAuditLogEntityItself() {
        boolean auditableForAuditLog = (boolean) ReflectionTestUtils.invokeMethod(
                listener, "isAuditable", AuditLog.builder().build());
        boolean auditableForOther = (boolean) ReflectionTestUtils.invokeMethod(
                listener, "isAuditable", new Object());

        assertThat(auditableForAuditLog).isFalse();
        assertThat(auditableForOther).isTrue();
    }
}
