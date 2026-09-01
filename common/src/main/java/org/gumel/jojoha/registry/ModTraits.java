package org.gumel.jojoha.registry;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The traits listed in the design doc, registered as data only (id + description + rarity
 * flag). Gameplay effects are future work — this pass just gives every future system a
 * stable id to point at, and a slot for it on {@code JojohaPlayerData#trait}.
 */
public final class ModTraits {
    private static final Map<ResourceLocation, Trait> TRAITS = new LinkedHashMap<>();

    public static final Trait SILENT_FOOTSTEPS = register("silent_footsteps",
            "Footsteps no longer make sound.", false);
    public static final Trait TWO_RIGHT_HANDS = register("two_right_hands",
            "Holding a weapon in your offhand allows you to attack with it.", false);
    public static final Trait RUNNING_STYLE = register("running_style",
            "Gain speed while running, similarly to a horse; can also charge a jump the same way.", false);
    public static final Trait SECRET_TECHNIQUE = register("secret_technique",
            "Below 40% HP, running speed is increased.", false);
    public static final Trait KILLERS_LUCK = register("killers_luck",
            "Fluctuating \"killing prowess\", shifting roughly once per in-game day. Higher levels grant power and luck, lower levels grant nothing.", false);
    public static final Trait JOESTAR_BIRTHMARK = register("joestar_birthmark",
            "Players with higher WOR near you can passively gain a Stand.", true);
    public static final Trait DEVILS_BLESSING = register("devils_blessing",
            "Deal more damage to players with high WOR.", true);

    private ModTraits() {
    }

    private static Trait register(String path, String description, boolean legendary) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        Trait trait = new Trait(id, description, legendary);
        TRAITS.put(id, trait);
        return trait;
    }

    public static Trait byId(ResourceLocation id) {
        return TRAITS.get(id);
    }

    public static Map<ResourceLocation, Trait> all() {
        return Collections.unmodifiableMap(TRAITS);
    }

    /** No-op call site to force this class's static initializers to run. */
    public static void bootstrap() {
    }

    public record Trait(ResourceLocation id, String description, boolean legendary) {
    }
}
