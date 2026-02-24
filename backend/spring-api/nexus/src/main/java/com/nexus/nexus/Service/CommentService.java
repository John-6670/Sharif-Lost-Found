package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Security.JwtPrincipal;

import java.util.List;

public interface CommentService {

    /** Returns all top-level comments for an item, each with their replies nested inside. */
    List<CommentResponseDto> getCommentsForItem(Long itemId);

    /** Creates a top-level comment (parentCommentId == null) or a reply. */
    CommentResponseDto addComment(Long itemId, CommentRequestDto request, JwtPrincipal principal);
}
