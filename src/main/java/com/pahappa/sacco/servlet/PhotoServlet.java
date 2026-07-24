package com.pahappa.sacco.servlet;

import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.security.SessionUtil;
import com.pahappa.sacco.service.MemberPhotoService;
import com.pahappa.sacco.service.MemberService;
import com.pahappa.sacco.util.EntityManagerProvider;
import com.pahappa.sacco.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//Streams a member's uploaded photo.
@WebServlet("/app/common/photo")
public class PhotoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = SessionUtil.getCurrentUser(request);
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long memberId;
        try {
            memberId = Long.valueOf(request.getParameter("memberId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        EntityManager em = JpaUtil.createEntityManager();
        EntityManagerProvider.bind(em);
        try {
            MemberService memberService = cdi(MemberService.class);
            MemberPhotoService photoService = cdi(MemberPhotoService.class);

            Member member = memberService.findById(memberId);
            if (member.getPhotoPath() == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Path file = photoService.resolvePhotoFile(member.getPhotoPath());
            if (!Files.exists(file)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String contentType = member.getPhotoPath().toLowerCase().endsWith(".png")
                    ? "image/png" : "image/jpeg";
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "private, max-age=300");
            Files.copy(file, response.getOutputStream());
        } finally {
            EntityManagerProvider.unbind();
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private <T> T cdi(Class<T> type) {
        return javax.enterprise.inject.spi.CDI.current().select(type).get();
    }
}
