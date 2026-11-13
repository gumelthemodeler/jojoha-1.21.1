package org.gumel.jojoha.data;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.value.IntValue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StatSources {
    private static final float STRENGTH_PER_DAMAGE = 0.5F;

    private static final float VITALITY_PER_DAMAGE = 1.2F;

    private static final int HURT_COOLDOWN_LEFT = 10;

    private static final float STRENGTH_PER_HARDNESS = 0.4F;

    private static final float STRENGTH_SHARE_WITH_STAND = 0.5F;

    private static final float POWER_PER_KILL_HEALTH = 0.2F;

    private static final float AGILITY_PER_BLOCK = 0.25F;

    private static final double MOST_BLOCKS_PER_SAMPLE = 14.0;

    private static final int ENDURANCE_SECONDS = 4;

    private static final Map<UUID, Vec3> LAST_SAMPLED =
            new HashMap<>();
    private static final Map<UUID, int[]> LOADED_SECONDS = new HashMap<>();

    private static final float WORTHINESS_PER_HEALTH = 0.25F;

    private static final int CHUNKS_PER_WORTHINESS = 3;

    private static final int DANGEROUS_GROUND_TICKS = 20 * 20;

    private static final int MOST_CHUNKS_REMEMBERED = 4096;

    private static final Map<UUID, Set<Long>> VISITED = new HashMap<>();
    private static final Map<UUID, int[]> EXPLORED = new HashMap<>();

    private static final int SAMPLE_TICKS = 20;

    private StatSources() {
    }

    public static void init() {
        EntityEvent.LIVING_HURT.register(StatSources::onHurt);
        EntityEvent.LIVING_DEATH.register(StatSources::onDeath);
        BlockEvent.BREAK.register(StatSources::onBreak);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            StatGrowth.forget(player.getUUID());
            PlayerDataAccess.forget(player.getUUID());
            RareFinds.forget(player.getUUID());
            VISITED.remove(player.getUUID());
            EXPLORED.remove(player.getUUID());
            LAST_SAMPLED.remove(player.getUUID());
            LOADED_SECONDS.remove(player.getUUID());
            StandGrowth.forget(player.getUUID());
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer served) {
                tick(served);
            }
        });
    }

    private static EventResult onHurt(LivingEntity hurt, DamageSource source, float amount) {
        if (hurt.level().isClientSide()) {
            return EventResult.pass();
        }

        if (hurt instanceof ServerPlayer victim && victim.isAlive() && !victim.isDamageSourceBlocked(source)
                && !victim.isInvulnerableTo(source) && victim.invulnerableTime <= HURT_COOLDOWN_LEFT) {
            float taken = Math.min(amount, victim.getHealth() + victim.getAbsorptionAmount());
            StatGrowth.rememberDanger(victim, taken);
            float share = Math.min(1F, taken / Math.max(1F, victim.getMaxHealth()));
            StatGrowth.award(victim, StatPoints.VITALITY,
                    Math.round(taken * VITALITY_PER_DAMAGE * share * 4F));
        }

        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && hurt.isAlive()) {
            amount = Math.min(amount, hurt.getHealth() + hurt.getAbsorptionAmount());
            JojohaPlayerData data = PlayerDataAccess.get(attacker);
            if (data.lastDamageWasStandAttack) {
                StandGrowth.dealtDamage(attacker, amount);
            } else if (fightingWithStand(data)) {
                StandGrowth.dealtDamage(attacker, amount);
                StatGrowth.award(attacker, StatPoints.STRENGTH,
                        Math.round(amount * STRENGTH_PER_DAMAGE * STRENGTH_SHARE_WITH_STAND
                                * StatGrowth.dangerScale(attacker)));
            } else {
                StatGrowth.award(attacker, StatPoints.STRENGTH,
                        Math.round(amount * STRENGTH_PER_DAMAGE * StatGrowth.dangerScale(attacker)));
            }
        }

        return EventResult.pass();
    }

    private static EventResult onDeath(LivingEntity died, DamageSource source) {
        if (died.level().isClientSide() || !(source.getEntity() instanceof ServerPlayer killer)
                || !(died instanceof Enemy)) {
            return EventResult.pass();
        }
        int worth = Math.max(1, Math.round(died.getMaxHealth() * WORTHINESS_PER_HEALTH
                * StatGrowth.dangerScale(killer)));
        StatGrowth.award(killer, StatPoints.WORTHINESS, StatGrowth.dailyLimited(killer, "valor", worth));

        if (fightingWithStand(PlayerDataAccess.get(killer))) {
            StatGrowth.awardStand(killer, StatPoints.POWER, StatGrowth.dailyLimited(killer, "stand_kills",
                    Math.max(1, Math.round(died.getMaxHealth() * POWER_PER_KILL_HEALTH
                            * StatGrowth.dangerScale(killer)))));
        }
        return EventResult.pass();
    }

    private static boolean fightingWithStand(JojohaPlayerData data) {
        return data.standSummoned && data.stand.isPresent();
    }

    private static EventResult onBreak(Level level,
                                       BlockPos pos,
                                       BlockState state,
                                       ServerPlayer player,
                                       IntValue experience) {
        if (level.isClientSide() || player == null) {
            return EventResult.pass();
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0F) {
            return EventResult.pass();
        }

        int worth = Math.max(1, Math.round(hardness * STRENGTH_PER_HARDNESS));
        StatGrowth.award(player, StatPoints.STRENGTH,
                StatGrowth.dailyLimited(player, "mining", worth));
        return EventResult.pass();
    }

    private static void tick(ServerPlayer player) {
        RareFinds.watch(player);
        if (player.tickCount % SAMPLE_TICKS != 0) {
            return;
        }

        Vec3 here = player.position();
        Vec3 before = LAST_SAMPLED.put(player.getUUID(), here);
        if (before != null && player.isSprinting() && !player.isPassenger() && !player.isFallFlying()) {
            double covered = Math.hypot(here.x - before.x, here.z - before.z);
            if (covered >= 1.0 && covered <= MOST_BLOCKS_PER_SAMPLE) {
                StatGrowth.award(player, StatPoints.AGILITY, StatGrowth.dailyLimited(player, "travel",
                        Math.max(1, Math.round((float) covered * AGILITY_PER_BLOCK))));
            }
        }

        boolean loaded = player.isSprinting() || player.isSwimming()
                || player.getFoodData().getFoodLevel() <= 6;
        if (loaded) {
            int[] seconds = LOADED_SECONDS.computeIfAbsent(player.getUUID(), id -> new int[1]);
            if (++seconds[0] >= ENDURANCE_SECONDS) {
                seconds[0] = 0;
                StatGrowth.award(player, StatPoints.ENDURANCE, StatGrowth.dailyLimited(player, "endurance", 1));
            }
        }

        Set<Long> visited = VISITED.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        long chunk = ChunkPos.asLong(player.blockPosition()) * 31L
                + player.level().dimension().location().hashCode();
        if (visited.size() < MOST_CHUNKS_REMEMBERED && visited.add(chunk)) {
            int[] explored = EXPLORED.computeIfAbsent(player.getUUID(), id -> new int[2]);
            if (++explored[0] >= CHUNKS_PER_WORTHINESS) {
                explored[0] = 0;
                StatGrowth.award(player, StatPoints.WORTHINESS, StatGrowth.dailyLimited(player, "exploring", 1));
            }
        }

        ResourceLocation biome = player.level().getBiome(player.blockPosition())
                .unwrapKey().map(ResourceKey::location).orElse(null);
        boolean dangerous = player.level().dimension() != Level.OVERWORLD
                || biome != null && biome.getNamespace().equals(Jojoha.MOD_ID);
        if (dangerous) {
            int[] explored = EXPLORED.computeIfAbsent(player.getUUID(), id -> new int[2]);
            explored[1] += SAMPLE_TICKS;
            if (explored[1] >= DANGEROUS_GROUND_TICKS) {
                explored[1] = 0;
                StatGrowth.award(player, StatPoints.WORTHINESS, StatGrowth.dailyLimited(player, "wandering", 1));
            }
        }

        RareFinds.check(player);
    }

}
