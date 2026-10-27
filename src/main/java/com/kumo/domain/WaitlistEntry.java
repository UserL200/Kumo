package com.kumo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    // Set once the confirmation ("you're on the list") email is sent.
    // Left null if the email send failed, so it can be retried later
    // without re-inserting a duplicate row.
    private LocalDateTime confirmationEmailSentAt;

    // Set later, by an admin/batch process, when this person is
    // emailed to say bank verification is ready and they can proceed.
    private LocalDateTime invitedAt;
}