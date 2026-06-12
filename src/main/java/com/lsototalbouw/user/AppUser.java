package com.lsototalbouw.user;

import com.lsototalbouw.common.entity.BaseEntity;
import com.lsototalbouw.company.CompanyAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Domain entity representing a user account in the application.
 *
 * <p>Contains user profile information (name, email), authentication credentials,
 * security locking metadata (failed attempts count, lockout timestamps), and authorized roles
 * for system operations. Linked directly to a specific {@link CompanyAccount} tenant.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_company_active_email", columnList = "company_account_id, active, email")
})
public class AppUser extends BaseEntity {

    /** The tenant company account this user belongs to. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_account_id", nullable = false)
    private CompanyAccount companyAccount;

    /** The user's full name. */
    @Column(nullable = false, length = 120)
    private String fullName;

    /** The unique email address used as the username during authentication. */
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    /** The bcrypt-encoded password hash. */
    @Column(nullable = false)
    private String password;

    /** Counter tracking consecutive failed authentication attempts. Resets on successful login. */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** The expiration timestamp for a temporary login lockout. If null, the account is not locked. */
    private LocalDateTime lockedUntil;

    /** The timestamp of the user's last successful login. */
    private LocalDateTime lastLoginAt;

    /** The set of granted roles defining authorization bounds for the user. Loaded eagerly for security checks. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Protected constructor required by JPA.
     */
    protected AppUser() {
    }

    /**
     * Initializes a new user account with basic profile and credential details.
     *
     * @param companyAccount the company tenant
     * @param fullName       the user's full name
     * @param email          the user's email address
     * @param password       the encoded password hash
     */
    public AppUser(CompanyAccount companyAccount, String fullName, String email, String password) {
        this.companyAccount = companyAccount;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public CompanyAccount getCompanyAccount() {
        return companyAccount;
    }

    /**
     * Updates the user's profile credentials.
     *
     * @param fullName the updated full name
     * @param email    the updated email address
     */
    public void updateProfile(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    /**
     * Updates the user's password hash.
     *
     * @param password the updated encoded password hash
     */
    public void updatePassword(String password) {
        this.password = password;
    }

    /**
     * Checks if the user's account is currently locked out due to excessive failed attempts.
     *
     * @return {@code true} if a lock timer is present and active, otherwise {@code false}
     */
    public boolean isLoginLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Increments the count of failed login attempts and sets a lockout timer if limits are exceeded.
     *
     * @param maxAttempts the threshold of failed attempts before a lockout is enforced
     * @param lockMinutes the duration of the lockout penalty in minutes
     */
    public void recordFailedLogin(int maxAttempts, int lockMinutes) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    /**
     * Resets failed login counters and lockout timers, updating the last login timestamp.
     */
    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = LocalDateTime.now();
    }

    /**
     * Manually unlocks an account, resetting failed attempts counters and lockout timers.
     */
    public void unlockLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
