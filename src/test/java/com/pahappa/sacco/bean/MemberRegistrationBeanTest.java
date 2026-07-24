package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberRegistrationBeanTest {

    @Test
    void roleLabelReturnsHumanReadableNameForSupportedRoles() {
        MemberRegistrationBean bean = new MemberRegistrationBean();

        assertEquals("Member", bean.roleLabel(Role.MEMBER));
        assertEquals("Cashier", bean.roleLabel(Role.CASHIER));
        assertEquals("Loan Officer", bean.roleLabel(Role.LOAN_OFFICER));
        assertEquals("Administrator", bean.roleLabel(Role.ADMIN));
    }
}
