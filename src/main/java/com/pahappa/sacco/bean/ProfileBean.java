package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.service.UserService;
import com.pahappa.sacco.util.FacesMessageUtil;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
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
    private UserService userService;
    @Inject
    private CurrentUserBean currentUser;

    private Member member;
    private String phone;
    private String email;
    private String requestedUsername;
    private String newPassword;
    private String confirmPassword;
    private String usernameAvailabilityMessage;

    @PostConstruct
    public void init() {
        User authenticatedUser = currentUser.getUser();
        if (authenticatedUser == null) {
            return;
        }
        Long memberId = authenticatedUser.getMemberProfile() != null ? authenticatedUser.getMemberProfile().getId() : null;
        if (memberId == null) {
            return;
        }
        this.member = memberService.findById(memberId);
        if (this.member != null) {
            this.phone = this.member.getPhone();
            this.email = this.member.getEmail();
        }
        this.requestedUsername = authenticatedUser.getUsername();
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

    public void saveCredentials() {
        try {
            if (newPassword == null || newPassword.isBlank()) {
                throw new BusinessRuleViolationException("New password must not be empty.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new BusinessRuleViolationException("Password and confirmation do not match.");
            }
            User updated = userService.updateUsernameAndPassword(currentUser.getUser(), requestedUsername, newPassword);
            currentUser.getUser().setUsername(updated.getUsername());
            FacesMessageUtil.addInfo("Your username and password have been updated successfully.");
            usernameAvailabilityMessage = null;
            newPassword = null;
            confirmPassword = null;
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to change this profile.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public void checkUsernameAvailability(AjaxBehaviorEvent event) {
        if (requestedUsername == null || requestedUsername.isBlank()) {
            usernameAvailabilityMessage = null;
            return;
        }
        if (requestedUsername.equalsIgnoreCase(currentUser.getUser().getUsername())) {
            usernameAvailabilityMessage = "This username is already your current login value.";
            return;
        }
        usernameAvailabilityMessage = userService.usernameExists(requestedUsername)
                ? "This username is already taken. Please choose another." : "This username is available.";
    }

    public Member getMember() { return member; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRequestedUsername() { return requestedUsername; }
    public void setRequestedUsername(String requestedUsername) { this.requestedUsername = requestedUsername; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getUsernameAvailabilityMessage() { return usernameAvailabilityMessage; }
}
