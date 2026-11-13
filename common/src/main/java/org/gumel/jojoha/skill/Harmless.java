package org.gumel.jojoha.skill;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Set;

public final class Harmless {
    private static final Set<String> MOVES = Set.of(
            "dodge", "double_jump", "burst", "time_skip", "heaven_step", "wall_crawl", "contortion",
            "adaptive_flight", "hermit_grapple", "thorn_zip", "web_swing");

    private Harmless() {
    }

    public static boolean is(ResourceLocation move) {
        return move != null && Jojoha.MOD_ID.equals(move.getNamespace()) && MOVES.contains(move.getPath());
    }
}
