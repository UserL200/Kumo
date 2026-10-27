package com.kumo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumo.dto.ApiErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory, per-IP rate limiter for spam-prone public endpoints.
 *
 * This is deliberately in-memory rather than Redis-backed: the app runs
 * as a single instance today, so a per-process bucket map is enough. If
 * this ever runs behind a load balancer across multiple instances, this
 * needs to move to a shared store (e.g. Redis) or each instance's limit
 * effectively multiplies by the instance count.
 *
 * KNOWN LIMITATION: bucketsByIp never evicts entries, so it grows by one
 * entry per distinct IP seen, forever. Fine at current traffic; if this
 * becomes a memory concern, swap the ConcurrentHashMap for a Caffeine
 * cache with expireAfterAccess so idle IPs get evicted automatically.
 *
 * Two bandwidths are combined on the same bucket: a tight burst limit
 * to stop rapid-fire submissions, and a longer-window cap to stop a
 * slow trickle of abuse from the same IP over a day.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PATH_PREFIX = "/api/v1/waitlist";

    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(PROTECTED_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // CORS preflight requests carry no body and aren't the actual
        // submission — don't burn rate-limit quota on them. The real
        // POST that follows still gets counted normally.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = bucketsByIp.computeIfAbsent(clientIp, ip -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeRateLimitResponse(response);
    }

    private Bucket newBucket() {
        // Burst limit: max 3 requests per 10 minutes.
        Bandwidth burst = Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(10)));
        // Daily cap: max 8 requests per 24 hours, from the same IP.
        Bandwidth daily = Bandwidth.classic(8, Refill.greedy(8, Duration.ofHours(24)));

        return Bucket.builder()
                .addLimit(burst)
                .addLimit(daily)
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // If this app sits behind a reverse proxy / load balancer (it
        // does, on Render), the ORIGINAL client IP is the first entry
        // a client could have set themselves — that's spoofable. Your
        // own trusted proxy appends the real client IP as the LAST
        // entry in the chain, so that's the one to trust. Taking the
        // first entry would let anyone bypass the limiter by sending
        // a different fake leading IP on every request.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .error("Too many requests. Please try again later.")
                .status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value())
                .timestamp(Instant.now().toString())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}