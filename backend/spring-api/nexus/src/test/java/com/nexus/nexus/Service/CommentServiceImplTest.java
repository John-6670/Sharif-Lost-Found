package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Entity.Comment;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.CommentMapper;
import com.nexus.nexus.Repository.CommentReportRepository;
import com.nexus.nexus.Repository.CommentRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ServiceImplementation.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentServiceImpl service;

    private JwtPrincipal principal;
    private User reporter;

    @BeforeEach
    void setUp() {
        principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        reporter = User.builder().id(1L).email("user@example.com").fullName("User").build();
    }

    @Test
    void getCommentsForItem_paginatesRootsAndIncludesReplies() {
        Item item = Item.builder().id(1L).build();
        Comment root = Comment.builder().id(10L).item(item).text("root").createdAt(LocalDateTime.now()).build();
        Comment reply = Comment.builder().id(11L).item(item).parent(root).text("reply").createdAt(LocalDateTime.now()).build();

        Page<Comment> page = new PageImpl<>(List.of(root));
        when(reportRepository.findById(1L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItemIdAndParentIsNullOrderByCreatedAtAsc(eq(1L), any())).thenReturn(page);
        when(commentRepository.findByParentIdInOrderByCreatedAtAsc(List.of(10L))).thenReturn(List.of(reply));

        when(commentMapper.toDto(root)).thenReturn(CommentResponseDto.builder().id(10L).build());
        when(commentMapper.toDto(reply)).thenReturn(CommentResponseDto.builder().id(11L).build());

        CommentPage result = service.getCommentsForItem(1L, 0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getReplies()).hasSize(1);
        assertThat(result.totalItems()).isEqualTo(1);
    }

    @Test
    void reportComment_deletesAfterThreeReports() {
        Item item = Item.builder().id(1L).build();
        Comment comment = Comment.builder().id(2L).item(item).build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(item));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(comment));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(reporter));
        when(commentReportRepository.existsByCommentIdAndReporterId(2L, 1L)).thenReturn(false);
        when(commentReportRepository.countByCommentId(2L)).thenReturn(3L);

        service.reportComment(1L, 2L, "spam", principal);

        verify(commentRepository).deleteByParentId(2L);
        verify(commentReportRepository).deleteByCommentId(2L);
        verify(commentRepository).deleteById(2L);
    }

    @Test
    void reportComment_rejectsDuplicateReport() {
        Item item = Item.builder().id(1L).build();
        Comment comment = Comment.builder().id(2L).item(item).build();

        when(reportRepository.findById(1L)).thenReturn(Optional.of(item));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(comment));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(reporter));
        when(commentReportRepository.existsByCommentIdAndReporterId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.reportComment(1L, 2L, "spam", principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reported");
    }

    @Test
    void addComment_rejectsBlankText() {
        assertThatThrownBy(() -> service.addComment(1L, new CommentRequestDto("  ", null), principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }
}
