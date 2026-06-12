package com.lsototalbouw.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lsototalbouw.customer.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:public_demo_read_only_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "app.upload.documents-dir=target/public-demo-read-only-it-documents"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class PublicDemoReadOnlyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customers;

    @Test
    void publicDemoAllowsLoginButBlocksStateChangingRequests() throws Exception {
        long customerCount = customers.count();

        MvcResult login = mockMvc.perform(post("/login")
                        .param("username", "demo@lsototalbouw.nl")
                        .param("password", "Demo2026!")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/customers").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/customers")
                        .session(session)
                        .param("name", "Should Not Persist")
                        .param("email", "blocked@example.test")
                        .param("phone", "+31 6 00000000")
                        .param("address", "Demo Street 1")
                        .param("city", "Rotterdam")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard?demoReadOnly"));

        assertThat(customers.count()).isEqualTo(customerCount);
    }
}
