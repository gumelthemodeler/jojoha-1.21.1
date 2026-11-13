package org.gumel.jojoha.registry;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModTraits {
    private static final Map<ResourceLocation, Trait> TRAITS = new LinkedHashMap<>();

    public static final Trait SILENT_FOOTSTEPS = register("silent_footsteps",
            "Footsteps no longer make sound.", false);
    public static final Trait TWO_RIGHT_HANDS = register("two_right_hands",
            "Melee attacks have a 20% chance to strike again with your other right hand, for half damage and a brief interrupt. Three in 5 seconds make your next attack a Double Blow.", false);
    public static final Trait RUNNING_STYLE = register("running_style",
            "Gain speed while running, similarly to a horse; can also charge a jump the same way.", false);
    public static final Trait SECRET_TECHNIQUE = register("secret_technique",
            "Below 30% HP, running speed is increased.", false);
    public static final Trait KILLERS_LUCK = register("killers_luck",
            "Fluctuating \"killing prowess\", shifting roughly once per in-game day. Higher levels grant power and luck, lower levels grant nothing.", false);
    public static final Trait JOESTAR_BIRTHMARK = register("joestar_birthmark",
            "Players with higher WOR near you can passively gain a Stand.", true);
    public static final Trait DEVILS_BLESSING = register("devils_blessing",
            "Deal more damage to players with high WOR.", true);
    public static final Trait KEEN_EYES = register("keen_eyes",
            "Invisible creatures within 8 blocks show as a faint outline.", false);
    public static final Trait IRON_STOMACH = register("iron_stomach",
            "Food never harms you; vampires draw 10% more blood.", false);
    public static final Trait DEEP_BREATH = register("deep_breath",
            "Hold your breath 50% longer; the Hamon bar refills 10% faster.", false);
    public static final Trait PLAYING_DEAD = register("playing_dead",
            "Below 20% health, lying still while sneaking for 3 seconds makes mobs lose interest.", false);
    public static final Trait STEADY_HAND = register("steady_hand",
            "Thrown weapons and arrows fly 15% farther.", false);

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

    public static void bootstrap() {
    }

    public record Trait(ResourceLocation id, String description, boolean legendary) {
        public String nameKey() {
            return "trait.jojoha." + id.getPath();
        }

        public String descriptionKey() {
            return nameKey() + ".desc";
        }
    }
}
