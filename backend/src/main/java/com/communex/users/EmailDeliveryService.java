
package com.communex.users;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {

    private final ObjectMapper objectMapper;
    private final String relayUrl;
    private final String relaySecret;
    private final HttpClient httpClient;

    public EmailDeliveryService(
            ObjectMapper objectMapper,
            @Value("${communex.email-relay.url:}") String relayUrl,
            @Value("${communex.email-relay.secret:}") String relaySecret
    ) {
        this.objectMapper = objectMapper;
        this.relayUrl = relayUrl == null ? "" : relayUrl.trim();
        this.relaySecret = relaySecret == null ? "" : relaySecret.trim();

        // Handle Google's ContentService redirect explicitly below.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public boolean isConfigured() {
        return !relayUrl.isBlank() && !relaySecret.isBlank();
    }

    public boolean send(String to, String subject, String text) {
        if (!isConfigured()) {
            return false;
        }

        URI uri = URI.create(relayUrl);

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"script.google.com".equalsIgnoreCase(uri.getHost())
                || !uri.getPath().startsWith("/macros/s/")
                || !uri.getPath().endsWith("/exec")) {
            throw new IllegalStateException(
                    "Email relay URL must be a deployed Apps Script /exec URL"
            );
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "secret", relaySecret,
                    "to", to,
                    "subject", subject,
                    "text", text
            ));

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int redirects = 0;

            while (response.statusCode() >= 300
                    && response.statusCode() < 400) {

                if (redirects++ >= 5) {
                    throw new IllegalStateException(
                            "Too many email relay redirects"
                    );
                }

                String location = response.headers()
                        .firstValue("Location")
                        .orElseThrow(() -> new IllegalStateException(
                                "Email relay redirect has no Location header"
                        ));

                URI nextUri = response.uri().resolve(location);

                if (!"https".equalsIgnoreCase(nextUri.getScheme())
                        || !"script.googleusercontent.com"
                                .equalsIgnoreCase(nextUri.getHost())) {
                    throw new IllegalStateException(
                            "Unexpected email relay redirect destination"
                    );
                }

                HttpRequest followUp = HttpRequest.newBuilder(nextUri)
                        .timeout(Duration.ofSeconds(25))
                        .GET()
                        .build();

                response = httpClient.send(
                        followUp,
                        HttpResponse.BodyHandlers.ofString()
                );
            }

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Email relay returned HTTP " + response.statusCode()
                );
            }

            Map<String, Object> result = objectMapper.readValue(
                    response.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            if (!Boolean.TRUE.equals(result.get("ok"))) {
                throw new IllegalStateException(
                        "Email relay did not confirm sending the message"
                );
            }

            return true;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Email relay request was interrupted", ex
            );
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Email relay request failed", ex
            );
        }
    }
}
