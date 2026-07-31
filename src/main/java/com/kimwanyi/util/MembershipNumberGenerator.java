package com.kimwanyi.util;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.kimwanyi.dao.MemberDAO;

@Component
public class MembershipNumberGenerator {

    private final MemberDAO memberDAO;

    public MembershipNumberGenerator(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "KIM-" + year + "-";

        long sequence = memberDAO.countByMembershipNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", sequence);

        while (memberDAO.existsByMembershipNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }

        return candidate;
    }
}
