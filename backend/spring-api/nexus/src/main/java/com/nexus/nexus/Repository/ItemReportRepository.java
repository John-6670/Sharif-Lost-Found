package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.ItemReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemReportRepository extends JpaRepository<ItemReport, Long> {
    
    boolean existsByItemIdAndReporterId(Long itemId, Long reporterId);
    
    Optional<ItemReport> findByItemIdAndReporterId(Long itemId, Long reporterId);
}
