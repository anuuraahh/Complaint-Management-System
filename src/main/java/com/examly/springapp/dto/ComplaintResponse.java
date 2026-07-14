package com.examly.springapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplaintResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String status;
    private String complainantName;
    private String complainantEmail;
    private String assignedEmployeeName;
    private String assignedTo;
    private String resolutionComments;
    private String priority;
    private LocalDateTime submittedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime resolvedDate;
}