package com.pahappa.sacco.filter;

import com.pahappa.sacco.util.EntityManagerProvider;
import com.pahappa.sacco.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.servlet.*;
import java.io.IOException;

public class PersistenceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        EntityManager em = JpaUtil.createEntityManager();
        EntityManagerProvider.bind(em);
        try {
            chain.doFilter(request, response);
        } finally {
            EntityManagerProvider.unbind();
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public void destroy() {
    }
}
