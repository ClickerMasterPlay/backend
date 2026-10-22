package com.resourcemagnate.app.game.repository;


import com.resourcemagnate.app.game.model.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlockTypeRepository extends JpaRepository<BlockType, Long> {

    Optional<BlockType> findByCode(String code);
}
