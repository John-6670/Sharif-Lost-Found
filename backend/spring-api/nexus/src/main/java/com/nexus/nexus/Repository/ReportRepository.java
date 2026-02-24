package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByIsRemovedFalse();

    @Query("SELECT i FROM Item i WHERE i.isRemoved = false " +
            "AND (LOWER(i.itemName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Item> searchByKeyword(@Param("keyword") String keyword);
}
