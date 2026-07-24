package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.security.SessionUtil;
import com.pahappa.sacco.util.FacesRequestUtil;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

// Reads directly from the HttpSession on every call rather than caching a field
@Named("currentUser")
@SessionScoped
public class CurrentUserBean implements Serializable {

    public User getUser() {
        return SessionUtil.getCurrentUser(FacesRequestUtil.currentRequest());
    }

    public boolean isAuthenticated() {
        return getUser() != null;
    }

    public String getFullName() {
        User user = getUser();
        return user != null ? user.getFullName() : "";
    }

    public Role getRole() {
        User user = getUser();
        return user != null ? user.getRole() : null;
    }

    public boolean isAdmin() { return getRole() == Role.ADMIN; }
    public boolean isLoanOfficer() { return getRole() == Role.LOAN_OFFICER; }
    public boolean isCashier() { return getRole() == Role.CASHIER; }
    public boolean isMember() { return getRole() == Role.MEMBER; }

    public String logout() {
        SessionUtil.logout(FacesRequestUtil.currentRequest());
        return "/login.xhtml?faces-redirect=true";
    }
}
