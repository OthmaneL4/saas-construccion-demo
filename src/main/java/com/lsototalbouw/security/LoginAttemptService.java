package com.lsototalbouw.security;

import com.lsototalbouw.audit.AuditAction;
import com.lsototalbouw.audit.AuditLog;
import com.lsototalbouw.audit.AuditLogRepository;
import com.lsototalbouw.company.CompanyAccount;
import com.lsototalbouw.notification.BusinessNotification;
import com.lsototalbouw.notification.BusinessNotificationRepository;
import com.lsototalbouw.user.AppUser;
import com.lsototalbouw.user.AppUserRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security service that monitors login attempts and prevents brute-force authentication attacks.
 *
 * <p>Tracks consecutive failed login attempts on a per-user basis. Once the configured limit
 * is reached, the account is temporarily locked for a specified lock period.
 * All successful, failed, and account lock events are audited in separate isolated database transactions.
 */
@Service
public class LoginAttemptService {

    private static final DateTimeFormatter LOCK_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final AppUserRepository users;
    private final AuditLogRepository auditLogs;
    private final BusinessNotificationRepository notifications;
    private final int maxAttempts;
    private final int lockMinutes;

    public LoginAttemptService(AppUserRepository users, AuditLogRepository auditLogs,
                               BusinessNotificationRepository notifications,
                               @Value("${app.security.login.max-failed-attempts:5}") int maxAttempts,
                               @Value("${app.security.login.lock-minutes:15}") int lockMinutes) {
        this.users = users;
        this.auditLogs = auditLogs;
        this.notifications = notifications;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    /**
     * Records a successful user authentication, resetting the failed attempts counter.
     *
     * <p>Runs in a new transaction context ({@link Propagation#REQUIRES_NEW}) to ensure the reset
     * and successful login audit event are saved regardless of any outer transaction status.
     *
     * @param email the email address of the authenticated user
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(String email) {
        users.findByEmailIgnoreCase(normalize(email)).ifPresent(user -> {
            user.recordSuccessfulLogin();
            resolveLockoutNotification(user);
            audit(user, AuditAction.LOGIN_SUCCESS, "Login correcto", "Acceso correcto para " + user.getEmail());
        });
    }

    /**
     * Increments the count of failed login attempts for a user and triggers a lock if the maximum limit is reached.
     *
     * <p>Runs in a new transaction context ({@link Propagation#REQUIRES_NEW}) to guarantee that the failed attempt counter
     * and security audit log are persisted even if Spring Security's authentication provider aborts the authentication request.
     *
     * @param email  the email address supplied during the failed login attempt
     * @param reason the descriptive failure reason (e.g. incorrect password, bad credentials)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(String email, String reason) {
        users.findByEmailIgnoreCase(normalize(email)).ifPresent(user -> {
            if (!user.isLoginLocked()) {
                user.recordFailedLogin(maxAttempts, lockMinutes);
            }
            if (user.isLoginLocked()) {
                notifyLockout(user);
                audit(user, AuditAction.ACCOUNT_LOCKED, "Cuenta bloqueada temporalmente",
                        "Cuenta " + user.getEmail() + " bloqueada hasta "
                                + user.getLockedUntil().format(LOCK_FORMAT));
            } else {
                audit(user, AuditAction.LOGIN_FAILURE, "Login fallido",
                        "Intento fallido " + user.getFailedLoginAttempts() + "/" + maxAttempts
                                + " para " + user.getEmail() + ". Motivo: " + trim(reason, 120));
            }
        });
    }

    /**
     * Helper method to write a security-related audit log record.
     *
     * @param user     the user related to the security event
     * @param action   the {@link AuditAction} category type
     * @param summary  a short title summarizing the authentication event
     * @param details  detailed contextual audit information
     */
    private void audit(AppUser user, AuditAction action, String summary, String details) {
        CompanyAccount company = user.getCompanyAccount();
        AuditLog log = new AuditLog(
                company,
                user,
                action,
                "Autenticacion",
                user.getId() == null ? null : user.getId().toString(),
                trim(summary, 240),
                trim(details, 1000)
        );
        auditLogs.save(log);
    }

    /**
     * Creates or refreshes a visible operations notification for account lockouts.
     *
     * @param user the locked user account
     */
    private void notifyLockout(AppUser user) {
        CompanyAccount company = user.getCompanyAccount();
        Long companyId = company.getId();
        String sourceKey = lockoutSourceKey(user);
        String lockedUntil = user.getLockedUntil() == null ? "sin fecha" : user.getLockedUntil().format(LOCK_FORMAT);
        String title = "Cuenta bloqueada por seguridad";
        String message = trim("El usuario " + user.getEmail() + " ha sido bloqueado hasta " + lockedUntil
                + " tras " + user.getFailedLoginAttempts() + " intentos fallidos.", 500);
        notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(companyId, sourceKey)
                .ifPresentOrElse(existing -> existing.updateFrom(
                                "danger", title, message, "/users", LocalDate.now()),
                        () -> notifications.save(new BusinessNotification(
                                company, sourceKey, "danger", title, message, "/users", LocalDate.now())));
    }

    /**
     * Marks a previous lockout notification as resolved when login restrictions are cleared.
     *
     * @param user the user account that has recovered access
     */
    private void resolveLockoutNotification(AppUser user) {
        notifications.findByCompanyAccountIdAndSourceKeyAndActiveTrue(
                        user.getCompanyAccount().getId(), lockoutSourceKey(user))
                .filter(BusinessNotification::isUnread)
                .ifPresent(BusinessNotification::markAsRead);
    }

    private String lockoutSourceKey(AppUser user) {
        return "security:login-lock:" + user.getId();
    }

    /**
     * Normalizes email inputs to clean case-insensitive lookups.
     *
     * @param email the raw input email
     * @return the trimmed, lowercase representation of the email
     */
    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Safe substring utility to truncate long descriptions to database column limits.
     *
     * @param value     the raw string to format
     * @param maxLength the maximum allowable characters
     * @return the truncated string if length exceeded, or the original string
     */
    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
