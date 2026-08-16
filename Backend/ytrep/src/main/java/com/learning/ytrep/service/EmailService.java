package com.learning.ytrep.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.ytrep.exception.APIException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name}")
    private String brevoSenderName;

    @Value("${app.mail.debug:false}")
    private boolean mailDebug;

    @Value("${app.verification.expiry-days}")
    private long expiryDays;

    public EmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public void sendVerificationCode(String toEmail, String code) {
        if (mailDebug) {
            log.info("[MAIL DEBUG] Verification code for {}: {}", toEmail, code);
            return;
        }

        String html = "<h2>Welcome to YouRep!</h2>"
                + "<p>Your verification code is:</p>"
                + "<h1 style=\"letter-spacing:6px;color:#065fd4;\">" + code + "</h1>"
                + "<p>This code expires in 10 minutes.</p>"
                + "<p><strong>You have " + expiryDays + " day" + (expiryDays == 1 ? "" : "s")
                + " to verify your email.</strong> If you don't verify in time, your account and its data will be deleted.</p>"
                + "<p>If you did not create a YouRep account, you can ignore this email.</p>";

        sendEmail(toEmail, "Your YouRep verification code", html);
    }

    public void sendReminderEmail(String toEmail) {
        if (mailDebug) {
            log.info("[MAIL DEBUG] Reminder for {}: verify your email within 1 day before your account is deleted", toEmail);
            return;
        }

        String html = "<h2>Don't lose your YouRep account!</h2>"
                + "<p>You signed up for YouRep but haven't verified your email yet.</p>"
                + "<p><strong>You have 1 day left.</strong> Verify your email now, otherwise your account and its data will be deleted.</p>"
                + "<p>Sign in and open the <strong>Verify Email</strong> page to enter your code, or resend the code from there.</p>"
                + "<p>If you did not create a YouRep account, you can ignore this email.</p>";

        sendEmail(toEmail, "Your YouRep account expires soon — verify your email", html);
    }

    private void sendEmail(String toEmail, String subject, String html) {
        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("name", brevoSenderName);
        sender.put("email", brevoSenderEmail);

        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("email", toEmail);
        List<Map<String, Object>> to = new ArrayList<>();
        to.add(recipient);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", sender);
        payload.put("to", to);
        payload.put("subject", subject);
        payload.put("htmlContent", html);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new APIException("Failed to build verification email");
        }

        Request request = new Request.Builder()
                .url(BREVO_URL)
                .addHeader("api-key", brevoApiKey)
                .addHeader("Accept", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                log.error("Brevo send failed: {} {}", response.code(), body);
                throw new APIException("Failed to send verification email");
            }
        } catch (IOException e) {
            log.error("Brevo request failed", e);
            throw new APIException("Failed to send verification email");
        }
    }
}
