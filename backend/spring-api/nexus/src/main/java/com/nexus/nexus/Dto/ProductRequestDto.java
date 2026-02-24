package com.nexus.nexus.Dto;

import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Enumaration.TypeOfReport;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {

    private Long id;
    private String name;
    private String description;
    @JsonProperty("notes")
    private String notes;
    private TypeOfReport type;
    private Status status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String image;
    private Long categoryId;
    @JsonAlias({"category", "category_name"})
    private String categoryName;

    private ReporterDto reporter;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
