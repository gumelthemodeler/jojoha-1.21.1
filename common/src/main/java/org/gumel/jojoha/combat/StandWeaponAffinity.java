package org.gumel.jojoha.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.item.MeteoriteTier;
import org.gumel.jojoha.item.WeaponClass;
import org.gumel.jojoha.stand.StandTypes;

import java.util.Map;

public final class StandWeaponAffinity {
    public static final ResourceLocation SILVER_CHARIOT_ID = StandTypes.SILVER_CHARIOT_ID;

    private static final Map<ResourceLocation, WeaponClass> AFFINITIES =
            Map.of(SILVER_CHARIOT_ID, WeaponClass.RAPIER);

    private static final float WOOD = 0.02F;
    private static final float STONE = 0.03F;
    private static final float IRON = 0.05F;
    private static final float GOLD = 0.04F;
    private static final float DIAMOND = 0.07F;
    private static final float NETHERITE = 0.09F;
    private static final float METEORITE = 0.10F;

    private StandWeaponAffinity() {
    }

    public static float multiplier(Player player, JojohaPlayerData data) {
        if (!data.stand.isPresent() || !data.standSummoned) {
            return 1F;
        }

        WeaponClass wanted = AFFINITIES.get(data.stand.standId());
        if (wanted == null) {
            return 1F;
        }

        ItemStack held = player.getMainHandItem();
        if (WeaponClasses.held(player) != wanted) {
            return 1F;
        }

        return 1F + bonusFor(held);
    }

    private static float bonusFor(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredItem tiered)) {
            return 0F;
        }

        Tier tier = tiered.getTier();
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
        return 0F;
    }
}
