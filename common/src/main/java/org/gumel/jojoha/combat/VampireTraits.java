package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;

/**
 * What being a vampire costs.
 *
 * <p>Currently one thing, and the one that matters: daylight. The design doc gives the vampire line
 * its strength in raw numbers - a longer time stop, more of everything - and a power with no
 * corresponding weakness is not a bargain, it is an upgrade. The sun is what makes turning a
 * decision rather than a reward.
 *
 * <p>Modelled on how vanilla burns the undead rather than invented: it takes daylight, an
 * unobstructed sky, and dry weather, and a helmet turns it aside. Matching those rules means every
 * habit a player already has for surviving as a skeleton transfers - shade works, rain works, a hat
 * works - instead of them having to learn a second, nearly identical set.
 */
public final class VampireTraits {
    /**
     * How long each burn lasts, in seconds.
     *
     * <p>Refreshed every tick the sun is on them, so the number only really decides how long they
     * keep burning after reaching cover - long enough to hurt, short enough that a dive indoors is
     * a rescue rather than a formality.
     */
    private static final int BURN_SECONDS = 6;

    /**
     * Damage per tick from a helmet turning the sun aside.
     *
     * <p>A hat is protection, not immunity. Vanilla lets a helmet stop the burn outright, which for
     * a player - who can always have one - would mean the weakness never applied at all. This is the
     * compromise: covered, you smoulder instead of igniting, and it costs you the helmet slowly.
     */
    private static final int HELMET_WEAR_PER_SECOND = 1;

    private VampireTraits() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.vampireStage == VampireStage.NONE) {
            return;
        }

        if (!inSunlight(player)) {
            return;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            // Once a second rather than every tick, or a helmet would be gone in seconds.
            if (player.tickCount % 20 == 0 && helmet.isDamageableItem()) {
                helmet.hurtAndBreak(HELMET_WEAR_PER_SECOND, player, EquipmentSlot.HEAD);
            }
            return;
        }

        player.igniteForSeconds(BURN_SECONDS);
    }

    /**
     * Whether the sun is actually on this player.
     *
     * <p>Every condition here is one vanilla already applies to the undead. Water and rain are
     * checked because a burning thing that stays lit in a downpour reads as a bug rather than as a
     * rule, and the sky check uses the block above their eyes rather than their feet - standing in
     * a doorway with your head in the light should still burn.
     */
    private static boolean inSunlight(Player player) {
        if (!(player.level() instanceof ServerLevel level) || !level.isDay() || level.isRaining()) {
            return false;
        }

        if (player.isInWaterOrRain() || player.isInPowderSnow) {
            return false;
        }

        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return level.canSeeSky(eyes) && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, eyes) > 8;
    }
}
