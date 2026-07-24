package com.pahappa.sacco.filter;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.security.SessionUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


// Enforces authentication and role-based access control for every request under /app/*
public class AuthenticationFilter implements Filter {

    private static final Map<String, Set<Role>> PATH_ROLE_MAP = Map.of(
            "/app/admin/", EnumSet.of(Role.ADMIN),
            "/app/officer/", EnumSet.of(Role.ADMIN, Role.LOAN_OFFICER),
            "/app/cashier/", EnumSet.of(Role.ADMIN, Role.CASHIER),
            "/app/member/", EnumSet.of(Role.MEMBER),
            "/app/common/", EnumSet.allOf(Role.class)
    );

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (!SessionUtil.isAuthenticated(request)) {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }

        Set<Role> allowedRoles = resolveAllowedRoles(path);
        if (allowedRoles == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean authorized = SessionUtil.getCurrentUser(request) != null
                && allowedRoles.contains(SessionUtil.getCurrentUser(request).getRole());

        if (!authorized) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(req, res);
    }

    private Set<Role> resolveAllowedRoles(String path) {
        for (Map.Entry<String, Set<Role>> entry : PATH_ROLE_MAP.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public void destroy() {
    }
}
