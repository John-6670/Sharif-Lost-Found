package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Entity.Comment;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.CommentMapper;
import com.nexus.nexus.Repository.CommentRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsForItem(Long itemId) {
        // Verify item exists and is not removed
        Item item = reportRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (item.getIsRemoved()) {
            throw new IllegalArgumentException("Item has been removed");
        }

        // Single query — fetch all comments for this item ordered by creation time
        List<Comment> all = commentRepository.findByItemIdOrderByCreatedAtAsc(itemId);

        // Build id -> dto map (preserving insertion order = chronological)
        Map<Long, CommentResponseDto> dtoMap = new LinkedHashMap<>();
        for (Comment c : all) {
            CommentResponseDto dto = commentMapper.toDto(c);
            dto.setReplies(new ArrayList<>());
            dtoMap.put(c.getId(), dto);
        }

        // Wire up the tree: replies attach to their parent, roots collected separately
        List<CommentResponseDto> roots = new ArrayList<>();
        for (Comment c : all) {
            CommentResponseDto dto = dtoMap.get(c.getId());
            if (c.getParent() == null) {
                roots.add(dto);
            } else {
                CommentResponseDto parentDto = dtoMap.get(c.getParent().getId());
                if (parentDto != null) {
                    parentDto.getReplies().add(dto);
                } else {
                    roots.add(dto); // safety fallback for orphaned comments
                }
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public CommentResponseDto addComment(Long itemId, CommentRequestDto request, JwtPrincipal principal) {
        validatePrincipal(principal);

        // Criterion 6: reject empty/blank text
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }

        Item item = reportRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (item.getIsRemoved()) {
            throw new IllegalArgumentException("Cannot comment on a removed item");
        }

        User author = resolveAuthor(principal);

        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            // Parent must belong to the same item
            if (!parent.getItem().getId().equals(itemId)) {
                throw new IllegalArgumentException("Parent comment does not belong to this item");
            }
        }

        Comment comment = Comment.builder()
                .text(request.getText().trim())
                .createdAt(LocalDateTime.now())
                .item(item)
                .author(author)
                .parent(parent)
                .build();

        comment = commentRepository.save(comment);

        CommentResponseDto dto = commentMapper.toDto(comment);
        dto.setReplies(new ArrayList<>());
        return dto;
    }

    private void validatePrincipal(JwtPrincipal principal) {
        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new SecurityException("Missing required JWT claims");
        }
        if (!principal.verified()) {
            throw new SecurityException("User is not verified");
        }
    }

    private User resolveAuthor(JwtPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseGet(() -> {
                    String fullName = principal.name() != null && !principal.name().isBlank()
                            ? principal.name()
                            : principal.email().split("@")[0];
                    return userRepository.save(User.builder()
                            .fullName(fullName)
                            .email(principal.email())
                            .password(UUID.randomUUID().toString())
                            .registrationDate(LocalDateTime.now())
                            .lastSeen(LocalDateTime.now())
                            .build());
                });
    }
}
