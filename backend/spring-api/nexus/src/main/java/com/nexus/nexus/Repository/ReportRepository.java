package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReportRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByCategory_Id(Long categoryId);

    @Query("""
            SELECT i FROM Item i
            WHERE (:minLat IS NULL OR i.latitude BETWEEN :minLat AND :maxLat)
              AND (:minLon IS NULL OR i.longitude BETWEEN :minLon AND :maxLon)
              AND (:name IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:type IS NULL OR i.type = :type)
              AND (:from IS NULL OR i.createdAt >= :from)
              AND (:to IS NULL OR i.createdAt <= :to)
            """)
    List<Item> searchByLocationAndFilters(
            @Param("minLat") java.math.BigDecimal minLat,
            @Param("maxLat") java.math.BigDecimal maxLat,
            @Param("minLon") java.math.BigDecimal minLon,
            @Param("maxLon") java.math.BigDecimal maxLon,
            @Param("name") String name,
            @Param("type") com.nexus.nexus.Enumaration.TypeOfReport type,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
