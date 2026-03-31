package com.pharmaCx.dms.config;

import com.pharmaCx.dms.domain.enums.UserRole;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Assigns a numeric level to each UserRole for threshold comparisons.
 * OPERATOR=1 (lowest) … SYSTEM_ADMIN=6 (highest).
 */
@Component
public class RoleHierarchyConfig {

    private static final Map<UserRole, Integer> LEVELS = Map.of(
            UserRole.OPERATOR,           1,
            UserRole.ENGINEER,           2,
            UserRole.MANAGER,            3,
            UserRole.HEAD_OF_DEPARTMENT, 4,
            UserRole.DIRECTOR,           5,
            UserRole.SYSTEM_ADMIN,       6
    );

    public int getLevel(UserRole role) {
        return LEVELS.getOrDefault(role, 0);
    }

    public boolean meetsMinimum(UserRole role, UserRole minimumRole) {
        return getLevel(role) >= getLevel(minimumRole);
    }

    public boolean meetsMinimumByName(UserRole role, String minimumRoleName) {
        try {
            return meetsMinimum(role, UserRole.valueOf(minimumRoleName));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
