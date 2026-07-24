package com.pahappa.sacco.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Initializes JpaUtil's EntityManagerFactory when the app deploys, and
 * closes it cleanly on undeploy — prevents connection pool leaks across
 * redeploys, which is a common cause of Tomcat "too many connections"
 * errors during iterative development.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        JpaUtil.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        JpaUtil.shutdown();
    }
}
