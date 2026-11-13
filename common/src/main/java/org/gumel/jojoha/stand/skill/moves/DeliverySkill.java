package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class DeliverySkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "delivery");

    public static final DeliverySkill INSTANCE = new DeliverySkill();

    private static final int COOLDOWN_TICKS = 20;
    private static final double FORGIVENESS = 0.8;

    private DeliverySkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.delivery";
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return data.stand.isPresent();
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return EnergyWeight.TRIVIAL.cost();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        if (player.getMainHandItem().isEmpty()) {
            return "message.jojoha.delivery.empty_hand";
        }
        return recipient(player, data) == null && container(player, data) == null
                ? "message.jojoha.delivery.nobody" : null;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ServerPlayer other = recipient(player, data);
        if (other != null) {
            ItemStack sent = held.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!other.getInventory().add(sent)) {
                other.drop(sent, false);
            }
            arrive(level, player, stand, other.position().add(0, other.getBbHeight() * 0.6, 0));
            return true;
        }
        BlockHitResult hit = aim(player, data);
        Container box = container(player, data);
        if (box == null) {
            return false;
        }
        ItemStack left = HopperBlockEntity.addItem(null, box, held.copy(), hit.getDirection());
        if (left.getCount() == held.getCount()) {
            player.displayClientMessage(Component.translatable(
                    "message.jojoha.delivery.full"), true);
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, left);
        box.setChanged();
        arrive(level, player, stand, Vec3.atCenterOf(hit.getBlockPos()));
        return true;
    }

    private static BlockHitResult aim(ServerPlayer player, JojohaPlayerData data) {
        return ChoreSkill.aim(player, data);
    }

    private static ServerPlayer recipient(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity seen = VineHaul.look(player, StandHands.reachFor(data), FORGIVENESS);
        return seen instanceof ServerPlayer other && !other.isSpectator() ? other : null;
    }

    private static Container container(ServerPlayer player, JojohaPlayerData data) {
        BlockHitResult hit = aim(player, data);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        ServerLevel level = player.serverLevel();
        if (!level.mayInteract(player, pos)) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity base && !base.canOpen(player)) {
            return null;
        }
        return HopperBlockEntity.getContainerAt(level, pos);
    }

    private static void arrive(ServerLevel level, ServerPlayer player, StandEntity stand, Vec3 to) {
        if (stand != null) {
            stand.reachTowards(to);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, BlockPos.containing(to), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1F);
    }
}
