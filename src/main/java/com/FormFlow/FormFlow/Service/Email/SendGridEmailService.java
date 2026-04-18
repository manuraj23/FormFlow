package com.FormFlow.FormFlow.Service.Email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    private final String sendGridApiKey;
    private final String fromEmail;

    public SendGridEmailService(
            @Value("${sendgrid.api-key}") String sendGridApiKey,
            @Value("${sendgrid.from-email}") String fromEmail
    ) {
        this.sendGridApiKey = sendGridApiKey;
        this.fromEmail = fromEmail;
    }

    public void sendMail() {
        String subject = "Mail test - FormFlow 1";
        String messageBody = "Email test runs fine 1";

        Email from = new Email(fromEmail);
        Email to = new Email("manuraj0642@gmail.com");
        Content content = new Content("text/plain", messageBody);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sendGrid = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            int statusCode = response.getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException(
                        "SendGrid mail request failed with status " + statusCode + ": " + response.getBody()
                );
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send email through SendGrid", ex);
        }
    }
}
