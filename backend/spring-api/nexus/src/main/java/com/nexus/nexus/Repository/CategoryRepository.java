package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    java.util.Optional<Category> findByNameIgnoreCase(String name);
}
