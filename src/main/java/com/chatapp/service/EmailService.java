package com.chatapp.service;

import com.chatapp.dto.StatisticsDTO;
import com.chatapp.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera il report delle statistiche come email HTML (template Thymeleaf
 * "templates/email/stats-report.html") e lo invia tramite JavaMailSender.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendStatisticsReport(User recipient, StatisticsDTO stats) {
        Context context = new Context();
        context.setVariable("fullName", recipient.getFullName());
        context.setVariable("stats", stats);
        context.setVariable("generatedAt",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' HH:mm")));

        String html = templateEngine.process("email/stats-report", context);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(recipient.getEmail());
            helper.setFrom(fromAddress);
            helper.setSubject("Your Chat App Statistics Report");
            helper.setText(html, true);
            mailSender.send(mimeMessage);
            log.info("Statistics report emailed to {}", recipient.getEmail());
        } catch (MessagingException e) {
            // Errori nella costruzione del messaggio stesso (allegati, encoding, ecc.).
            log.error("Failed to build statistics email for {}", recipient.getEmail(), e);
            throw new IllegalStateException("Could not send the statistics email. Check the SMTP configuration.", e);
        } catch (MailException e) {
            // mailSender.send(...) lancia MailException (non controllata, es. MailAuthenticationException
            // quando Gmail rifiuta le credenziali) invece di MessagingException - prima non veniva
            // catturata qui, quindi risaliva fino al client come stack trace grezzo invece di un
            // messaggio chiaro. La causa più comune con Gmail è usare la password normale
            // dell'account invece di una "Password per le app" di 16 caratteri.
            log.error("SMTP error while sending statistics email to {}", recipient.getEmail(), e);
            throw new IllegalStateException(
                    "Could not send the statistics email. If you're using Gmail, make sure MAIL_PASSWORD is an " +
                            "App Password (16 characters, generated at myaccount.google.com/apppasswords), not your normal Gmail password.",
                    e);
        }
    }
}
