package com.pahappa.sacco.service;

import javax.enterprise.context.ApplicationScoped;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

@ApplicationScoped
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());

    private final String smtpHost = System.getenv().getOrDefault("SMTP_HOST", "smtp.gmail.com");
    private final String smtpPort = System.getenv().getOrDefault("SMTP_PORT", "587");
    private final String smtpUser = System.getenv().getOrDefault("SMTP_USERNAME", "samuelkala2003@gmail.com");
    private final String smtpPassword = System.getenv().getOrDefault("SMTP_PASSWORD", "eikx tqnw tzel wphh");
    private final String fromAddress = System.getenv().getOrDefault("SMTP_FROM", "samuelkala2003@gmail.com");
    private final String smtpTls = System.getenv().getOrDefault("SMTP_TLS", "true");
    private final String appBaseUrl = System.getenv().getOrDefault("APP_BASE_URL", "http://localhost:8080/kimwanyi-sacco");

    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isBlank() && fromAddress != null && !fromAddress.isBlank();
    }

    public void sendRegistrationCredentials(String to, String username, String temporaryPassword,
                                             String memberFullName, String membershipNumber) {
        if (to == null || to.isBlank()) {
            logger.warning("Skipping registration email: no member email address provided.");
            return;
        }
        if (!isConfigured()) {
            logger.warning("SMTP not configured; registration email not sent to " + to);
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", smtpUser != null && !smtpUser.isBlank() ? "true" : "false");
            props.put("mail.smtp.starttls.enable", smtpTls);
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);

            Session session = Session.getInstance(props, smtpUser != null && !smtpUser.isBlank() ?
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(smtpUser, smtpPassword);
                        }
                    } : null);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Kimwanyi SACCO — Your new login credentials");

            String body = "Dear " + memberFullName + ",\n\n"
                    + "Your new SACCO account has been created with membership number " + membershipNumber + ".\n"
                    + "Please use the following temporary credentials to log in for the first time:\n\n"
                    + "Username: " + username + "\n"
                    + "Temporary password: " + temporaryPassword + "\n\n"
                    + "Log in here: " + appBaseUrl + "/login.xhtml\n\n"
                    + "After logging in, change your username and password to your preferred values.\n"
                    + "Your username must be unique and must not resemble an existing username.\n\n"
                    + "Thank you,\nKimwanyi SACCO";
            message.setText(body);

            Transport.send(message);
            logger.info("Sent registration credentials email to " + to);
        } catch (Exception e) {
            logger.warning("Failed to send registration email to " + to + ": " + e.getMessage());
        }
    }
}
