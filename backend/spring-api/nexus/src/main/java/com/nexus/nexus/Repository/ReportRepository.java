package com.nexus.nexus.Repository;

import com.nexus.nexus.Enumaration.TypeOfReport;
import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ReportRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByCategory_Id(Long categoryId);

    List<Item> findAllByStatus(Status status);

    List<Item> findAllByCategory_IdAndStatus(Long categoryId, Status status);

    Page<Item> findAllByStatus(Status status, Pageable pageable);

    @Query("""
            SELECT i FROM Item i
            WHERE i.latitude BETWEEN COALESCE(:minLat, i.latitude) AND COALESCE(:maxLat, i.latitude)
              AND i.longitude BETWEEN COALESCE(:minLon, i.longitude) AND COALESCE(:maxLon, i.longitude)
              AND (COALESCE(:name, '') = '' OR LOWER(i.name) LIKE :name)
              AND i.type = COALESCE(:type, i.type)
              AND i.status = :status
              AND (:categoryIds IS NULL OR i.category.id IN :categoryIds)
              AND i.createdAt >= COALESCE(:from, i.createdAt)
              AND i.createdAt <= COALESCE(:to, i.createdAt)
              ORDER BY i.createdAt DESC
            """)
    List<Item> searchByLocationAndFilters(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon,
            @Param("name") String name,
            @Param("type") TypeOfReport type,
            @Param("status") Status status,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    Page<Item> findAll(Pageable pageable);

    @Query("""
            SELECT i FROM Item i
            WHERE i.latitude BETWEEN COALESCE(:minLat, i.latitude) AND COALESCE(:maxLat, i.latitude)
              AND i.longitude BETWEEN COALESCE(:minLon, i.longitude) AND COALESCE(:maxLon, i.longitude)
              AND (COALESCE(:name, '') = '' OR LOWER(i.name) LIKE :name)
              AND i.type = COALESCE(:type, i.type)
              AND i.status = :status
              AND (:categoryIds IS NULL OR i.category.id IN :categoryIds)
              AND i.createdAt >= COALESCE(:from, i.createdAt)
              AND i.createdAt <= COALESCE(:to, i.createdAt)
              ORDER BY i.createdAt DESC
            """)
    Page<Item> searchByLocationAndFilters(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon,
            @Param("name") String name,
            @Param("type") TypeOfReport type,
            @Param("status") Status status,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    long countByStatus(Status status);

    long countByReporter_IdAndType(Long reporterId, TypeOfReport type);
}
