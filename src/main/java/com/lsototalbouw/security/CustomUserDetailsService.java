package com.lsototalbouw.security;

import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService}.
 *
 * <p>Bridges Spring Security authentication routines with the application's database. It retrieves
 * persistent user data by email, checks account locking status, and converts roles to granted authorities.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public CustomUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Resolves user credential details and granted authority roles from the database.
     *
     * <p>Configures account status flags such as locked (if user is inactive or login attempt limit is exceeded)
     * and disabled (if user is inactive).
     *
     * @param username the email address identifying the user trying to authenticate (case-insensitive)
     * @return a fully populated {@link UserDetails} principal object representing the authenticated user
     * @throws UsernameNotFoundException if no user account matches the provided email address
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .toList())
                .accountLocked(!user.isActive() || user.isLoginLocked())
                .disabled(!user.isActive())
                .build();
    }
}
