package org.gumel.jojoha.stand.skill.moves;

/**
 * Starts the per-tick machinery some moves need.
 *
 * <p>A public door onto package-private helpers, so those helpers do not have to be opened up to
 * the whole mod just to be switched on once at startup.
 */
public final class MoveTickers {
    private MoveTickers() {
    }

    public static void init() {
        SkullCrusherSkill.init();
        PlayerFlurry.init();
        InhaleChannel.init();
    }
}
