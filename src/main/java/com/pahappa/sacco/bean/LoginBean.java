package com.pahappa.sacco.bean;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.security.AuthenticationService;
import com.pahappa.sacco.security.SessionUtil;
import com.pahappa.sacco.util.FacesMessageUtil;
import com.pahappa.sacco.util.FacesRequestUtil;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

@Named("loginBean")
@RequestScoped
public class LoginBean implements Serializable {

    @Inject
    private AuthenticationService authenticationService;

    private String username;
    private String password;

    public String login() {
        AuthenticationService.AuthenticationResult result =
                authenticationService.authenticate(username, password);

        if (!result.success) {
            FacesMessageUtil.addError(result.errorMessage);
            return null; // redisplay the login page with the error message
        }

        HttpServletRequest request = FacesRequestUtil.currentRequest();
        SessionUtil.login(request, result.user); // session fixation prevention happens inside here

        return redirectTargetFor(result.user);
    }

    private String redirectTargetFor(User user) {
        Role role = user.getRole();
        switch (role) {
            case ADMIN:
                return "/app/admin/dashboard.xhtml?faces-redirect=true";
            case LOAN_OFFICER:
                return "/app/officer/dashboard.xhtml?faces-redirect=true";
            case CASHIER:
                return "/app/cashier/dashboard.xhtml?faces-redirect=true";
            case MEMBER:
                return "/app/member/dashboard.xhtml?faces-redirect=true";
            default:
                return "/login.xhtml?faces-redirect=true";
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
