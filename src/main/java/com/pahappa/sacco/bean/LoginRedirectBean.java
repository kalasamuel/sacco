package com.pahappa.sacco.bean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;

@Named("loginRedirectBean")
@RequestScoped
public class LoginRedirectBean implements Serializable {

    public void redirectToLogin() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            String contextPath = context.getExternalContext().getRequestContextPath();
            context.getExternalContext().redirect(contextPath + "/login.xhtml");
            context.responseComplete();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to redirect to login page.", e);
        }
    }
}
