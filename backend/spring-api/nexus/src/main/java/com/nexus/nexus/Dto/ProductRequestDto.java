package com.nexus.nexus.Dto;

import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Enumaration.TypeOfReport;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {

    private String name;
    private String description;
    private TypeOfReport type;
    private Status status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String image;
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;
}
