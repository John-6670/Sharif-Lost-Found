package com.nexus.nexus.Dto;

import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Enumaration.TypeOfReport;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class ProductResponseDto {

    private Long id;
    private String itemName;
    private String description;
    private TypeOfReport type;
    private Status status;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private String categoryName;
    private ApplicantDto applicant;
    private ApplicantDto resolver;
    private LocationDto location;

    /** Image returned as a Base64-encoded string; null when no image is stored. */
    private String imageBase64;
}
