package com.lsototalbouw.config;

import com.lsototalbouw.security.SecurityRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {

    private static final String[] FULL_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN
    };

    private static final String[] APP_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.FINANCE,
            SecurityRoles.OPERATIONS,
            SecurityRoles.INVENTORY,
            SecurityRoles.DOCUMENTS,
            SecurityRoles.AUDITOR
    };

    private static final String[] FINANCE_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.FINANCE
    };

    private static final String[] OPERATIONS_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.OPERATIONS
    };

    private static final String[] CUSTOMER_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.FINANCE,
            SecurityRoles.OPERATIONS
    };

    private static final String[] INVENTORY_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.INVENTORY,
            SecurityRoles.OPERATIONS
    };

    private static final String[] DOCUMENT_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.DOCUMENTS,
            SecurityRoles.OPERATIONS
    };

    private static final String[] AUDIT_ACCESS = {
            SecurityRoles.OWNER,
            SecurityRoles.ADMIN,
            SecurityRoles.AUDITOR
    };

    private static final String[] PUBLIC_ASSETS = {
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${app.h2-console.enabled:false}") boolean h2ConsoleEnabled)
            throws Exception {
        http
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(PUBLIC_ASSETS).permitAll();
                    if (h2ConsoleEnabled) {
                        authorize.requestMatchers("/login", "/error/**", "/h2-console/**").permitAll();
                    } else {
                        authorize.requestMatchers("/login", "/error/**").permitAll();
                    }
                    authorize.requestMatchers("/actuator/health").permitAll();
                    authorize.requestMatchers("/dashboard").hasAnyRole(APP_ACCESS);
                    authorize.requestMatchers("/notifications/**").hasAnyRole(APP_ACCESS);
                    authorize.requestMatchers("/settings/company").hasAnyRole(FULL_ACCESS);
                    authorize.requestMatchers("/users/**").hasAnyRole(FULL_ACCESS);
                    authorize.requestMatchers("/audit/**").hasAnyRole(AUDIT_ACCESS);
                    authorize.requestMatchers("/invoices/**", "/quotations/**", "/payments/**", "/expenses/**", "/profitability/**", "/receivables/**")
                            .hasAnyRole(FINANCE_ACCESS);
                    authorize.requestMatchers("/customers/**").hasAnyRole(CUSTOMER_ACCESS);
                    authorize.requestMatchers("/projects/**", "/calendar/**", "/work-logs/**")
                            .hasAnyRole(OPERATIONS_ACCESS);
                    authorize.requestMatchers("/materials/**", "/tools/**", "/suppliers/**")
                            .hasAnyRole(INVENTORY_ACCESS);
                    authorize.requestMatchers("/documents/**").hasAnyRole(DOCUMENT_ACCESS);
                    authorize.anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("LSOTOTALBOUWSESSION", "JSESSIONID"))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/error/403"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; " +
                                        "style-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com 'unsafe-inline'; " +
                                        "font-src 'self' https://cdnjs.cloudflare.com; img-src 'self' data:; frame-ancestors 'self'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));

        if (h2ConsoleEnabled) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
