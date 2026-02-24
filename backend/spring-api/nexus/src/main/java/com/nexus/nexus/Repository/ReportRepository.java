package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByCategory_Id(Long categoryId);

    List<Item> findByLatitudeBetweenAndLongitudeBetween(
            java.math.BigDecimal minLat,
            java.math.BigDecimal maxLat,
            java.math.BigDecimal minLon,
            java.math.BigDecimal maxLon
    );
}
