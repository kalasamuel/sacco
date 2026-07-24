package com.pahappa.sacco.bean;

import com.pahappa.sacco.security.CsrfUtil;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Named("csrfBean")
@RequestScoped
public class CsrfBean {

    public String getToken() {
        HttpServletRequest request =
                (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return CsrfUtil.getOrCreateToken(request);
    }

    public void setToken(String token) {
        // validation is in CsrfFilter
    }
}
