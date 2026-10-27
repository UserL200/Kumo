package com.kumo.service;

import com.kumo.domain.Role;
import com.kumo.domain.User;
import com.kumo.domain.VerificationStatus;
import com.kumo.dto.*;
import com.kumo.exception.UnauthorizedException;
import com.kumo.exception.ValidationException;
import com.kumo.repository.UserRepository;
import com.kumo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ValidationException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role(request.getRole() == Role.LANDLORD ? Role.LANDLORD : Role.TENANT)
                .verificationStatus(VerificationStatus.NOT_STARTED)
                .build();

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            String email = jwtService.extractUsername(refreshToken);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));

            if (!jwtService.isTokenValid(refreshToken, user.getEmail())) {
                throw new UnauthorizedException("Refresh token is expired or invalid");
            }

            return buildAuthResponse(user);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token: " + e.getMessage());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getId());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .verificationStatus(user.getVerificationStatus())
                .maskedAccountReference(user.getMaskedAccountReference())
                .bankName(user.getBankName())
                .bankLinkedAt(user.getBankLinkedAt())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getJwtExpirationMs())
                .user(userDto)
                .build();
    }
}
