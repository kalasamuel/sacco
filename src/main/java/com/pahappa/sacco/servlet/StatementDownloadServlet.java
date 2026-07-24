package com.pahappa.sacco.servlet;

import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.security.SessionUtil;
import com.pahappa.sacco.service.StatementService;
import com.pahappa.sacco.util.EntityManagerProvider;
import com.pahappa.sacco.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/app/common/statement")
public class StatementDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = SessionUtil.getCurrentUser(request);
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long requestedMemberId;
        try {
            requestedMemberId = Long.valueOf(request.getParameter("memberId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid memberId.");
            return;
        }

        Long effectiveMemberId;
        if (currentUser.getRole() == Role.MEMBER) {
            if (currentUser.getMemberProfile() == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            effectiveMemberId = currentUser.getMemberProfile().getId();
        } else {
            // Staff roles (CASHIER/ADMIN/LOAN_OFFICER) may request any
            // member's statement, matching how they operate at the counter.
            effectiveMemberId = requestedMemberId;
        }

        EntityManager em = JpaUtil.createEntityManager();
        EntityManagerProvider.bind(em);
        try {
            byte[] pdfBytes = statementService().generateStatement(effectiveMemberId);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"statement-" + effectiveMemberId + ".pdf\"");
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
        } finally {
            EntityManagerProvider.unbind();
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private StatementService statementService() {
        return javax.enterprise.inject.spi.CDI.current().select(StatementService.class).get();
    }
}
