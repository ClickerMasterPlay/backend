package com.resourcemagnate.app.game.repository;

import com.resourcemagnate.app.game.model.Tool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolRepository extends JpaRepository<Tool, String> {
}