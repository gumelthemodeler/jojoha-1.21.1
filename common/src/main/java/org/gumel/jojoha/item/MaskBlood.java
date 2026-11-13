package org.gumel.jojoha.item;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.gumel.jojoha.registry.ModRegistries;

public final class MaskBlood {
    private static final String BLOODIED_TAG = "Bloodied";

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
            if (!(offHand.getItem() instanceof StoneMaskItem) || isBloodied(offHand)) {
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
                Component.translatable("message.jojoha.mask.fed"), true);
    }

    public static boolean holdingBloodiedMask(Player player) {
        return isBloodied(player.getItemInHand(InteractionHand.MAIN_HAND));
    }
}
