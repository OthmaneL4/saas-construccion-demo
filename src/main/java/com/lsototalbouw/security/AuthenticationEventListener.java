package com.lsototalbouw.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Application event listener catching Spring Security authentication events.
 *
 * <p>Listens for {@link AuthenticationSuccessEvent} and {@link AbstractAuthenticationFailureEvent}
 * and triggers login security policies in {@link LoginAttemptService}.
 */
@Component
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    public AuthenticationEventListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Responds to successful login events.
     *
     * @param event the Spring Security success event containing authentication principal details
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        loginAttemptService.recordSuccessfulLogin(event.getAuthentication().getName());
    }

    /**
     * Responds to failed login events.
     *
     * @param event the Spring Security failure event containing error details and user credentials
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        loginAttemptService.recordFailedLogin(
                event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName()
        );
    }
}
