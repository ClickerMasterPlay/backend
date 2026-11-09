package com.resourcemagnate.app.challenge;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChallengeCatalog {

    public static final int VISIBLE_CHALLENGES = 3;

    private static final List<ChallengeDefinition> CHALLENGES = List.of(
            challenge(0, ChallengeKind.CLICKS, 15, 2),
            challenge(1, ChallengeKind.BREAK, 1, 5),
            challenge(2, ChallengeKind.EARN, 2, 3),
            challenge(3, ChallengeKind.CLICKS, 40, 4),
            challenge(4, ChallengeKind.BUY, 1, 12),
            challenge(5, ChallengeKind.BREAK, 2, 8),
            challenge(6, ChallengeKind.EARN, 8, 6),
            challenge(7, ChallengeKind.LEVEL, 2, 15),
            challenge(8, ChallengeKind.CLICKS, 100, 8),
            challenge(9, ChallengeKind.SPEND, 100, 20),
            challenge(10, ChallengeKind.BREAK, 4, 12),
            challenge(11, ChallengeKind.POWER, 1.2, 10),
            challenge(12, ChallengeKind.EARN, 25, 15),
            challenge(13, ChallengeKind.CLICKS, 200, 12),
            challenge(14, ChallengeKind.BUY, 2, 25),
            challenge(15, ChallengeKind.BREAK, 6, 20),
            challenge(16, ChallengeKind.LEVEL, 3, 30),
            challenge(17, ChallengeKind.SPEND, 400, 40),
            challenge(18, ChallengeKind.EARN, 60, 30),
            challenge(19, ChallengeKind.CLICKS, 400, 25),
            challenge(20, ChallengeKind.POWER, 1.5, 35),
            challenge(21, ChallengeKind.BREAK, 10, 40),
            challenge(22, ChallengeKind.BUY, 4, 50),
            challenge(23, ChallengeKind.EARN, 150, 60),
            challenge(24, ChallengeKind.LEVEL, 4, 70),
            challenge(25, ChallengeKind.CLICKS, 800, 50),
            challenge(26, ChallengeKind.SPEND, 1500, 80),
            challenge(27, ChallengeKind.BREAK, 16, 90),
            challenge(28, ChallengeKind.POWER, 2, 80),
            challenge(29, ChallengeKind.EARN, 400, 120),
            challenge(30, ChallengeKind.BUY, 6, 140),
            challenge(31, ChallengeKind.CLICKS, 1500, 100),
            challenge(32, ChallengeKind.LEVEL, 5, 160),
            challenge(33, ChallengeKind.BREAK, 24, 180),
            challenge(34, ChallengeKind.SPEND, 5000, 200),
            challenge(35, ChallengeKind.EARN, 1000, 250),
            challenge(36, ChallengeKind.POWER, 3, 220),
            challenge(37, ChallengeKind.CLICKS, 3000, 180),
            challenge(38, ChallengeKind.BUY, 8, 300),
            challenge(39, ChallengeKind.BREAK, 36, 350),
            challenge(40, ChallengeKind.LEVEL, 7, 400),
            challenge(41, ChallengeKind.EARN, 2500, 450),
            challenge(42, ChallengeKind.SPEND, 15000, 500),
            challenge(43, ChallengeKind.CLICKS, 6000, 400),
            challenge(44, ChallengeKind.POWER, 5, 550),
            challenge(45, ChallengeKind.BREAK, 50, 700),
            challenge(46, ChallengeKind.BUY, 10, 800),
            challenge(47, ChallengeKind.LEVEL, 9, 900),
            challenge(48, ChallengeKind.EARN, 8000, 1000),
            challenge(49, ChallengeKind.CLICKS, 12000, 900),
            challenge(50, ChallengeKind.SPEND, 40000, 1200),
            challenge(51, ChallengeKind.BREAK, 70, 1500),
            challenge(52, ChallengeKind.POWER, 10, 1800),
            challenge(53, ChallengeKind.LEVEL, 12, 2000),
            challenge(54, ChallengeKind.EARN, 25000, 2500),
            challenge(55, ChallengeKind.CLICKS, 25000, 3000),
            challenge(56, ChallengeKind.BREAK, 100, 4000),
            challenge(57, ChallengeKind.BUY, 14, 4500),
            challenge(58, ChallengeKind.POWER, 25, 6000),
            challenge(59, ChallengeKind.LEVEL, 16, 8000)
    );

    public List<ChallengeDefinition> all() {
        return CHALLENGES;
    }

    public ChallengeDefinition get(int index) {
        if (index < 0 || index >= CHALLENGES.size()) {
            return null;
        }
        return CHALLENGES.get(index);
    }

    private static ChallengeDefinition challenge(
            int index,
            ChallengeKind kind,
            double goal,
            long reward
    ) {
        return new ChallengeDefinition(index, kind, goal, reward);
    }
}
