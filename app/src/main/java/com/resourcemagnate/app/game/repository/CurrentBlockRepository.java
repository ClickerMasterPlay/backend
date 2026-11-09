package com.resourcemagnate.app.game.repository;

import com.resourcemagnate.app.game.model.CurrentBlock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CurrentBlockRepository
        extends JpaRepository<CurrentBlock, UUID> {

    @Query("""
        select cb
        from CurrentBlock cb
        join fetch cb.blockType
        where cb.userId = :userId
        """)
    Optional<CurrentBlock> findByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select cb
        from CurrentBlock cb
        join fetch cb.blockType
        where cb.userId = :userId
        """)
    Optional<CurrentBlock> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}
