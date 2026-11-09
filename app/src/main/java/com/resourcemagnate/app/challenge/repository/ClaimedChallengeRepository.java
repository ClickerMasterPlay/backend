package com.resourcemagnate.app.challenge.repository;

import com.resourcemagnate.app.challenge.model.ClaimedChallenge;
import com.resourcemagnate.app.challenge.model.ClaimedChallengeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimedChallengeRepository
        extends JpaRepository<ClaimedChallenge, ClaimedChallengeId> {

    List<ClaimedChallenge> findAllByUserIdOrderByChallengeIndex(UUID userId);
}
