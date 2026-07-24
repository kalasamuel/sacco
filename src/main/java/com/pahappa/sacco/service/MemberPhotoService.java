package com.pahappa.sacco.service;

import com.pahappa.sacco.dao.MemberDao;
import com.pahappa.sacco.entity.Member;
import com.pahappa.sacco.entity.Role;
import com.pahappa.sacco.entity.User;
import com.pahappa.sacco.exception.BusinessRuleViolationException;
import com.pahappa.sacco.security.RoleGuard;
import com.pahappa.sacco.util.TransactionUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class MemberPhotoService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;

    private final MemberDao memberDao;

    @Inject
    public MemberPhotoService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    protected MemberPhotoService() {
        this.memberDao = null;
    }

    public void uploadPhoto(Long memberId, InputStream fileContent, String originalFilename,
                             long fileSizeBytes, User performedBy) {
        RoleGuard.require(performedBy, Role.ADMIN, Role.CASHIER);

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessRuleViolationException(
                    "Only JPG and PNG photos are allowed.");
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleViolationException("Photo must be under 2MB.");
        }

        String storedFilename = UUID.randomUUID() + "." + extension.toLowerCase();

        try {
            Path targetDir = uploadDirectory();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFilename);
            Files.copy(fileContent, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Failed to save photo: " + e.getMessage());
        }

        TransactionUtil.executeVoid(em -> {
            Member member = memberDao.findById(memberId)
                    .orElseThrow(() -> new BusinessRuleViolationException("Member not found."));
            member.setPhotoPath(storedFilename);
        });
    }

    public Path resolvePhotoFile(String storedFilename) {
        return uploadDirectory().resolve(storedFilename);
    }

    private Path uploadDirectory() {
        String catalinaBase = System.getProperty("catalina.base");
        String root = (catalinaBase != null) ? catalinaBase : System.getProperty("java.io.tmpdir");
        return Paths.get(root, "sacco-uploads", "members");
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
