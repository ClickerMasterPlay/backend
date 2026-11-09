package com.resourcemagnate.app.game.repository;


import com.resourcemagnate.app.game.model.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockTypeRepository extends JpaRepository<BlockType, String> {
}
