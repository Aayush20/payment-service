package org.example.paymentservice.services;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    @Value("${sendgrid.api-key}")
    private String sendgridApiKey;

    @Value("${sendgrid.sender-email}")
    private String senderEmail;

    @Value("${sendgrid.sender-name}")
    private String senderName;

    public void sendEmail(String to, String subject, String content) throws IOException {
        Email from = new Email(senderEmail, senderName);
        Email toEmail = new Email(to);
        Content emailContent = new Content("text/plain", content);
        Mail mail = new Mail(from, subject, toEmail, emailContent);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                throw new RuntimeException("Failed to send email: " + response.getBody());
            }
            logger.info("📧 Payment email sent to {} | Status: {}", to, response.getStatusCode());

        } catch (IOException e) {
            logger.error("❌ Email sending failed for {}: {}", to, e.getMessage());
            throw new IOException("Failed to send email", e);
        }
    }
}
