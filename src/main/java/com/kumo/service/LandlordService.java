package com.kumo.service;

import com.kumo.dto.InviteResponse;
import com.kumo.domain.Property;
import com.kumo.domain.Role;
import com.kumo.domain.TenantInvite;
import com.kumo.domain.User;
import com.kumo.dto.PropertyRequest;
import com.kumo.dto.PropertyResponse;
import com.kumo.exception.ResourceNotFoundException;
import com.kumo.exception.UnauthorizedException;
import com.kumo.exception.ValidationException;
import com.kumo.repository.PropertyRepository;
import com.kumo.repository.TenantInviteRepository;
import com.kumo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LandlordService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final TenantInviteRepository inviteRepository;
    private final PropertyService propertyService;

    @Transactional
    public PropertyResponse createProperty(String landlordEmail, PropertyRequest request) {
        User landlord = requireLandlord(landlordEmail);

        Property property = Property.builder()
                .landlord(landlord)
                .address(request.getAddress().trim())
                .propertyType(request.getPropertyType())
                .monthlyRent(request.getMonthlyRent())
                .tenancyStartDate(request.getTenancyStartDate())
                .landlordName(landlord.getFirstName() + " " + landlord.getLastName())
                .landlordContact(landlord.getEmail())
                .active(true)
                .build();

        return propertyService.mapToResponse(propertyRepository.save(property));
    }

    public List<PropertyResponse> getMyProperties(String landlordEmail) {
        User landlord = requireLandlord(landlordEmail);
        return propertyRepository.findById(landlord.getId()).stream()
                .map(propertyService::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InviteResponse inviteTenant(String landlordEmail, Long propertyId, String tenantEmail) {
        User landlord = requireLandlord(landlordEmail);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getLandlord() == null || !property.getLandlord().getId().equals(landlord.getId())) {
            throw new UnauthorizedException("This property does not belong to you");
        }
        if (property.getTenant() != null) {
            throw new ValidationException("This property already has a tenant");
        }

        TenantInvite invite = TenantInvite.builder()
                .token(newToken())
                .property(property)
                .email(tenantEmail.toLowerCase().trim())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        inviteRepository.save(invite);

        return new InviteResponse(invite.getToken(), invite.getEmail(), invite.getExpiresAt());
    }

    @Transactional
    public PropertyResponse acceptInvite(String tenantEmail, String token) {
        User tenant = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        TenantInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.getAcceptedAt() != null) {
            throw new ValidationException("This invite has already been used");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("This invite has expired");
        }
        if (!invite.getEmail().equalsIgnoreCase(tenant.getEmail())) {
            throw new UnauthorizedException("This invite was sent to a different email address");
        }

        Property property = invite.getProperty();
        if (property.getTenant() != null) {
            throw new ValidationException("This property already has a tenant");
        }

        property.setTenant(tenant);
        if (property.getTenancyStartDate() == null) {
            property.setTenancyStartDate(LocalDate.now());
        }
        invite.setAcceptedAt(LocalDateTime.now());

        return propertyService.mapToResponse(propertyRepository.save(property));
    }

    private User requireLandlord(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found"));
        if (user.getRole() != Role.LANDLORD) {
            throw new UnauthorizedException("Only landlords can do this");
        }
        return user;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}