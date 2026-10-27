package com.kumo.service;

import com.kumo.domain.WaitlistEntry;
import com.kumo.dto.WaitlistJoinRequest;
import com.kumo.dto.WaitlistJoinResponse;
import com.kumo.exception.ValidationException;
import com.kumo.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService {

    // South African mobile numbers: 0821234567, +27821234567, or
    // 27821234567 — first digit after the leading 0 / country code
    // must be 6, 7, or 8 (the SA mobile prefixes), then 8 more digits.
    private static final Pattern SA_PHONE_PATTERN = Pattern.compile("^(?:\\+27|27|0)[678]\\d{8}$");

    private static final String GENERIC_SUCCESS_MESSAGE =
            "You're on the waiting list. Check your inbox for a confirmation email — "
            + "we'll let you know as soon as bank verification is ready.";

    private final WaitlistRepository waitlistRepository;
    private final EmailService emailService;

    @Transactional
    public WaitlistJoinResponse joinWaitlist(WaitlistJoinRequest request) {
        // Honeypot check: a real user never sees or fills this field.
        // Respond with the normal-looking success message but do no
        // actual work, so a bot doesn't learn it was caught.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            log.warn("Waitlist honeypot triggered for email {}", request.getEmail());
            return WaitlistJoinResponse.builder().message(GENERIC_SUCCESS_MESSAGE).build();
        }

        String email = request.getEmail().toLowerCase().trim();
        String phone = normalizePhone(request.getPhoneNumber());

        if (!SA_PHONE_PATTERN.matcher(phone).matches()) {
            throw new ValidationException("Enter a valid South African mobile number");
        }

        // If this email is already on the list, respond with the exact
        // same message as a brand-new signup rather than a distinct
        // "already registered" message. Two reasons: (1) it stops the
        // endpoint being used to check whether a given email exists in
        // our system, and (2) it stops someone repeatedly submitting a
        // stranger's email from re-triggering a confirmation email to
        // that stranger every time — a harassment vector, and a risk
        // to our sender reputation if it gets marked as spam enough.
        if (waitlistRepository.existsByEmail(email)) {
            log.info("Waitlist signup for already-registered email; no email resent");
            return WaitlistJoinResponse.builder().message(GENERIC_SUCCESS_MESSAGE).build();
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .email(email)
                .phoneNumber(phone)
                .build();

        // existsByEmail() above and save() below are two separate
        // steps, not one atomic operation — two near-simultaneous
        // submissions of the same email can both pass the check
        // before either commits. The unique constraint on `email`
        // catches that at the DB level; if it fires, treat it exactly
        // like the existsByEmail case above rather than letting the
        // exception reach the client (that would leak whether the
        // email exists, and undercut the generic-message design).
        try {
            entry = waitlistRepository.save(entry);
        } catch (DataIntegrityViolationException e) {
            log.info("Race on duplicate email during waitlist signup for {}", email);
            return WaitlistJoinResponse.builder().message(GENERIC_SUCCESS_MESSAGE).build();
        }

        // sendWaitlistConfirmation failing by *throwing* (SMTP timeout,
        // SendGrid auth error, etc.) rather than returning false would
        // otherwise propagate out of this @Transactional method and
        // roll back the entry we just saved — silently contradicting
        // the intent that a failed email still leaves the person on
        // the list. Catch broadly here so any failure mode degrades to
        // "saved, not yet confirmed" rather than "not saved at all".
        boolean sent;
        try {
            sent = emailService.sendWaitlistConfirmation(email);
        } catch (Exception e) {
            log.error("Confirmation email send failed for {}", email, e);
            sent = false;
        }

        if (sent) {
            entry.setConfirmationEmailSentAt(LocalDateTime.now());
            waitlistRepository.save(entry);
        }
        // If the email failed to send, the entry is still saved — the
        // person is genuinely on the list, just without confirmation
        // yet. confirmationEmailSentAt staying null marks it for retry.

        return WaitlistJoinResponse.builder().message(GENERIC_SUCCESS_MESSAGE).build();
    }

    private String normalizePhone(String rawPhone) {
        return rawPhone.replaceAll("[\\s-]", "");
    }
}