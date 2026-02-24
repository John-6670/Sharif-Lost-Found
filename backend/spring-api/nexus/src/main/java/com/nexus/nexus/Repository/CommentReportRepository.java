package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    long countByCommentId(Long commentId);

    boolean existsByCommentIdAndReporterId(Long commentId, Long reporterId);

    void deleteByCommentId(Long commentId);
}
