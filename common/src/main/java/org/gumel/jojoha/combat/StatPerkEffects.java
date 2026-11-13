package org.gumel.jojoha.combat;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.stand.skill.moves.DodgeSkill;
import org.gumel.jojoha.stand.skill.moves.DoubleJumpSkill;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.StandLeapSkill;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StatPerkEffects {
    private static final float IRON_KNUCKLES_BONUS = 1F;
    private static final float OVERWHELMING_FORCE_PIERCE = 0.2F;
    public static final int STAGGERING_BLOW_TICKS = 15;

    private static final float THICK_SKIN_SCALE = 0.95F;
    private static final int RECOVERY_AFTER_TICKS = 200;
    private static final int RECOVERY_EVERY_TICKS = 80;
    private static final int RECOVERY_FOOD = 7;
    private static final long LAST_STAND_COOLDOWN = 10 * 60 * 20;
    private static final long UNDYING_COOLDOWN = 5 * 60 * 20;
    private static final float LAST_STAND_HEALTH = 2F;
    private static final float UNDYING_HEALTH = 6F;
    private static final int LAST_STAND_RESIST_TICKS = 40;
    private static final int LAST_STAND_RESIST_LEVEL = 1;

    private static final float QUICK_RECOVERY_SCALE = 0.8F;
    private static final int SLIP_AWAY_TICKS = 5;

    public static final float IMMOVABLE_BREAK_SCALE = 1.5F;
    public static final float BRACED_DRAIN_SCALE = 0.8F;
    public static final float IRON_STOMACH_SCALE = 0.85F;

    private static final float FINISHER_BELOW = 0.2F;
    private static final float FINISHER_SCALE = 1.2F;
    private static final float ARMOUR_CRUSHER_PIERCE = 0.15F;
    private static final float SWIFT_STEP_SCALE = 0.8F;
    private static final int TIME_IS_SHORT_EVERY = 5;
    public static final float TIME_IS_SHORT_SCALE = 0.5F;
    private static final int TIME_IS_SHORT_LONGEST = 30 * 20;
    public static final float STEADY_FORM_REGEN_SCALE = 1.15F;
    public static final float UNWAVERING_FORM_DRAIN_SCALE = 0.85F;
    public static final float UNBREAKABLE_FORM_RESERVE = 0.10F;
    public static final float WELLSPRING_REGEN_SCALE = 1.15F;
    public static final float EFFICIENT_COST_SCALE = 0.95F;
    public static final float REBUKE_RESTORE_SCALE = 1.5F;
    public static final float BULWARK_LOCKOUT_SCALE = 0.75F;
    public static final float BOUNDLESS_RESTORE_SCALE = 1.5F;
    private static final int GUARD_CRUSHER_EVERY_TICKS = 100;

    private static final Set<ResourceLocation> AGILE_MOVES = Set.of(DodgeSkill.ID, DoubleJumpSkill.ID);
    private static final Set<ResourceLocation> SWIFT_MOVES = Set.of(StandDashSkill.ID, StandLeapSkill.ID,
            TimeSkipSkill.ID);

    private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
    private static final Map<UUID, Long> LAST_STAND_USED = new HashMap<>();
    private static final Map<UUID, Long> SLIPPING_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> STAND_MOVES_THROWN = new HashMap<>();
    private static final Map<UUID, Long> GUARD_CRUSHED = new HashMap<>();
    private static final Map<UUID, Long> CRUSHING = new HashMap<>();

    private StatPerkEffects() {
    }

    public static void init() {
        EntityEvent.LIVING_HURT.register((victim, source, amount) -> {
            if (victim instanceof ServerPlayer player) {
                LAST_HURT.put(player.getUUID(), player.level().getGameTime());
            }
            return EventResult.pass();
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer served) {
                recover(served);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            UUID id = player.getUUID();
            LAST_HURT.remove(id);
            SLIPPING_UNTIL.remove(id);
            STAND_MOVES_THROWN.remove(id);
            GUARD_CRUSHED.remove(id);
        });
    }

    public static float incoming(LivingEntity victim, DamageSource source, float amount) {
        float dealt = amount;

        if (bareHanded(source) instanceof ServerPlayer attacker
                && StatPerk.IRON_KNUCKLES.has(PlayerDataAccess.get(attacker))) {
            dealt += IRON_KNUCKLES_BONUS;
        }

        if (victim instanceof ServerPlayer player
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && StatPerk.THICK_SKIN.has(PlayerDataAccess.get(player))) {
            dealt *= THICK_SKIN_SCALE;
        }
        return dealt;
    }

    public static boolean slippedAway(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Long until = SLIPPING_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    public static void dodged(ServerPlayer player, JojohaPlayerData data) {
        if (StatPerk.SLIP_AWAY.has(data)) {
            SLIPPING_UNTIL.put(player.getUUID(), player.level().getGameTime() + SLIP_AWAY_TICKS);
        }
    }

    public static float standBlow(ServerPlayer player, JojohaPlayerData data, LivingEntity target, float amount) {
        float dealt = amount;
        if (StatPerk.FINISHER.has(data) && target.getHealth() < target.getMaxHealth() * FINISHER_BELOW) {
            dealt *= FINISHER_SCALE;
        }
        if (StatPerk.GUARD_CRUSHER.has(data) && guarding(target) && !GuardBreak.isBroken(target)) {
            long now = player.level().getGameTime();
            Long crushed = GUARD_CRUSHED.get(player.getUUID());
            if (crushed == null || now - crushed >= GUARD_CRUSHER_EVERY_TICKS || now < crushed) {
                GUARD_CRUSHED.put(player.getUUID(), now);
                GuardBreak.apply(target);
            }
        }
        if (StatPerk.ARMOUR_CRUSHER.has(data)) {
            CRUSHING.put(target.getUUID(), player.level().getGameTime());
        }
        EnergySystem.standBlowLanded(player, data);
        return dealt;
    }

    public static float armourPierce(LivingEntity victim, DamageSource source) {
        float pierce = 0F;
        if (bareHanded(source) instanceof ServerPlayer attacker
                && StatPerk.OVERWHELMING_FORCE.has(PlayerDataAccess.get(attacker))) {
            pierce = OVERWHELMING_FORCE_PIERCE;
        }
        Long marked = CRUSHING.get(victim.getUUID());
        if (marked != null && source.getEntity() instanceof ServerPlayer thrower
                && marked == thrower.level().getGameTime()
                && StatPerk.ARMOUR_CRUSHER.has(PlayerDataAccess.get(thrower))) {
            pierce = Math.max(pierce, ARMOUR_CRUSHER_PIERCE);
        }
        return pierce;
    }

    private static ServerPlayer bareHanded(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && attacker.getMainHandItem().isEmpty()
                && !PlayerDataAccess.get(attacker).lastDamageWasStandAttack) {
            return attacker;
        }
        return null;
    }

    private static boolean guarding(LivingEntity target) {
        return target.isBlocking() || target instanceof ServerPlayer player
                && PlayerDataAccess.get(player).standGuarding;
    }

    public static boolean lastStand(LivingEntity dying, DamageSource source) {
        if (!(dying instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.TOTEM_OF_UNDYING)
                || player.getItemInHand(InteractionHand.OFF_HAND).is(Items.TOTEM_OF_UNDYING)) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!StatPerk.LAST_STAND.has(data)) {
            return false;
        }
        boolean undying = StatPerk.UNDYING.has(data);
        long now = player.level().getGameTime();
        Long used = LAST_STAND_USED.get(player.getUUID());
        if (used != null && now >= used && now - used < (undying ? UNDYING_COOLDOWN : LAST_STAND_COOLDOWN)) {
            return false;
        }

        LAST_STAND_USED.put(player.getUUID(), now);
        player.setHealth(undying ? UNDYING_HEALTH : LAST_STAND_HEALTH);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, LAST_STAND_RESIST_TICKS, LAST_STAND_RESIST_LEVEL));
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(),
                player.getY() + player.getBbHeight() * 0.6, player.getZ(), 30, 0.4, 0.6, 0.4, 0.3);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        player.displayClientMessage(Component.translatable(
                undying ? "message.jojoha.perk.undying" : "message.jojoha.perk.last_stand"), true);
        return true;
    }

    private static void recover(ServerPlayer player) {
        if (player.tickCount % RECOVERY_EVERY_TICKS != 0 || player.isDeadOrDying()
                || player.getHealth() >= player.getMaxHealth()
                || player.getFoodData().getFoodLevel() < RECOVERY_FOOD) {
            return;
        }
        long now = player.level().getGameTime();
        Long hurt = LAST_HURT.get(player.getUUID());
        if (hurt != null && now >= hurt && now - hurt < RECOVERY_AFTER_TICKS) {
            return;
        }
        if (StatPerk.RECOVERY.has(PlayerDataAccess.get(player))) {
            player.heal(1F);
        }
    }

    public static float cooldownFactor(ResourceLocation move, JojohaPlayerData data) {
        float factor = 1F;
        if (AGILE_MOVES.contains(move) && StatPerk.QUICK_RECOVERY.has(data)) {
            factor *= QUICK_RECOVERY_SCALE;
        }
        if (SWIFT_MOVES.contains(move) && StatPerk.SWIFT_STEP.has(data)) {
            factor *= SWIFT_STEP_SCALE;
        }
        return factor;
    }

    public static boolean freeRecovery(ServerPlayer player, JojohaPlayerData data, int cooldownTicks,
                                       boolean lockout, boolean standMove) {
        if (!standMove || lockout || cooldownTicks > TIME_IS_SHORT_LONGEST || !StatPerk.TIME_IS_SHORT.has(data)) {
            return false;
        }
        int thrown = STAND_MOVES_THROWN.merge(player.getUUID(), 1, Integer::sum);
        if (thrown >= TIME_IS_SHORT_EVERY) {
            STAND_MOVES_THROWN.put(player.getUUID(), 0);
            return true;
        }
        return false;
    }
}
