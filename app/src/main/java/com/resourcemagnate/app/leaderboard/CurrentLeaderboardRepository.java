package com.resourcemagnate.app.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrentLeaderboardRepository extends JpaRepository<CurrentLeaderboard, UUID> {
    
    Optional<CurrentLeaderboard> findByUserId(UUID userId);
    
    @Query("SELECT cl FROM CurrentLeaderboard cl ORDER BY cl.rank ASC")
    List<CurrentLeaderboard> findTopByOrderByRankAsc(int limit);
    
    @Query("SELECT COUNT(*) FROM CurrentLeaderboard")
    long countAll();
}