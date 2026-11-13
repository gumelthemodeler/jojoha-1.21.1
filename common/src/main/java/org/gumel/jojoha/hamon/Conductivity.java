package org.gumel.jojoha.hamon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.item.ClackersItem;
import org.gumel.jojoha.item.MeteoriteTier;
import org.gumel.jojoha.skill.Indicator;

import java.util.List;

public final class Conductivity {
    public static final float NEUTRAL = 1.0F;

    private static final float WOOD = 0.85F;
    private static final float STONE = 0.95F;
    private static final float IRON = 1.10F;
    private static final float GOLD = 1.35F;
    private static final float DIAMOND = 1.00F;
    private static final float NETHERITE = 1.15F;
    private static final float METEORITE = 1.20F;

    private static final float CLACKERS = 1F + ClackersItem.HAMON_BONUS;

    private Conductivity() {
    }

    public static void appendLine(ItemStack stack,
                                  List<Component> lines) {
        float rating = of(stack);
        String percent = Math.round(rating * 100) + "%";
        lines.add(Indicator.HAMON.line(Component.translatable(
                        "tooltip.jojoha.conductivity", percent)
                .withStyle(rating > NEUTRAL ? ChatFormatting.GOLD
                        : ChatFormatting.DARK_GRAY)));
    }

    public static float of(ItemStack stack) {
        if (stack.isEmpty()) {
            return NEUTRAL;
        }
        if (stack.getItem() instanceof ClackersItem) {
            return CLACKERS;
        }
        if (!(stack.getItem() instanceof TieredItem tiered)) {
            return NEUTRAL;
        }
        return ofTier(tiered.getTier());
    }

    private static float ofTier(Tier tier) {
        if (tier == MeteoriteTier.INSTANCE) {
            return METEORITE;
        }
        if (tier == Tiers.WOOD) {
            return WOOD;
        }
        if (tier == Tiers.STONE) {
            return STONE;
        }
        if (tier == Tiers.IRON) {
            return IRON;
        }
        if (tier == Tiers.GOLD) {
            return GOLD;
        }
        if (tier == Tiers.DIAMOND) {
            return DIAMOND;
        }
        if (tier == Tiers.NETHERITE) {
            return NETHERITE;
        }
        return NEUTRAL;
    }

    public static float multiplier(Player player) {
        if (!Fate.breathesHamon(PlayerDataAccess.get(player))) {
            return NEUTRAL;
        }
        return Math.max(of(player.getMainHandItem()), of(player.getOffhandItem()));
    }
}
