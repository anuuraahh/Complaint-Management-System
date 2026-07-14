package com.examly.springapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ComplaintUpdateResponse {
    private Long id;
    private String comment;
    private String statusChangeFrom;
    private String statusChangeTo;
    private String visibility;
    private String updatedByName;
    private LocalDateTime timestamp;
}
