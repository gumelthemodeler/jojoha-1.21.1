package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BeatdownSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "beatdown");
    public static final BeatdownSkill INSTANCE = new BeatdownSkill();

    private static final double REACH = 4.5;

    private static final int SEIZE_TICKS = 8;
    private static final int HITS = 8;
    private static final int HIT_EVERY = 4;

    private static final float HIT_DAMAGE = 2.2F;
    private static final float FINAL_DAMAGE = 6.0F;

    private static final double HOLD_DISTANCE = 1.6;

    private static final double HUNG_FROM = 0.8;

    private static final double LAUNCH_AWAY = 1.2;
    private static final double LAUNCH_UP = 0.5;

    private static final int COOLDOWN_TICKS = 180;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private static final List<Hold> HOLDS = new ArrayList<>();

    private BeatdownSkill() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> HOLDS.removeIf(Hold::advance));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.beatdown";
    }

    @Override
    public ResourceLocation replaces() {
        return GrabSkill.ID;
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
        if (stand == null) {
            return false;
        }
        LivingEntity target = SkillTargeting.lookTarget(player, REACH);
        if (target == null) {
            stand.triggerStrikeForward("grab");
            Whiff.stand(player);
            return true;
        }

        stand.triggerGrabAt(target);
        StandBeat.after(StandEntity.STRIKE_CONTACT_TICKS, player, target, BeatdownSkill::seize);
        return true;
    }

    private static void seize(ServerPlayer player, LivingEntity target) {
        StandEntity stand = StandEntity.findFor(player);
        if (stand == null || !SkillTargeting.stillInReach(player, target, REACH)) {
            Whiff.stand(player);
            return;
        }
        if (target instanceof ServerPlayer defender && Defence.tryParry(defender,
                player.serverLevel().damageSources().playerAttack(player),
                Impacts.colourOf(defender))) {
            return;
        }

        HOLDS.removeIf(hold -> hold.target == target);
        HOLDS.add(new Hold(player, target));

        stand.triggerBeatdownPose();
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), ModSounds.TW_MUDADA.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, target.blockPosition(), ModSounds.STAND_HIT.get(), SoundSource.PLAYERS,
                0.6F, 1.35F);
    }

    public static boolean escape(ServerPlayer held) {
        boolean escaped = false;
        Iterator<Hold> it = HOLDS.iterator();
        while (it.hasNext()) {
            Hold hold = it.next();
            if (hold.target == held) {
                hold.release();
                it.remove();
                escaped = true;
            }
        }
        if (escaped) {
            held.removeEffect(ModEffects.stun());
        }
        return escaped;
    }

    private static final class Hold {
        private final ServerPlayer holder;
        private final LivingEntity target;
        private final StandEntity stand;
        private int tick;
        private int hitsDone;

        private Hold(ServerPlayer holder, LivingEntity target) {
            this.holder = holder;
            this.target = target;
            this.stand = StandSummonHandler.findStand(holder, PlayerDataAccess.get(holder));
        }

        private StandEntity stand() {
            return stand != null && stand.isAlive() ? stand : null;
        }

        private void release() {
            StandEntity s = stand();
            if (s != null) {
                s.clearWork();
            }
        }

        private boolean advance() {
            if (!holder.isAlive() || !target.isAlive() || holder.level() != target.level()
                    || holder.distanceTo(target) > REACH * 2.5) {
                StandEntity s = stand();
                if (s != null) {
                    s.clearWork();
                }
                return true;
            }

            pin();
            tick++;

            if (tick < SEIZE_TICKS) {
                return false;
            }

            int intoBlows = tick - SEIZE_TICKS;
            if (hitsDone < HITS) {
                if (intoBlows % HIT_EVERY == 0) {
                    if (blow(hitsDone % 2 == 1, HIT_DAMAGE, false)) {
                        release();
                        return true;
                    }
                    hitsDone++;
                }
                return false;
            }

            if (intoBlows >= HITS * HIT_EVERY + HIT_EVERY) {
                if (blow(true, FINAL_DAMAGE, true)) {
                    release();
                    return true;
                }
                launch();
                return true;
            }
            return false;
        }

        private void pin() {
            target.addEffect(new MobEffectInstance(ModEffects.stun(), 4, Stun.TIER_ONE, false, false, true));

            Vec3 look = holder.getLookAngle();
            Vec3 forward = new Vec3(look.x, 0.0, look.z);
            forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();

            StandEntity s = stand();

            Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
            if (s != null) {
                s.sendToWork(holder.position().add(forward.scale(HOLD_DISTANCE)).add(side.scale(0.9))
                        .add(0.0, 0.35, 0.0));
            }

            Vec3 at = s != null
                    ? s.leftHandAnchor()
                    : holder.position().add(forward.scale(HOLD_DISTANCE)).add(0.0, 0.35, 0.0);

            target.setPos(at.x, at.y - target.getBbHeight() * HUNG_FROM, at.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0F;
            target.hurtMarked = true;

            float facing = s != null ? s.getYRot() + 180F : holder.getYRot() + 180F;
            target.setYRot(facing);
            target.setYBodyRot(facing);
            target.setYHeadRot(facing);
        }

        private boolean blow(boolean punch2, float base, boolean last) {
            ServerLevel level = holder.serverLevel();
            JojohaPlayerData data = PlayerDataAccess.get(holder);
            float damage = StandPassives.scaleOutgoing(holder, data, target,
                    StandTuning.damage(last ? "beatdown_final" : "beatdown_hit", base)
                            * data.stand.powerScale());

            HitWindow.open(target);
            boolean parryable = target instanceof ServerPlayer defender
                    && Defence.inParryWindow(defender);
            boolean landed = target.hurt(level.damageSources().playerAttack(holder), DamageScale.stand(holder, target, damage));
            if (!landed && parryable) {
                return true;
            }
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;

            if (last) {
                Bleed.apply(target);
            }

            StandEntity s = stand();
            if (s != null) {
                s.triggerPunchAt(target, punch2);
            }

            Impacts.landed(level, target,
                    target.position().subtract(holder.position()),
                    last ? ImpactSpec.HEAVY
                            : ImpactSpec.MEDIUM,
                    Impacts.colourOf(holder));

            data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
            PlayerDataAccess.set(holder, data);
            return false;
        }

        private void launch() {
            ServerLevel level = holder.serverLevel();
            Vec3 away = target.position().subtract(holder.position());
            Vec3 heading = away.lengthSqr() < 1.0E-4 ? holder.getLookAngle() : away.normalize();
            target.setDeltaMovement(heading.x * LAUNCH_AWAY, LAUNCH_UP, heading.z * LAUNCH_AWAY);
            target.hurtMarked = true;
            level.playSound(null, target.blockPosition(), ModSounds.TW_DONUT_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.25, 0.25, 0.25, 0.2);

            StandEntity s = stand();
            if (s != null) {
                s.clearWork();
            }
        }
    }
}
