package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.item.ThrownDagger;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;

public final class TimeStopThrow {
    public static final int COOLDOWN_TICKS = 6;

    private static final double SPEED = 2.0;

    private static final float MAX_DAMAGE = 10.0F;

    private static final double AIM_REACH = 24.0;

    private static final double HAND_HEIGHT = 0.7;

    private static final double HAND_REACH = 0.4;

    private static final double HAND_SIDE = 0.35;

    private TimeStopThrow() {
    }

    public static boolean throwing(JojohaPlayerData data) {
        return data.timeStopHeldTicks > 0;
    }

    public static void handleThrow(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standSummoned || data.halfSummon || !data.stand.isPresent() || data.standMode.handlesItems()
                || !data.manifestTier().canStandPunch() || !StandTypes.hasBody(data)) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now < data.punchCooldownExpiry) {
            return;
        }
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null && weapon(player, data, stand)) {
            data.punchCooldownExpiry = now + COOLDOWN_TICKS;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
    }

    public static boolean weapon(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ItemStack held = player.getMainHandItem();
        if (!throwing(data) || held.isEmpty() || !WeaponClasses.swingsItself(held)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        float damage = DamageScale.stand(player, null,
                Math.min(MAX_DAMAGE, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        Vec3 from = hand(player, stand);
        Vec3 heading = aim(player).subtract(from);
        if (heading.lengthSqr() < 1.0E-4) {
            heading = player.getLookAngle();
        }

        ThrownDagger thrown = new ThrownDagger(level, player, held.copyWithCount(1), (float) (damage / SPEED));
        if (player.getAbilities().instabuild) {
            thrown.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        } else {
            held.shrink(1);
        }
        thrown.setPos(from.x, from.y, from.z);
        thrown.shoot(heading.x, heading.y, heading.z, (float) SPEED, 0F);
        level.addFreshEntity(thrown);

        stand.triggerKnifeThrowPose();
        level.playSound(null, stand.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS,
                1.0F, 0.8F + level.getRandom().nextFloat() * 0.2F);
        return true;
    }

    static Vec3 aim(ServerPlayer player) {
        LivingEntity target = SkillTargeting.lookTarget(player, AIM_REACH);
        return target != null
                ? target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
                : player.getEyePosition().add(player.getLookAngle().scale(AIM_REACH));
    }

    static Vec3 hand(ServerPlayer player, StandEntity stand) {
        Vec3 look = player.getLookAngle();
        Vec3 sideways = look.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 right = sideways.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : sideways.normalize();
        return stand.position().add(0.0, stand.getBbHeight() * HAND_HEIGHT, 0.0)
                .add(look.scale(HAND_REACH)).add(right.scale(HAND_SIDE));
    }
}
