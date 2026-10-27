package com.kumo.controller;

import com.kumo.domain.PaymentSource;
import com.kumo.dto.RentExportResponse;
import com.kumo.dto.RentRecordRequest;
import com.kumo.dto.RentRecordResponse;
import com.kumo.security.SecurityUtils;
import com.kumo.service.RentRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rent-records")
@RequiredArgsConstructor
public class RentRecordController {

    private final RentRecordService rentRecordService;

    @GetMapping
    public ResponseEntity<Page<RentRecordResponse>> getRentRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) PaymentSource source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        Page<RentRecordResponse> records = rentRecordService.filterRentRecords(
                tenantEmail, startDate, endDate, propertyId, source, page, size
        );
        return ResponseEntity.ok(records);
    }

    @PostMapping("/manual")
    public ResponseEntity<RentRecordResponse> addManualRecord(@Valid @RequestBody RentRecordRequest request) {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        RentRecordResponse record = rentRecordService.addManualRecord(tenantEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @GetMapping("/export")
    public ResponseEntity<RentExportResponse> exportHistory() {
        String tenantEmail = SecurityUtils.getCurrentUserEmail();
        RentExportResponse export = rentRecordService.exportVerifiedRentHistory(tenantEmail);
        return ResponseEntity.ok(export);
    }
}
