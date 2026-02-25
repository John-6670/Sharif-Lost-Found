package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Security.JwtPrincipal;


public interface CommentService {

    /** Returns paged top-level comments for an item, each with their replies nested inside. */
    CommentPage getCommentsForItem(Long itemId, int page, int size);

    /** Creates a top-level comment (parentCommentId == null) or a reply. */
    CommentResponseDto addComment(Long itemId, CommentRequestDto request, JwtPrincipal principal);

    /** Reports a comment; deletes it after 3 distinct reports. */
    void reportComment(Long itemId, Long commentId, String cause, JwtPrincipal principal);
}
