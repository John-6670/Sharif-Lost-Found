package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Entity.Comment;
import com.nexus.nexus.Entity.CommentReport;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.CommentMapper;
import com.nexus.nexus.Repository.CommentReportRepository;
import com.nexus.nexus.Repository.CommentRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.CommentPage;
import com.nexus.nexus.Service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public CommentPage getCommentsForItem(Long itemId, int page, int size) {
        // Verify item exists
        reportRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        Page<Comment> rootsPage = commentRepository.findByItemIdAndParentIsNullOrderByCreatedAtAsc(
                itemId, PageRequest.of(safePage, safeSize));
        List<Comment> roots = rootsPage.getContent();

        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        List<Comment> replies = rootIds.isEmpty()
                ? List.of()
                : commentRepository.findByParentIdInOrderByCreatedAtAsc(rootIds);

        Map<Long, CommentResponseDto> rootDtoMap = new LinkedHashMap<>();
        for (Comment root : roots) {
            CommentResponseDto dto = commentMapper.toDto(root);
            dto.setReplies(new ArrayList<>());
            rootDtoMap.put(root.getId(), dto);
        }

        Map<Long, List<CommentResponseDto>> repliesByParent = new HashMap<>();
        for (Comment reply : replies) {
            CommentResponseDto dto = commentMapper.toDto(reply);
            dto.setReplies(new ArrayList<>());
            repliesByParent.computeIfAbsent(reply.getParent().getId(), key -> new ArrayList<>())
                    .add(dto);
        }

        for (Map.Entry<Long, List<CommentResponseDto>> entry : repliesByParent.entrySet()) {
            CommentResponseDto parent = rootDtoMap.get(entry.getKey());
            if (parent != null) {
                parent.getReplies().addAll(entry.getValue());
            }
        }

        List<CommentResponseDto> items = new ArrayList<>(rootDtoMap.values());
        return new CommentPage(
                items,
                safePage,
                safeSize,
                rootsPage.getTotalElements(),
                rootsPage.getTotalPages(),
                rootsPage.hasNext()
        );
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

    @Override
    @Transactional
    public void reportComment(Long itemId, Long commentId, String cause, JwtPrincipal principal) {
        validatePrincipal(principal);

        if (cause == null || cause.isBlank()) {
            throw new IllegalArgumentException("Report cause is required");
        }

        // Verify item exists
        reportRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        if (!comment.getItem().getId().equals(itemId)) {
            throw new IllegalArgumentException("Comment does not belong to this item");
        }

        User reporter = resolveAuthor(principal);

        if (commentReportRepository.existsByCommentIdAndReporterId(commentId, reporter.getId())) {
            throw new IllegalArgumentException("You have already reported this comment");
        }

        commentReportRepository.save(CommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .cause(cause.trim())
                .createdAt(OffsetDateTime.now())
                .build());

        long reportCount = commentReportRepository.countByCommentId(commentId);
        if (reportCount >= 3) {
            // Delete replies first to avoid FK violations
            commentRepository.deleteByParentId(commentId);
            commentReportRepository.deleteByCommentId(commentId);
            commentRepository.deleteById(commentId);
        }
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
                            .registrationDate(OffsetDateTime.now())
                            .lastSeen(OffsetDateTime.now())
                            .isVerified(true)
                            .isSuperuser(false)
                            .build());
                });
    }
}
