package com.examly.springapp.service;

import com.examly.springapp.dto.ResolutionTimeResponse;
import com.examly.springapp.dto.StatusBreakdownResponse;
import com.examly.springapp.dto.SubmissionTrendResponse;
import com.examly.springapp.model.ComplaintStatus;
import com.examly.springapp.model.Role;
import com.examly.springapp.repository.ComplaintRepository;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    // FR8.1: Count of complaints per status
    public List<StatusBreakdownResponse> getStatusBreakdown() {
        return Arrays.stream(ComplaintStatus.values())
                .map(status -> new StatusBreakdownResponse(
                        status.name(),
                        complaintRepository.findByStatus(status).size()))
                .toList();
    }

    // FR8.2: Per-employee resolution stats
    public List<ResolutionTimeResponse> getResolutionTime() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .map(employee -> {
                    var resolved = complaintRepository
                            .findByAssignedEmployeeId(employee.getId())
                            .stream()
                            .filter(c -> c.getResolvedAt() != null && c.getSubmittedAt() != null)
                            .toList();

                    double avgHours = resolved.isEmpty() ? 0.0 :
                            resolved.stream()
                                    .mapToLong(c -> Duration.between(
                                            c.getSubmittedAt(), c.getResolvedAt()).toHours())
                                    .average()
                                    .orElse(0.0);

                    return new ResolutionTimeResponse(
                            employee.getName(),
                            employee.getEmail(),
                            resolved.size(),
                            avgHours);
                })
                .toList();
    }

    // FR8.3: Complaint submission trends grouped by date
    public List<SubmissionTrendResponse> getSubmissionTrends() {
        return complaintRepository.countBySubmissionDate().stream()
                .map(row -> new SubmissionTrendResponse(
                        row.get("date").toString(),
                        ((Number) row.get("count")).longValue()))
                .toList();
    }

    // FR8.4: Export all complaints as CSV
    public String exportComplaintsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("id,title,category,status,priority,complainant,assignedEmployee,submittedAt,resolvedAt\n");
        complaintRepository.findAll().forEach(c -> {
            csv.append(c.getId()).append(",")
               .append(escapeCsv(c.getTitle())).append(",")
               .append(escapeCsv(c.getCategory())).append(",")
               .append(c.getStatus().name()).append(",")
               .append(c.getPriority() != null ? c.getPriority().name() : "").append(",")
               .append(escapeCsv(c.getComplainant().getName())).append(",")
               .append(c.getAssignedEmployee() != null ? escapeCsv(c.getAssignedEmployee().getName()) : "").append(",")
               .append(c.getSubmittedAt() != null ? c.getSubmittedAt().toString() : "").append(",")
               .append(c.getResolvedAt() != null ? c.getResolvedAt().toString() : "").append("\n");
        });
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}