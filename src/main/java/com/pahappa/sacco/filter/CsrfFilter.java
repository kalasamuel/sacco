package com.pahappa.sacco.filter;

import com.pahappa.sacco.security.CsrfUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// Filters POST requests to validate CSRF tokens and ensures a session token exists for non-POST requests
public class CsrfFilter implements Filter {

    private static final String TOKEN_PARAM = "csrfToken";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String submittedToken = request.getParameter(TOKEN_PARAM);
            if (!CsrfUtil.isValid(request, submittedToken)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token.");
                return;
            }
        } else {
            // Ensure a token exists for the session so the very first
            // rendered form always has one available to embed.
            CsrfUtil.getOrCreateToken(request);
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }
}
