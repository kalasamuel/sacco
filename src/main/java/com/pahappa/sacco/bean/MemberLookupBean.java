package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.service.MemberService;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("memberLookupBean")
@ViewScoped
public class MemberLookupBean implements Serializable {

    @Inject
    private MemberService memberService;

    private String searchQuery;
    private List<Member> results;
    private Member selectedMember;

    public void search() {
        results = memberService.search(searchQuery);
        selectedMember = null;
    }

    public void select(Member member) {
        this.selectedMember = member;
    }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public List<Member> getResults() { return results; }
    public Member getSelectedMember() { return selectedMember; }
}
