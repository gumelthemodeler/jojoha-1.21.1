package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class HeavenStepSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heaven_step");
    public static final HeavenStepSkill INSTANCE = new HeavenStepSkill();

    public static final double REACH = 16.0;
    public static final double BEHIND = 1.4;
    public static final float DAMAGE = 11F;
    public static final double KNOCKBACK = 0.9;
    public static final double LIFT = 0.35;
    public static final int CONTACT_TICKS = StandEntity.STRIKE_CONTACT_TICKS;
    private static final double[] SIDES = {0.0, 90.0, -90.0, 180.0};

    private static final int COOLDOWN_TICKS = 100;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private HeavenStepSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.heaven_step";
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
    public boolean movesTheUsersBody() {
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return false;
        }
        LivingEntity target = SkillTargeting.lookTarget(player, Heaven.range(REACH));
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.jojoha.heaven.step_no_target"), true);
            return false;
        }
        Vec3 landing = landing(player, target);
        if (landing == null) {
            player.displayClientMessage(Component.translatable("message.jojoha.heaven.step_blocked"), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        HeavenShow.full(level, HeavenFxPacket.STEP, player.getId(), -1, player.position(), 0F, 0);
        Vec3 toTarget = target.position().subtract(landing);
        float yaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        player.teleportTo(level, landing.x, landing.y, landing.z, yaw, player.getXRot());
        player.fallDistance = 0F;
        Vec3 beside = landing.add(new Vec3(toTarget.x, 0.0, toTarget.z).normalize().scale(0.6));
        stand.teleportTo(beside.x, landing.y, beside.z);
        stand.triggerStrikeAt(target, "punch2");
        HeavenShow.full(level, HeavenFxPacket.STEP, player.getId(), -1, landing, 1F, 0);
        level.playSound(null, player.blockPosition(), ModSounds.TIME_SKIP.get(), SoundSource.PLAYERS, 0.9F, 1.2F);
        StandBeat.after(CONTACT_TICKS, player, target, HeavenStepSkill::land);
        return true;
    }

    private static Vec3 landing(ServerPlayer player, LivingEntity target) {
        float facing = target.getYRot();
        for (double turn : SIDES) {
            double r = Math.toRadians(facing + turn);
            Vec3 back = new Vec3(Math.sin(r), 0.0, -Math.cos(r)).scale(BEHIND + target.getBbWidth() * 0.5);
            Vec3 spot = target.position().add(back);
            AABB box = player.getBoundingBox().move(spot.subtract(player.position()));
            if (player.serverLevel().noCollision(player, box)) {
                return spot;
            }
        }
        return null;
    }

    private static void land(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive() || target.distanceToSqr(player) > 16.0) {
            Whiff.stand(player);
            return;
        }
        Vec3 away = target.position().subtract(player.position());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        Vec3 push = (flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(KNOCKBACK)).add(0.0, LIFT, 0.0);
        if (HeavenStrikes.strike(player, target, "heaven_step", DAMAGE, push, ImpactSpec.HEAVY, true)) {
            Stun.stagger(target);
        }
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.2F, 0.7F);
    }
}
