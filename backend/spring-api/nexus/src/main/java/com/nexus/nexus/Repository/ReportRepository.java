package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ReportRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByCategory_Id(Long categoryId);

    List<Item> findAllByStatus(com.nexus.nexus.Enumaration.Status status);

    List<Item> findAllByCategory_IdAndStatus(Long categoryId, com.nexus.nexus.Enumaration.Status status);

    Page<Item> findAllByStatus(com.nexus.nexus.Enumaration.Status status, Pageable pageable);

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
            """)
    List<Item> searchByLocationAndFilters(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLon") java.math.BigDecimal minLon,
            @Param("maxLon") java.math.BigDecimal maxLon,
            @Param("name") String name,
            @Param("type") com.nexus.nexus.Enumaration.TypeOfReport type,
            @Param("status") com.nexus.nexus.Enumaration.Status status,
            @Param("categoryIds") java.util.List<Long> categoryIds,
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
            """)
    Page<Item> searchByLocationAndFilters(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLon") java.math.BigDecimal minLon,
            @Param("maxLon") java.math.BigDecimal maxLon,
            @Param("name") String name,
            @Param("type") com.nexus.nexus.Enumaration.TypeOfReport type,
            @Param("status") com.nexus.nexus.Enumaration.Status status,
            @Param("categoryIds") java.util.List<Long> categoryIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    long countByStatus(com.nexus.nexus.Enumaration.Status status);
}
