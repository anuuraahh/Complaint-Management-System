package com.examly.springapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmissionTrendResponse {
    private String date;  // yyyy-MM-dd
    private long count;
}
