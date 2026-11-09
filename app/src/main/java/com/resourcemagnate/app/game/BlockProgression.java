package com.resourcemagnate.app.game;

public final class BlockProgression {

    private BlockProgression() {
    }

    /**
     * Selects the next block by the number of blocks already destroyed.
     */
    public static String typeCode(long blocksBroken) {
        if (blocksBroken >= 50) {
            return "netherite";
        }
        if (blocksBroken >= 30) {
            return "diamond";
        }
        if (blocksBroken >= 15) {
            return "gold";
        }
        if (blocksBroken >= 5) {
            return "iron";
        }
        return "stone";
    }
}
