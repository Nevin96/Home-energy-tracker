package com.nev.alert_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;



@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void sendEmail(String to,
                          String subject,
                          String body,
                          Long option) {
        log.info("Sending email to: {},subject: {}",to,subject);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("noreply@nev.com");
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException e){
            log.error("failed to sent email to: {}",to,e);
        }
    }
}
