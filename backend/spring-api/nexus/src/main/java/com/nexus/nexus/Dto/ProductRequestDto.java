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
public class ProductRequestDto {

    private String productName;
    private String description;
    private TypeOfReport typeOfReport;
    private Status status;
    private LocalDateTime lostOrFoundTime;
    private String deliveredTo;
    private Double locationX;
    private Double locationY;
}
