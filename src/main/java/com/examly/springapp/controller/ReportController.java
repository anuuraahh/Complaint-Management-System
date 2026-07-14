package com.examly.springapp.controller;

import com.examly.springapp.dto.ResolutionTimeResponse;
import com.examly.springapp.dto.StatusBreakdownResponse;
import com.examly.springapp.dto.SubmissionTrendResponse;
import com.examly.springapp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // FR8.1: Complaint count by status
    @GetMapping("/status-breakdown")
    public ResponseEntity<List<StatusBreakdownResponse>> getStatusBreakdown() {
        return ResponseEntity.ok(reportService.getStatusBreakdown());
    }

    // FR8.2: Per-employee resolution time
    @GetMapping("/resolution-time")
    public ResponseEntity<List<ResolutionTimeResponse>> getResolutionTime() {
        return ResponseEntity.ok(reportService.getResolutionTime());
    }

    // FR8.3: Complaint submission trends by date
    @GetMapping("/submission-trends")
    public ResponseEntity<List<SubmissionTrendResponse>> getSubmissionTrends() {
        return ResponseEntity.ok(reportService.getSubmissionTrends());
    }

    // FR8.4: Export all complaints as CSV
    @GetMapping("/export/complaints.csv")
    public ResponseEntity<String> exportComplaintsCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"complaints.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportService.exportComplaintsCsv());
    }
}
