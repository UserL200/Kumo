package com.kumo.controller;

import com.kumo.dto.WaitlistJoinRequest;
import com.kumo.dto.WaitlistJoinResponse;
import com.kumo.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/join")
    public ResponseEntity<WaitlistJoinResponse> join(@Valid @RequestBody WaitlistJoinRequest request) {
        WaitlistJoinResponse response = waitlistService.joinWaitlist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
