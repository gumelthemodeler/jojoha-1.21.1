package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;

public final class HermitSkins {
    private static final String[] TAILS = {
            "", "_blue", "_gold", "_green", "_pink", "_red", "_white"};

    private static final ResourceLocation[] ROPE = build("hermit_grapple", false);
    private static final ResourceLocation[] ROPE_GLOW = build("hermit_grapple", true);
    private static final ResourceLocation[] HOOK = build("hermit_grapple_hook", false);
    private static final ResourceLocation[] HOOK_GLOW = build("hermit_grapple_hook", true);

    private HermitSkins() {
    }

    public static ResourceLocation rope(int skin) {
        return ROPE[clamp(skin)];
    }

    public static ResourceLocation ropeGlow(int skin) {
        return ROPE_GLOW[clamp(skin)];
    }

    public static ResourceLocation hook(int skin) {
        return HOOK[clamp(skin)];
    }

    public static ResourceLocation hookGlow(int skin) {
        return HOOK_GLOW[clamp(skin)];
    }

    public static int of(Entity thrower) {
        if (!(thrower instanceof Player player)) {
            return 0;
        }

        StandEntity stand = StandEntityLookup.boundStandOf(player);
        return stand == null ? 0 : stand.getSkin();
    }

    private static int clamp(int skin) {
        return skin >= 0 && skin < TAILS.length ? skin : 0;
    }

    private static ResourceLocation[] build(String sheet, boolean glow) {
        ResourceLocation[] all = new ResourceLocation[TAILS.length];
        for (int i = 0; i < TAILS.length; i++) {
            all[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/entity/stand/hermit_purple/grapple/" + sheet + TAILS[i] + (glow ? "_glowmask" : "") + ".png");
        }
        return all;
    }
}
