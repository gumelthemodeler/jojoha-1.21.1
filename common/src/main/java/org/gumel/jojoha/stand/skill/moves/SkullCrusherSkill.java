package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.SkullFlashPacket;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class SkullCrusherSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "skull_crusher");
    public static final SkullCrusherSkill INSTANCE = new SkullCrusherSkill();

    private static final int COOLDOWN_TICKS = 140;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private static final double REACH = 4.5;

    private static final int GRAB_TICKS = 20;
    private static final int CRUSH_TICKS = 15;

    private static final int TRAIL_TICKS = 12;

    private static final double GRIP_DISTANCE = 1.55;
    private static final double GRIP_MAX_LIFT = 0.85;

    private static final double STAND_FORWARD = 0.55;
    private static final double STAND_SIDE = 0.9;
    private static final double STAND_LIFT = 0.4;

    private static final float DAMAGE = 6.0F;
    private static final double LAUNCH_AWAY = 1.35;
    private static final double LAUNCH_UP = 0.55;

    private static final int STUN_TICKS = 60;

    private static final int LAUNCH_RINGS = 6;
    private static final double LAUNCH_RING_GAP = 0.9;
    private static final double LAUNCH_RING_SCALE = 2.6;

    private static final double RING_BAND_LARGE = 2.0;

    public static final float POP_AT = 0.6F;
    public static final float POP_TICKS = 3F;

    private static final double HEAD_SHARE = 0.85;

    private static final double NECK_SHARE = 0.72;

    private static final int BURST_WEB = 40;
    private static final int BURST_BLOOD = 26;

    private static final double SPURT_OUT = 0.24;
    private static final double SPURT_UP = 0.38;

    private static final BlockParticleOption WEB_SHRED =
            new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    Blocks.COBWEB.defaultBlockState());

    private static final DustParticleOptions BONE_DUST =
            new DustParticleOptions(
                    new Vector3f(0.93F, 0.91F, 0.84F), 1.1F);

    private static final ItemParticleOption BONE_SHARD =
            new ItemParticleOption(
                    ParticleTypes.ITEM, new ItemStack(
                            Items.BONE));

    private static final List<Grab> HELD = new ArrayList<>();

    private SkullCrusherSkill() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.skull_crusher";
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity target = SkillTargeting.lookTarget(player, REACH);
        if (target == null) {
            if (stand != null) {
                stand.triggerStrikeForward("grab");
            }
            Whiff.stand(player);
            return true;
        }

        if (stand != null) {
            stand.triggerGrabAt(target);
        }
        StandBeat.after(StandEntity.STRIKE_CONTACT_TICKS, player, target, SkullCrusherSkill::seize);
        return true;
    }

    private static void seize(ServerPlayer player, LivingEntity target) {
        if (!SkillTargeting.stillInReach(player, target, REACH)) {
            Whiff.stand(player);
            return;
        }
        HELD.removeIf(grab -> grab.target == target);
        HELD.add(new Grab(player, target));

        hold(target, GRAB_TICKS + 2);

        ServerLevel level = player.serverLevel();

        NetworkHandler.sendSkullFlash(level, target, player, SkullFlashPacket.WINDUP, GRAB_TICKS);

        Vec3 head = target.position().add(0, target.getBbHeight() * 0.85, 0);
        level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 8, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.6F, 1.35F);
    }

    public static boolean isGripped(LivingEntity entity) {
        for (int i = 0; i < HELD.size(); i++) {
            Grab grab = HELD.get(i);

            if (grab.target == entity && !grab.trailing) {
                return true;
            }
        }
        return false;
    }

    private static void hold(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.stun(), ticks, Stun.TIER_TWO, false, false, true));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
    }

    private static void tick() {
        HELD.removeIf(Grab::advance);
    }

    private static final class Grab {
        private final ServerPlayer holder;
        private final LivingEntity target;
        private final JojohaPlayerData data;

        private final StandEntity stand;

        private boolean crushing;
        private boolean trailing;
        private boolean burst;
        private int remaining;

        private Grab(ServerPlayer holder, LivingEntity target) {
            this.holder = holder;
            this.target = target;
            this.data = PlayerDataAccess.get(holder);
            this.stand = StandSummonHandler.findStand(holder, this.data);
            this.remaining = GRAB_TICKS;
        }

        private StandEntity stand() {
            return stand != null && stand.isAlive() ? stand : null;
        }

        private boolean advance() {
            if (trailing) {
                if (!target.isAlive() || holder.level() != target.level()) {
                    return true;
                }
                bleeding(holder.serverLevel());
                return --remaining <= 0;
            }

            if (!holder.isAlive() || !target.isAlive()
                    || holder.level() != target.level()
                    || holder.distanceTo(target) > REACH * 2.5) {
                StandEntity held = stand();
                if (held != null) {
                    held.clearWork();
                }
                return true;
            }

            hold(target, remaining + 2);
            grip();

            if (crushing) {
                cracking(holder.serverLevel());

                float elapsed = CRUSH_TICKS - remaining;
                if (!burst && elapsed >= POP_AT * CRUSH_TICKS - POP_TICKS + 1F) {
                    burst = true;
                    burst(holder.serverLevel());
                }
            } else {
                straining(holder.serverLevel());
            }

            if (--remaining > 0) {
                return false;
            }

            if (!crushing) {
                crush();
                crushing = true;
                remaining = CRUSH_TICKS;
                return false;
            }

            launch();
            trailing = true;
            remaining = TRAIL_TICKS;
            return false;
        }

        private void bleeding(ServerLevel level) {
            float left = remaining / (float) TRAIL_TICKS;
            int drops = 1 + Math.round(left * 3);

            Vec3 at = target.position().add(0, target.getBbHeight() * NECK_SHARE, 0);
            level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z, drops,
                    0.1, 0.1, 0.1, 0.06);

            if (remaining % 3 == 0) {
                level.sendParticles(WEB_SHRED, at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.05);
            }
        }

        private void grip() {
            Vec3 look = holder.getLookAngle();
            Vec3 forward = new Vec3(look.x, 0, look.z);
            forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();

            Vec3 grip = holder.position().add(forward.scale(GRIP_DISTANCE));

            double wanted = holder.getEyeY() - target.getBbHeight() * HEAD_SHARE;
            double lift = Math.min(Math.max(wanted, holder.getY()), holder.getY() + GRIP_MAX_LIFT);

            target.setPos(grip.x, lift, grip.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0F;
            target.hurtMarked = true;

            float facing = holder.getYRot() + 180F;
            target.setYRot(facing);
            target.setYBodyRot(facing);
            target.setYHeadRot(facing);

            StandEntity held = stand();
            if (held != null) {
                Vec3 side = new Vec3(-forward.z, 0, forward.x);

                held.sendToWork(holder.position()
                        .add(forward.scale(STAND_FORWARD))
                        .add(side.scale(STAND_SIDE))
                        .add(0, STAND_LIFT, 0));
            }
        }

        private void crush() {
            ServerLevel level = holder.serverLevel();

            float damage = StandPassives.scaleOutgoing(
                    holder, data, target, DAMAGE * data.stand.powerScale());
            target.hurt(level.damageSources().playerAttack(holder), DamageScale.stand(holder, target, damage));

            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;

            impact(level);

            NetworkHandler.sendSkullFlash(level, target, holder,
                    SkullFlashPacket.SHATTER, CRUSH_TICKS);

            StandEntity held = stand();
            if (held != null) {
                held.triggerPunchAt(target, true);
            }
        }

        private void launch() {
            ServerLevel level = holder.serverLevel();

            Vec3 away = target.position().subtract(holder.position());
            Vec3 heading = away.lengthSqr() < 1.0E-4 ? holder.getLookAngle() : away.normalize();
            target.setDeltaMovement(heading.x * LAUNCH_AWAY, LAUNCH_UP, heading.z * LAUNCH_AWAY);
            target.hurtMarked = true;

            target.addEffect(new MobEffectInstance(ModEffects.stun(), STUN_TICKS, Stun.TIER_TWO,
                    false, false, true));

            StandEntity held = stand();
            if (held != null) {
                held.clearWork();
            }

            launchRow(level, heading);

            Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25, 0.22);
            level.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 1.0F, 0.62F);
        }

        private void launchRow(ServerLevel level, Vec3 heading) {
            Vec3 from = target.position().add(0, target.getBbHeight() * 0.55, 0);

            for (int step = 0; step < LAUNCH_RINGS; step++) {
                Vec3 at = from.add(heading.scale((step + 1) * LAUNCH_RING_GAP));

                double scale = LAUNCH_RING_SCALE * (1.0 - step * 0.09);

                level.sendParticles(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z,
                        0, scale, RING_BAND_LARGE, 0.0, 1.0);
            }
        }

        private Vec3 head() {
            return target.position().add(0, target.getBbHeight() * HEAD_SHARE, 0);
        }

        private void straining(ServerLevel level) {
            if (remaining % 3 == 0 && remaining < GRAB_TICKS / 3) {
                Vec3 at = head();
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.18, 0.18, 0.18, 0.01);
            }
        }

        private void cracking(ServerLevel level) {
            Vec3 at = head();

            float weight = remaining / (float) CRUSH_TICKS;

            level.sendParticles(BONE_DUST, at.x, at.y, at.z, 2 + Math.round(weight * 3),
                    0.22, 0.2, 0.22, 0.02);

            if (remaining % 2 == 0) {
                level.sendParticles(BONE_SHARD, at.x, at.y, at.z, 1 + Math.round(weight * 2),
                        0.16, 0.16, 0.16, 0.14);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0.12);

                level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z, 2,
                        0.12, 0.1, 0.12, 0.08);
            }
        }

        private void burst(ServerLevel level) {
            Vec3 at = head();

            level.sendParticles(WEB_SHRED, at.x, at.y, at.z, BURST_WEB, 0.0, 0.0, 0.0, 0.55);

            for (int i = 0; i < BURST_BLOOD; i++) {
                double angle = i * (Math.PI * 2 / BURST_BLOOD) + level.random.nextDouble() * 0.5;
                double out = SPURT_OUT * (0.55 + level.random.nextDouble() * 0.9);
                double up = SPURT_UP * (0.6 + level.random.nextDouble() * 0.8);

                level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z, 0,
                        Math.cos(angle) * out, up, Math.sin(angle) * out, 1.0);
            }

            level.sendParticles(ModRegistries.BLOOD_MOTE.get(), at.x, at.y, at.z,
                    BURST_BLOOD / 3, 0.14, 0.12, 0.14, 0.02);
        }

        private void impact(ServerLevel level) {
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);

            Impacts.landed(level, target, target.position().subtract(holder.position()),
                    ImpactSpec.FINISHER, Impacts.colourOf(holder));

            level.sendParticles(BONE_SHARD, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.35);
            level.sendParticles(BONE_DUST, at.x, at.y, at.z, 34, 0.35, 0.3, 0.35, 0.04);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.3, 0.2, 0.3, 0.12);

            level.playSound(null, target.blockPosition(), SoundEvents.SKELETON_DEATH,
                    SoundSource.PLAYERS, 0.7F, 1.6F);
        }
    }
}
