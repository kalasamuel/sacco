package com.pahappa.sacco.security;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.exception.UnauthorizedException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

// Service-layer role enforcement, second independent RBAC check
public final class RoleGuard {

    private RoleGuard() {
    }

    public static void require(User actingUser, Role... allowedRoles) {
        if (actingUser == null) {
            throw new UnauthorizedException("No authenticated user context.");
        }
        Set<Role> allowed = EnumSet.noneOf(Role.class);
        allowed.addAll(Arrays.asList(allowedRoles));
        if (!allowed.contains(actingUser.getRole())) {
            throw new UnauthorizedException(
                    "User role " + actingUser.getRole() + " is not permitted to perform this action.");
        }
    }
}
