package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named("profileBean")
@ViewScoped
public class ProfileBean implements Serializable {

    @Inject
    private MemberService memberService;
    @Inject
    private CurrentUserBean currentUser;

    private Member member;
    private String phone;
    private String email;

    @PostConstruct
    public void init() {
        Long memberId = currentUser.getUser().getMemberProfile().getId();
        this.member = memberService.findById(memberId);
        this.phone = member.getPhone();
        this.email = member.getEmail();
    }

    public void save() {
        try {
            member = memberService.updateProfile(member.getId(), phone, email, currentUser.getUser());
            FacesMessageUtil.addInfo("Profile updated successfully.");
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to update this profile.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public Member getMember() { return member; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
