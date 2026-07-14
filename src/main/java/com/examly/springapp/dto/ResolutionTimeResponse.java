package com.examly.springapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResolutionTimeResponse {
    private String employeeName;
    private String employeeEmail;
    private long resolvedCount;
    private Double avgResolutionHours;
}
