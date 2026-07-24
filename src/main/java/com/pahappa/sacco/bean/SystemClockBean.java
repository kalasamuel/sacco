package com.pahappa.sacco.bean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.util.Date;

@Named("clock")
@RequestScoped
public class SystemClockBean {

    public Date getNow() {
        return new Date();
    }
}
