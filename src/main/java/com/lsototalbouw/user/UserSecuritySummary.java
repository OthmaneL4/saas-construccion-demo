package com.lsototalbouw.user;

import java.util.List;

public class UserSecuritySummary {

    private final int totalUsers;
    private final int activeUsers;
    private final int lockedUsers;
    private final int usersWithRecentFailures;
    private final int assignedRoles;

    private UserSecuritySummary(int totalUsers, int activeUsers, int lockedUsers,
                                int usersWithRecentFailures, int assignedRoles) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.lockedUsers = lockedUsers;
        this.usersWithRecentFailures = usersWithRecentFailures;
        this.assignedRoles = assignedRoles;
    }

    public static UserSecuritySummary from(List<AppUser> users) {
        int activeUsers = 0;
        int lockedUsers = 0;
        int usersWithRecentFailures = 0;
        int assignedRoles = 0;

        for (AppUser user : users) {
            if (user.isActive()) {
                activeUsers++;
            }
            if (user.isLoginLocked()) {
                lockedUsers++;
            }
            if (user.getFailedLoginAttempts() > 0) {
                usersWithRecentFailures++;
            }
            assignedRoles += user.getRoles().size();
        }

        return new UserSecuritySummary(users.size(), activeUsers, lockedUsers,
                usersWithRecentFailures, assignedRoles);
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public int getLockedUsers() {
        return lockedUsers;
    }

    public int getUsersWithRecentFailures() {
        return usersWithRecentFailures;
    }

    public int getAssignedRoles() {
        return assignedRoles;
    }
}
