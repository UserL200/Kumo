package com.kumo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kumo.mail.from:hello@kumohost.co.za}")
    private String fromAddress;

    // Reuses the same Resend API key you already set as MAIL_PASSWORD
    // (spring.mail.password), so no new Railway variable is needed.
    @Value("${spring.mail.password}")
    private String resendApiKey;

    @Override
    public boolean sendWaitlistConfirmation(String toEmail) {
        try {
            Map<String, Object> payload = Map.of(
                    "from", fromAddress,
                    "to", new String[]{toEmail},
                    "subject", "You're on the Kumo waiting list",
                    "html", buildBody()
            );

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }

            log.error("Resend API returned non-success status {} for {}: {}",
                    response.statusCode(), toEmail, response.body());
            return false;

        } catch (Exception ex) {
            // Don't let a mail-provider hiccup fail the signup itself —
            // the entry is already saved. Log loudly so this is visible
            // for monitoring/retry, but let the request succeed.
            log.error("Failed to send waitlist confirmation email to {}", toEmail, ex);
            return false;
        }
    }

    private String buildBody() {
    return """
            <div style="font-family: Arial, Helvetica, sans-serif; max-width: 480px; margin: 0 auto; background-color: #f4f7f4; padding: 32px 24px; border-radius: 12px;">

              <div style="text-align: center; margin-bottom: 20px;">
                <span style="display: inline-flex; align-items: center; gap: 8px; background-color: #1a1a1a; padding: 6px 18px 6px 6px; border-radius: 999px;">
                    <img src="https://kumohost.co.za/kumo-logo-white.png" alt="" width="20" height="20" style="display: block;" />
                    <span style="color: #ffffff; font-size: 14px; font-weight: 700; letter-spacing: 1.5px;">KUMO</span>
                </span>
                </div>

              <div style="background-color: #ffffff; border-radius: 16px; padding: 32px 24px; text-align: center;">

                <img src="https://kumohost.co.za/kumo-mascot.png" alt="Kumo" width="120" height="120" style="display: block; margin: 0 auto 12px;" />

                <h2 style="margin: 4px 0 14px; color: #1a1a1a; font-size: 20px; font-weight: 700;">You're on the waitlist!</h2>

                <p style="color: #333333; font-size: 14px; line-height: 1.7; margin: 0 0 20px;">
                  Thanks for joining kumohost.co.za! We're finalising our bank-verification
                  partnerships and will email you the moment you can link your account and
                  start building your rent payment record.
                </p>

                <p style="color: #666666; font-size: 12px; font-weight: 500; margin: 0 0 12px;">Follow us</p>

                <div>
                  <a href="#" style="display: inline-block; width: 34px; height: 34px; margin: 0 5px; border-radius: 50%; background-color: #f4f4f4; text-align: center; line-height: 34px; text-decoration: none;">
                    <img src="https://cdn.jsdelivr.net/npm/simple-icons@v11/icons/x.svg" width="16" height="16" alt="X" style="vertical-align: middle;" />
                  </a>
                  <a href="#" style="display: inline-block; width: 34px; height: 34px; margin: 0 5px; border-radius: 50%; background-color: #f4f4f4; text-align: center; line-height: 34px; text-decoration: none;">
                    <img src="https://cdn.jsdelivr.net/npm/simple-icons@v11/icons/instagram.svg" width="16" height="16" alt="Instagram" style="vertical-align: middle;" />
                  </a>
                  <a href="#" style="display: inline-block; width: 34px; height: 34px; margin: 0 5px; border-radius: 50%; background-color: #f4f4f4; text-align: center; line-height: 34px; text-decoration: none;">
                    <img src="https://cdn.jsdelivr.net/npm/simple-icons@v11/icons/linkedin.svg" width="16" height="16" alt="LinkedIn" style="vertical-align: middle;" />
                  </a>
                  <a href="#" style="display: inline-block; width: 34px; height: 34px; margin: 0 5px; border-radius: 50%; background-color: #f4f4f4; text-align: center; line-height: 34px; text-decoration: none;">
                    <img src="https://cdn.jsdelivr.net/npm/simple-icons@v11/icons/facebook.svg" width="16" height="16" alt="Facebook" style="vertical-align: middle;" />
                  </a>
                </div>
              </div>

              <p style="text-align: center; color: #999999; font-size: 11px; margin-top: 20px;">
                You're receiving this email because you signed up on<br />
                <span style="color: #1a1a1a;">kumohost.co.za</span>
              </p>

            </div>
            """;
    }   
}