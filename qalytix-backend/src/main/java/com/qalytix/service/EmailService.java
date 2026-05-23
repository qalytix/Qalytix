package com.qalytix.service;

public interface EmailService {

    void sendInvitationEmail(String toEmail, String orgName, String inviterName, String acceptUrl);
}
