package com.nexus.nexus.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Builder
public class ProductResponseDto {

    private Long id;
    private String type;
    private String name;
    private String description;

    @JsonProperty("category")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    private String latitude;
    private String longitude;
    private String status;
    private String image;

    private ReporterDto reporter;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
