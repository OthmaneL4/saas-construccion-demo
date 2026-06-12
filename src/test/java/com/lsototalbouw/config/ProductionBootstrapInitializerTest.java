package com.lsototalbouw.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lsototalbouw.company.CompanyAccountRepository;
import com.lsototalbouw.user.AppUserRepository;
import com.lsototalbouw.user.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class ProductionBootstrapInitializerTest {

    @Test
    void productionBootstrapFailsFastWhenInitialAdminCredentialsAreMissing() {
        ProductionBootstrapInitializer initializer = initializerWithCredentials("", "");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_EMAIL")
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_PASSWORD");
    }

    @Test
    void productionBootstrapFailsFastWhenInitialAdminPasswordIsTooShort() {
        ProductionBootstrapInitializer initializer = initializerWithCredentials("owner@test.local", "short");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
    }

    private ProductionBootstrapInitializer initializerWithCredentials(String adminEmail, String adminPassword) {
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.count()).thenReturn(0L);
        return new ProductionBootstrapInitializer(
                mock(CompanyAccountRepository.class),
                mock(RoleRepository.class),
                users,
                mock(PasswordEncoder.class),
                "LSOTOTALBOUW",
                "NL001234567B01",
                "info@lsototalbouw.nl",
                "+31 6 12345678",
                "Rotterdam, Nederland",
                "Production Owner",
                adminEmail,
                adminPassword
        );
    }
}
