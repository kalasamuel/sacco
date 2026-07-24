package com.pahappa.sacco.service;

import com.pahappa.sacco.entity.Member;

public class MemberRegistrationResult {

    private final Member member;
    private final String username;
    private final String temporaryPassword;

    public MemberRegistrationResult(Member member, String username, String temporaryPassword) {
        this.member = member;
        this.username = username;
        this.temporaryPassword = temporaryPassword;
    }

    public Member getMember() {
        return member;
    }

    public String getUsername() {
        return username;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }
}
