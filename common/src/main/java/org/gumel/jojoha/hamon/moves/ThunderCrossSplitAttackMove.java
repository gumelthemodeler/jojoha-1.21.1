package org.gumel.jojoha.hamon.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class ThunderCrossSplitAttackMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thunder_cross_split_attack");
    public static final ThunderCrossSplitAttackMove INSTANCE = new ThunderCrossSplitAttackMove();

    public static final float DAMAGE = 7.0F;
    public static final float UNDEAD_DAMAGE = 10.0F;
    public static final double REACH = 3.0;
    private static final double ARC_COS = 0.5;
    private static final double LEAP_FORWARD = 0.9;
    private static final double LEAP_UP = 0.5;
    private static final double CROSS_AHEAD = 1.4;
    private static final double CROSS_HALF = 0.9;
    private static final int CROSS_MARKS = 7;

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = 22F;

    private ThunderCrossSplitAttackMove() {
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, MovementFx.THUNDER_CROSS);
        StandBeat.at(MovementFx.THUNDER_CROSS_LAUNCH_TICKS,
                player, () -> launch(player));
        StandBeat.at(MovementFx.THUNDER_CROSS_CONTACT_TICKS
                        - MovementFx.PALM_CONTACT_TICKS, player,
                () -> NetworkHandler.sendFirstPersonPose(player, "palm"));
        StandBeat.at(MovementFx.THUNDER_CROSS_CONTACT_TICKS, player,
                () -> chop(player));
        return true;
    }

    private static void launch(ServerPlayer player) {
        Vec3 ahead = flatLook(player);
        Vec3 launch = new Vec3(ahead.x * LEAP_FORWARD, LEAP_UP, ahead.z * LEAP_FORWARD);
        player.setDeltaMovement(launch);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), launch));
        player.setKnownMovement(launch);
        player.fallDistance = 0F;
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0F, player.getYRot()) : flat.normalize();
    }

    private static void chop(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 ahead = flatLook(player);
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        Vec3 origin = player.position();
        player.fallDistance = 0F;

        Vec3 cross = origin.add(0.0, player.getBbHeight() * 0.6, 0.0).add(ahead.scale(CROSS_AHEAD));
        for (int i = 0; i < CROSS_MARKS; i++) {
            double t = (i / (double) (CROSS_MARKS - 1) * 2.0 - 1.0) * CROSS_HALF;
            Vec3 one = cross.add(right.scale(t)).add(0.0, t, 0.0);
            Vec3 two = cross.add(right.scale(t)).add(0.0, -t, 0.0);
            HamonFx.spark(level, one.x, one.y, one.z, 2, 0.03, 0.0);
            HamonFx.spark(level, two.x, two.y, two.z, 2, 0.03, 0.0);
        }
        HamonFx.ring(level, cross.x, cross.y, cross.z, 1.4, 1);

        boolean landed = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(REACH, 1.5, REACH),
                entity -> entity != player && entity.isAlive()
                        && !(entity instanceof StandEntity))) {
            Vec3 to = new Vec3(target.getX() - origin.x, 0.0, target.getZ() - origin.z);
            double distance = to.length();
            if (distance > REACH + target.getBbWidth() * 0.5
                    || (distance > 1.0E-3 && to.scale(1.0 / distance).dot(ahead) < ARC_COS)) {
                continue;
            }
            float damage = HamonCombat.isUndead(target) ? UNDEAD_DAMAGE : DAMAGE;
            if (target.hurt(level.damageSources().playerAttack(player),
                    DamageScale.hamon(player, target, HamonCombat.scaled(player, damage)))) {
                Stun.stagger(target);
            }
            target.setDeltaMovement(target.getDeltaMovement().add(ahead.x * 0.3, -0.2, ahead.z * 0.3));
            target.hurtMarked = true;
            Impacts.landed(level, target, ahead,
                    ImpactSpec.HEAVY, Impacts.HAMON);
            HamonFx.ripple(level, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 4);
            landed = true;
        }

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.9F, landed ? 0.9F : 1.3F);
        HamonFx.sound(level, player, 0.9F, 1.0F);
    }
}
