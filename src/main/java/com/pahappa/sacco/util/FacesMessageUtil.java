package com.pahappa.sacco.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Consistent FacesMessage creation, so every bean reports errors and
 * successes the same way */
public final class FacesMessageUtil {

    private FacesMessageUtil() {
    }

    public static void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public static void addInfo(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
    }

    public static void addWarn(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, message, null));
    }
}
