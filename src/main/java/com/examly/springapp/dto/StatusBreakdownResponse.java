package com.examly.springapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusBreakdownResponse {
    private String status;
    private long count;
}
