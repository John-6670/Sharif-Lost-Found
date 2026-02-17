package com.nexus.nexus.Dto;

import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Enumaration.TypeOfReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {

    private String itemName;
    private String description;
    private TypeOfReport type;
    private Status status;
    private LocalDateTime reportedAt;
    private String categoryName;
    private Double locationLongitude;
    private Double locationLatitude;
}
