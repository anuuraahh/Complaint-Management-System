package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintUpdateRequest {

    @NotBlank(message = "Comment is required")
    private String comment;

    private String visibility; // PUBLIC or INTERNAL, defaults to PUBLIC
}
