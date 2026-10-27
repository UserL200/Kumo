package com.kumo.service;

import com.kumo.domain.*;
import com.kumo.repository.PropertyRepository;
import com.kumo.repository.RentRecordRepository;
import com.kumo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RentRecordRepository rentRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Initializing sensible South African dev seed data for Kumo...");

            // Create primary demo tenant
            User tenant = User.builder()
                    .email("sipho.dlamini@kumo.co.za")
                    .password(passwordEncoder.encode("Password123!"))
                    .firstName("Sipho")
                    .lastName("Dlamini")
                    .phoneNumber("+27821234567")
                    .role(Role.TENANT)
                    .verificationStatus(VerificationStatus.VERIFIED)
                    .bankName("Capitec Bank")
                    .maskedAccountReference("•••• 8842")
                    .encryptedProviderToken("ENC(demo_capitec_feed_token_9981)")
                    .bankLinkedAt(LocalDateTime.now().minusMonths(6))
                    .build();

            tenant = userRepository.save(tenant);

            // Create Properties
            Property flat = Property.builder()
                    .tenant(tenant)
                    .address("Unit 402, Rosebank Terrace, 17 Oxford Rd, Rosebank, Johannesburg")
                    .propertyType(PropertyType.FLAT_BLOCK)
                    .landlordName("Prestige Property Management")
                    .landlordContact("rentals@prestige.co.za")
                    .monthlyRent(BigDecimal.valueOf(8500.00))
                    .tenancyStartDate(LocalDate.now().minusMonths(12))
                    .active(true)
                    .build();

            flat = propertyRepository.save(flat);

            Property backyardRoom = Property.builder()
                    .tenant(tenant)
                    .address("Stand 1408, Zone 6, Diepkloof, Soweto")
                    .propertyType(PropertyType.BACKYARD_DWELLING)
                    .landlordName("Mrs. Khumalo")
                    .landlordContact("+27719876543")
                    .monthlyRent(BigDecimal.valueOf(2800.00))
                    .tenancyStartDate(LocalDate.now().minusMonths(24))
                    .active(false)
                    .build();

            propertyRepository.save(backyardRoom);

            // Create Verified Rent Records for the Rosebank Flat
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 6; i++) {
                LocalDate payDate = today.minusMonths(i).withDayOfMonth(1);
                RentRecord record = RentRecord.builder()
                        .tenant(tenant)
                        .property(flat)
                        .amount(BigDecimal.valueOf(8500.00))
                        .paymentDate(payDate)
                        .source(i % 5 == 0 ? PaymentSource.MANUALLY_LOGGED : PaymentSource.AUTO_DETECTED)
                        .status(PaymentStatus.CONFIRMED)
                        .referenceNumber("EFT-RENT-" + payDate.getYear() + "-" + payDate.getMonthValue())
                        .transactionNotes(i % 5 == 0 ? "EFT receipt uploaded manually" : "Auto-detected recurring payment from Capitec account")
                        .build();

                rentRecordRepository.save(record);
            }

            log.info("Dev seed data created successfully! Demo login: sipho.dlamini@kumo.co.za / Password123!");
        }
    }
}
