package com.lsototalbouw.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod_security_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.h2.console.enabled=false",
        "app.h2-console.enabled=false",
        "app.demo-login.enabled=false",
        "app.upload.documents-dir=target/prod-security-it-documents",
        "app.bootstrap.company-name=LSOTOTALBOUW",
        "app.bootstrap.admin-full-name=Production Security Owner",
        "app.bootstrap.admin-email=prod-security@test.local",
        "app.bootstrap.admin-password=ProdSecurity123!"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void productionLoginDoesNotExposeDemoCredentials() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Usuario demo"))))
                .andExpect(content().string(not(containsString("Admin123!"))));
    }

    @Test
    void productionDoesNotExposeH2Console() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/h2-console")
                        .with(user("prod-security@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void productionHealthEndpointIsPublicButDoesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")))
                .andExpect(content().string(not(containsString("documentStorage"))))
                .andExpect(content().string(not(containsString("DOCUMENTS_DIR"))))
                .andExpect(content().string(not(containsString("prod-security-it-documents"))))
                .andExpect(content().string(not(containsString("path"))));
    }

    @Test
    void productionResponsesIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'self'")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("script-src 'self' 'unsafe-inline'"))))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void productionRejectsStateChangingRequestsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/settings/company")
                        .with(user("prod-security@test.local").roles(SecurityRoles.OWNER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void productionLoginCreatesSecureSessionCookie() throws Exception {
        mockMvc.perform(formLogin()
                        .user("prod-security@test.local")
                        .password("ProdSecurity123!"))
                .andExpect(authenticated())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        org.assertj.core.api.Assertions.assertThat(environment.getProperty(
                "server.servlet.session.cookie.secure", Boolean.class)).isTrue();
        org.assertj.core.api.Assertions.assertThat(environment.getProperty(
                "server.servlet.session.cookie.name")).isEqualTo("LSOTOTALBOUWSESSION");
    }
}
