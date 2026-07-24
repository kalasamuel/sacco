package com.pahappa.sacco.bean;

import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.exception.UnauthorizedException;
import com.pahappa.sacco.service.MemberPhotoService;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.util.FacesMessageUtil;
import org.primefaces.model.file.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.pahappa.sacco.entity.Role;

@Named("memberRegistrationBean")
@ViewScoped
public class MemberRegistrationBean implements Serializable {

    @Inject
    private MemberService memberService;
    @Inject
    private MemberPhotoService memberPhotoService;
    @Inject
    private CurrentUserBean currentUser;

    private String membershipNumber;
    private String nationalId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private Role selectedRole = Role.MEMBER;
    private UploadedFile photo;
    private BigDecimal initialDeposit; // UGX

    public void register() {
        try {
            var result = memberService.registerMemberWithCredentials(membershipNumber, nationalId, firstName, lastName,
                    phone, email, initialDeposit, currentUser.getUser(), selectedRole);
            Long newMemberId = result.getMember().getId();

            if (photo != null && photo.getSize() > 0) {
                try {
                    memberPhotoService.uploadPhoto(newMemberId, photo.getInputStream(), photo.getFileName(),
                            photo.getSize(), currentUser.getUser());
                } catch (IOException e) {
                    FacesMessageUtil.addWarn("Member registered, but the photo could not be saved: " + e.getMessage());
                }
            }

            FacesMessageUtil.addInfo("Member " + firstName + " " + lastName + " registered successfully. "
                    + "Temporary login credentials have been emailed to " + email + ".");
            clearForm();
        } catch (UnauthorizedException e) {
            FacesMessageUtil.addError("You are not permitted to register new members.");
        } catch (BusinessRuleViolationException e) {
            FacesMessageUtil.addError(e.getMessage());
        }
    }

    public void checkNationalIdDuplicate(AjaxBehaviorEvent event) {
        if (nationalId != null && !nationalId.isBlank() && memberService.isNationalIdTaken(nationalId)) {
            FacesContext.getCurrentInstance().addMessage("registerForm:nationalId",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "This National ID is already registered.", null));
        }
    }

    public void checkMembershipNumberDuplicate(AjaxBehaviorEvent event) {
        if (membershipNumber != null && !membershipNumber.isBlank() && memberService.isMembershipNumberTaken(membershipNumber)) {
            FacesContext.getCurrentInstance().addMessage("registerForm:membershipNumber",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "This membership number is already in use.", null));
        }
    }

    public void generateMembershipNumberIfBlank() {
        if ((membershipNumber == null || membershipNumber.isBlank())
                && firstName != null && !firstName.isBlank()
                && lastName != null && !lastName.isBlank()) {
            membershipNumber = memberService.generateMembershipNumber(firstName, lastName);
        }
    }

    private void clearForm() {
        membershipNumber = null;
        nationalId = null;
        firstName = null;
        lastName = null;
        phone = null;
        email = null;
        selectedRole = Role.MEMBER;
        photo = null;
        initialDeposit = null;
    }

    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getSelectedRole() { return selectedRole; }
    public void setSelectedRole(Role selectedRole) { this.selectedRole = selectedRole; }
    public UploadedFile getPhoto() { return photo; }
    public void setPhoto(UploadedFile photo) { this.photo = photo; }

    public List<Role> getAvailableRoles() {
        if (currentUser != null && currentUser.isAdmin()) {
            return Arrays.asList(Role.ADMIN, Role.LOAN_OFFICER, Role.CASHIER, Role.MEMBER);
        }
        return Collections.singletonList(Role.MEMBER);
    }

    public String roleLabel(Role role) {
        if (role == null) {
            return "Member";
        }
        switch (role) {
            case ADMIN:
                return "Administrator";
            case LOAN_OFFICER:
                return "Loan Officer";
            case CASHIER:
                return "Cashier";
            case MEMBER:
            default:
                return "Member";
        }
    }

    public BigDecimal getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(BigDecimal initialDeposit) { this.initialDeposit = initialDeposit; }
}
