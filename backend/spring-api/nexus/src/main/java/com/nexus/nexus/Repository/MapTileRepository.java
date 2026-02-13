package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.MapTile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MapTileRepository extends JpaRepository<MapTile, Long> {
}
