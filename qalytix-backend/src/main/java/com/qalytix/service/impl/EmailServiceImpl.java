package com.qalytix.service.impl;

import com.qalytix.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendInvitationEmail(String toEmail, String orgName, String inviterName, String acceptUrl) {
        // TODO: integrate SendGrid — wire SENDGRID_API_KEY env var
        log.info("INVITATION EMAIL → to={} org='{}' inviter='{}' link={}",
                toEmail, orgName, inviterName, acceptUrl);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        // TODO: integrate SendGrid — wire SENDGRID_API_KEY env var
        log.info("PASSWORD RESET EMAIL → to={} link={}", toEmail, resetUrl);
    }
}
