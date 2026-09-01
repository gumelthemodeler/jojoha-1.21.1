package org.gumel.jojoha.item;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;

/**
 * Feeding the mask.
 *
 * <p>The mask is inert stone until blood touches it - the spines only come out once it has been fed,
 * and it is the blood that wakes it rather than the wearing. So it has to be bloodied before it can
 * be put on, and the way to bloody it is to hold it while something dies: carried in the off hand,
 * it catches what the kill spills.
 *
 * <p>The off hand specifically, because that is the hand not holding the weapon. It makes the
 * gesture legible - one hand kills, the other collects - and it means the mask cannot be the thing
 * that made the kill, which would be a rock used as a club rather than a relic being fed.
 *
 * <p>The mark lives on the stack rather than on the player. A player can own several masks and only
 * the one that was out has been fed; recording it on them would bloody every mask in the chest at
 * home along with the one in their hand.
 */
public final class MaskBlood {
    /** The flag, inside the stack's own custom data so no component needs registering for it. */
    private static final String BLOODIED_TAG = "Bloodied";

    /** How much comes off the kill, and how far it spreads. */
    private static final int SPLATTER_PARTICLES = 45;
    private static final double SPLATTER_SPREAD = 0.35;

    private MaskBlood() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer killer)) {
                return EventResult.pass();
            }

            ItemStack offHand = killer.getItemInHand(InteractionHand.OFF_HAND);
            if (!offHand.is(ModItems.STONE_MASK.get()) || isBloodied(offHand)) {
                return EventResult.pass();
            }

            bloody(killer, offHand, entity);
            return EventResult.pass();
        });
    }

    public static boolean isBloodied(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(BLOODIED_TAG);
    }

    /**
     * Marks the mask and shows it happening.
     *
     * <p>The burst comes off the <em>body</em>, not off the mask - it is the kill that produced the
     * blood, and drawing it at the mask would make the mask look like the thing that bled. Watching
     * it leave the mob is what connects the two.
     */
    private static void bloody(ServerPlayer killer, ItemStack mask, LivingEntity victim) {
        CustomData.update(DataComponents.CUSTOM_DATA, mask, tag -> tag.putBoolean(BLOODIED_TAG, true));

        if (!(killer.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(ModRegistries.BLOOD_MOTE.get(),
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                SPLATTER_PARTICLES, SPLATTER_SPREAD, SPLATTER_SPREAD, SPLATTER_SPREAD, 0.02);

        level.playSound(null, killer.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.7F, 0.5F);

        killer.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.jojoha.mask.fed"), true);
    }

    /** Whether this player is carrying a mask that has been fed - used by the item's own check. */
    public static boolean holdingBloodiedMask(Player player) {
        return isBloodied(player.getItemInHand(InteractionHand.MAIN_HAND));
    }
}
