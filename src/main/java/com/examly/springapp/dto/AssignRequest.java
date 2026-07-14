package com.examly.springapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;
}
