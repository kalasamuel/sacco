package com.pahappa.sacco.service;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberServiceRoleTest {

    @Test
    void adminCanAssignAnyRole() {
        MemberService service = new MemberService();
        User admin = new User("admin", "hash", "System Admin", Role.ADMIN);

        assertEquals(Role.CASHIER, service.resolveRequestedRole(admin, Role.CASHIER));
        assertEquals(Role.LOAN_OFFICER, service.resolveRequestedRole(admin, Role.LOAN_OFFICER));
    }

    @Test
    void nonAdminStaffCanOnlyRegisterMemberRole() {
        MemberService service = new MemberService();
        User cashier = new User("cashier", "hash", "Cashier", Role.CASHIER);

        assertEquals(Role.MEMBER, service.resolveRequestedRole(cashier, Role.MEMBER));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
                () -> service.resolveRequestedRole(cashier, Role.CASHIER));
        assertEquals("Only administrators may assign staff roles.", exception.getMessage());
    }
}
