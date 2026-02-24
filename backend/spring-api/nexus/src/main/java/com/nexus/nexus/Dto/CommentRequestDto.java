package com.nexus.nexus.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for both creating a top-level comment and replying.
 * For a reply, parentCommentId must be set.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {

    private String text;

    /** Null for a new top-level comment; parent comment id for a reply. */
    private Long parentCommentId;
}
