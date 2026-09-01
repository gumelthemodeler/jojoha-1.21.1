package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;

/**
 * Which vine and barb art a Hermit Purple skin uses.
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>Most Stands are one model wearing one sheet, so a skin is a single texture and {@code StandType}
 * carries it. Hermit Purple is three: the Stand itself, the vine it throws, and the barb on the end
 * of it. Those last two are drawn by entity and layer code that has never heard of a skin, so a
 * green Hermit Purple threw a purple rope with a purple hook on it.
 *
 * <p>The three sheets are indexed by the same number, and that number is the position in the Stand's
 * own skin list - so the order below is not a preference, it is a contract with
 * {@code StandTypes.registerHermitPurple}. Changing one without the other silently pairs a skin with
 * somebody else's rope.
 *
 * <h2>The glow sheets are derived, not drawn</h2>
 *
 * <p>Each skin also has a {@code _glowmask}, generated rather than painted. The three originals were
 * measured first: in every one, each pixel with alpha is byte-identical to the base sheet at the
 * same coordinate - a glowmask here is exactly "the base texture, masked to what glows". So each new
 * one is that skin's own colours under the original's alpha, which is the relationship the artist
 * already established rather than an invention of it. Without them a green skin would have glowed
 * purple, because the lit pass draws the mask's colours rather than tinting them.
 */
public final class HermitSkins {
    /**
     * The filename tail for each skin, in the order the Stand lists them.
     *
     * <p>Index 0 is the original and has no tail, which is what keeps the existing files where they
     * were - renaming them would have broken every resource pack that has ever touched this Stand.
     */
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

    /**
     * The skin worn by whoever threw this, or the original.
     *
     * <p>Read off the Stand entity rather than off the player's data, because the data is only
     * synced to its own owner - a vine thrown by somebody else has to be drawn from something every
     * client can see, and the Stand's skin is part of its tracked entity data.
     *
     * <p>Falls back to zero rather than refusing to draw. A rope with no Stand behind it is a
     * momentary state during a dismissal, and the original colours are a better answer for one frame
     * than a rope that blinks out.
     */
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
                    "textures/entity/" + sheet + TAILS[i] + (glow ? "_glowmask" : "") + ".png");
        }
        return all;
    }
}
