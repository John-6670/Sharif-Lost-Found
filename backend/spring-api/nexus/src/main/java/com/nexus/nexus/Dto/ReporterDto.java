package com.nexus.nexus.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporterDto {

    private Long id;
    private String email;
    private String name;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("is_verified")
    private Boolean isVerified;
}
