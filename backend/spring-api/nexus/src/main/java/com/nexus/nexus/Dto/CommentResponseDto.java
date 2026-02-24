package com.nexus.nexus.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a comment.
 * Top-level comments carry a populated {@code replies} list.
 * Reply objects always have an empty replies list (one level of nesting only).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {

    private Long id;
    private String text;
    private LocalDateTime createdAt;
    private ApplicantDto author;

    /** Id of the parent comment; null for top-level comments. */
    private Long parentCommentId;

    /** Populated only for top-level comments (criterion 4: visually nested). */
    private List<CommentResponseDto> replies;
}
