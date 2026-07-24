package com.pahappa.sacco.util;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

public final class FacesRequestUtil {

    private FacesRequestUtil() {
    }

    public static HttpServletRequest currentRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
}
