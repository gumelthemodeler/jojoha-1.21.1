package org.gumel.jojoha.vampire.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampireState;

public final class VaporousFreezeMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vaporous_freeze");
    public static final VaporousFreezeMove INSTANCE = new VaporousFreezeMove();

    private static final float DAMAGE = 8.0F;
    private static final double REACH = 3.5;
    private static final double WIDTH = 1.3;

    private static final int SLOW_TICKS = 100;
    private static final int SLOW_LEVEL = 3;

    public static final int FREEZE_TICKS = 100;

    private static final int FROST_RADIUS = 2;

    private static final int COOLDOWN_TICKS = 90;
    private static final float ENERGY_COST = 22F;

    private VaporousFreezeMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();

        if (!VampireState.isFrozen(player)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.vampire.not_cold"), true);
            return;
        }

        MovementFx.pose(player, MovementFx.GUT_PUNCH);
        player.swing(InteractionHand.MAIN_HAND, true);
        StandBeat.at(MovementFx.GUT_PUNCH_CONTACT_TICKS, player, () -> strike(player));
    }

    private static void freeze(LivingEntity target) {
        int ticks = JojohaServerConfig.VAPOROUS_FREEZE_TICKS.whole();
        if (ticks <= 0 || !target.canFreeze()) {
            return;
        }
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks * 2));
    }

    private static void strike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 at = player.getEyePosition().add(look.scale(REACH * 0.6));
        AABB probe = new AABB(at.subtract(WIDTH, WIDTH, WIDTH), at.add(WIDTH, WIDTH, WIDTH));

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, probe,
                entity -> entity != player && entity.isAlive())) {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Impacts.landed(level, target, look, ImpactSpec.HEAVY, Impacts.FROST);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOW_TICKS, SLOW_LEVEL));

            target.clearFire();
            freeze(target);

            VampireFx.frost(level, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 18);
        }

        BlockPos centre = BlockPos.containing(at);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-FROST_RADIUS, -1, -FROST_RADIUS),
                centre.offset(FROST_RADIUS, 1, FROST_RADIUS))) {
            if (level.getFluidState(pos).getType() == Fluids.WATER
                    && level.getFluidState(pos).isSource()) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            }
        }

        VampireFx.frost(level, at.x, at.y, at.z, 26);
        VampireFx.ring(level, at.x, at.y, at.z, 1.6, 2);
        VampireFx.blood(level, at.x, at.y, at.z, 10, 0.3, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.9F, 1.6F);
    }
}
