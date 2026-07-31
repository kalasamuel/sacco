package com.kimwanyi.controller;

import jakarta.faces.context.FacesContext;
import com.kimwanyi.exception.AuthenticationException;
import com.kimwanyi.util.FacesMessageUtil;
import com.kimwanyi.dao.UserAccountDAO;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.LoggedInUserDto;
import com.kimwanyi.model.dto.forms.LoginForm;
import com.kimwanyi.model.enums.AuditAction;
import com.kimwanyi.service.interfaces.AuditLogService;
import com.kimwanyi.service.interfaces.AuthenticationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loginBean")
@RequestScope
public class LoginBean {

    private final AuthenticationService authenticationService;
    private final UserSessionBean userSessionBean;
    private final AuditLogService auditLogService;
    private final UserAccountDAO userAccountDAO;

    private LoginForm loginForm = new LoginForm();

    public LoginBean(AuthenticationService authenticationService, 
                     UserSessionBean userSessionBean,
                     AuditLogService auditLogService,
                     UserAccountDAO userAccountDAO) {
        this.authenticationService = authenticationService;
        this.userSessionBean = userSessionBean;
        this.auditLogService = auditLogService;
        this.userAccountDAO = userAccountDAO;
    }

    public LoginForm getLoginForm() {
        return loginForm;
    }

    public void setLoginForm(LoginForm loginForm) {
        this.loginForm = loginForm;
    }

    public String login() {
        try {
            LoggedInUserDto loggedInUser = authenticationService.authenticate(loginForm);
            userSessionBean.setLoggedInUser(loggedInUser);
            return loggedInUser.getRoles().contains("ADMIN")
                    ? "/admin/dashboard?faces-redirect=true"
                    : "/members/dashboard?faces-redirect=true";
        } catch (AuthenticationException e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public String logout() {
        if (userSessionBean.isLoggedIn()) {
            UserAccount userAccount = userAccountDAO.findById(userSessionBean.getLoggedInUser().getId()).orElse(null);
            if (userAccount != null) {
                auditLogService.record(userAccount, AuditAction.LOGOUT, "UserAccount", userAccount.getId(), null);
            }
        }
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }
}
