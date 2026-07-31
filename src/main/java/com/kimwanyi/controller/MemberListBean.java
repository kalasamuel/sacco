package com.kimwanyi.controller;

import java.util.List;

import com.kimwanyi.model.dto.MemberDto;
import com.kimwanyi.service.interfaces.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import jakarta.annotation.PostConstruct;

@Component("memberListBean")
@RequestScope
public class MemberListBean {

    private final MemberService memberService;

    private List<MemberDto> members;
    private String keyword;

    public MemberListBean(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostConstruct
    public void init() { search(); }

    public void search() { members = memberService.search(keyword); }

    public List<MemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDto> members) {
        this.members = members;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
