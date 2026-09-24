package com.crm.platform.delivery.provider;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SmtpDeliveryProvider implements DeliveryProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpDeliveryProvider.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final int timeoutMs;
    private final JavaMailSender mailSender;

    private final ConcurrentMap<String, DeliveryResult> sentCache = new ConcurrentHashMap<>();

    @Autowired
    public SmtpDeliveryProvider(
            @Value("${smtp.host:localhost}") String host,
            @Value("${smtp.port:1025}") int port,
            @Value("${smtp.username:}") String username,
            @Value("${smtp.password:}") String password,
            @Value("${smtp.from:noreply@crm.internal}") String from,
            @Value("${smtp.timeout-ms:5000}") int timeoutMs,
            @Autowired(required = false) JavaMailSender mailSender) {
        this.host = host != null ? host.trim() : "localhost";
        this.port = port > 0 ? port : 1025;
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
        this.from = (from != null && !from.isBlank()) ? from.trim() : "noreply@crm.internal";
        this.timeoutMs = Math.max(500, Math.min(60000, timeoutMs));

        if (mailSender != null) {
            this.mailSender = mailSender;
        } else {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(this.host);
            sender.setPort(this.port);
            if (!this.username.isEmpty()) {
                sender.setUsername(this.username);
                sender.setPassword(this.password);
            }
            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", !this.username.isEmpty() ? "true" : "false");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.connectiontimeout", String.valueOf(this.timeoutMs));
            props.put("mail.smtp.timeout", String.valueOf(this.timeoutMs));
            props.put("mail.smtp.writetimeout", String.valueOf(this.timeoutMs));
            this.mailSender = sender;
        }
    }

    public SmtpDeliveryProvider(
            String host,
            int port,
            String username,
            String password,
            String from,
            int timeoutMs) {
        this(host, port, username, password, from, timeoutMs, null);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getFrom() {
        return from;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String key = request.getIdempotencyKey();
        if (key == null || key.isBlank()) {
            return doSend(request);
        }

        DeliveryResult cached = sentCache.get(key);
        if (cached != null) {
            log.debug("SMTP delivery idempotent cache hit for key={}", key);
            return cached;
        }

        synchronized (key.intern()) {
            cached = sentCache.get(key);
            if (cached != null) {
                log.debug("SMTP delivery idempotent cache hit for key={}", key);
                return cached;
            }

            DeliveryResult result = doSend(request);
            if (result != null && result.isSuccess()) {
                sentCache.put(key, result);
            }
            return result;
        }
    }

    private DeliveryResult doSend(DeliveryRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(this.from);
            helper.setTo(request.getRecipientEmail());
            helper.setSubject("Campaign Notification #" + request.getCampaignId());
            helper.setText(request.getMessageText() != null ? request.getMessageText() : "", false);

            if (request.getIdempotencyKey() != null) {
                mimeMessage.addHeader("X-Delivery-Idempotency-Key", request.getIdempotencyKey());
            }
            if (request.getCampaignId() != null) {
                mimeMessage.addHeader("X-Campaign-Id", String.valueOf(request.getCampaignId()));
            }
            if (request.getCustomerId() != null) {
                mimeMessage.addHeader("X-Customer-Id", String.valueOf(request.getCustomerId()));
            }

            mailSender.send(mimeMessage);

            String messageId = "SMTP-" + (request.getIdempotencyKey() != null ? request.getIdempotencyKey() : UUID.randomUUID().toString());
            DeliveryResult result = DeliveryResult.success(messageId);
            log.info("Email delivered via SMTP host={}:{} to recipient={} messageId={}",
                    host, port, request.getRecipientEmail(), messageId);
            return result;
        } catch (Exception e) {
            String errorMsg = "SMTP_DISPATCH_FAILURE: " + e.getMessage();
            log.error("Failed to deliver message via SMTP to recipient={}: {}", request.getRecipientEmail(), e.getMessage());
            return DeliveryResult.failure(errorMsg);
        }
    }

    public boolean hasProcessed(String idempotencyKey) {
        return sentCache.containsKey(idempotencyKey);
    }

    public void clearCache() {
        sentCache.clear();
    }
}
