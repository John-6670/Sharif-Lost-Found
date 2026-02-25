package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.ItemReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemReportRepository extends JpaRepository<ItemReport, Long> {
    boolean existsByItemIdAndReporterId(Long itemId, Long reporterId);
    long countByItemId(Long itemId);
}
