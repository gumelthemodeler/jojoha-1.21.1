package org.gumel.jojoha.vampire;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.entity.RisenZombie;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VampireMinions {

    private static final ResourceLocation RAISED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "raised");

    public static final int BODY_CAP = 3;

    public static final double FUSE_RANGE = 12.0;

    public static final int CORPSE_TICKS = 600;

    private static final double CORPSE_WITNESS_RANGE = 32.0;

    public enum Tier {
        SERVANT(20.0, 4.0, 1.0, 3600),
        AMALGAM(90.0, 7.0, 1.0, 4800),
        ABOMINATION(160.0, 11.0, 1.6, 6000);

        private final double health;
        private final double damage;
        private final double scale;
        private final int lifetime;

        Tier(double health, double damage, double scale, int lifetime) {
            this.health = health;
            this.damage = damage;
            this.scale = scale;
            this.lifetime = lifetime;
        }

        public double health() {
            return health;
        }

        public double damage() {
            return damage;
        }

        public double scale() {
            return scale;
        }

        public int lifetime() {
            return lifetime;
        }

        public Tier next() {
            return switch (this) {
                case SERVANT -> AMALGAM;
                case AMALGAM -> ABOMINATION;
                case ABOMINATION -> null;
            };
        }

        public String translationKey() {
            return "minion.jojoha." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final Map<UUID, Tier> TIERS = new HashMap<>();

    private record Corpse(Vec3 at, long until) {
    }

    private static final List<Corpse> CORPSES = new ArrayList<>();

    private VampireMinions() {
    }

    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof Player || entity.level().isClientSide()) {
                return EventResult.pass();
            }

            if (TIERS.remove(entity.getUUID()) != null
                    || entity instanceof RisenZombie) {
                return EventResult.pass();
            }

            if (entity.level() instanceof ServerLevel level && witnessed(level, entity)) {
                CORPSES.add(new Corpse(entity.position(), level.getGameTime() + CORPSE_TICKS));
            }
            return EventResult.pass();
        });
    }

    private static boolean witnessed(ServerLevel level, LivingEntity fallen) {
        for (ServerPlayer player : level.players()) {
            if (PlayerDataAccess.get(player).vampireStage
                    != VampireStage.NONE
                    && player.distanceToSqr(fallen)
                            < CORPSE_WITNESS_RANGE * CORPSE_WITNESS_RANGE) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 nearestCorpse(ServerPlayer caster, double reach) {
        long now = caster.level().getGameTime();
        Vec3 best = null;
        double nearest = reach * reach;

        for (int i = CORPSES.size() - 1; i >= 0; i--) {
            Corpse corpse = CORPSES.get(i);
            if (now >= corpse.until()) {
                CORPSES.remove(i);
                continue;
            }
            double away = corpse.at().distanceToSqr(caster.position());
            if (away < nearest) {
                nearest = away;
                best = corpse.at();
            }
        }
        return best;
    }

    public static void consumeCorpse(Vec3 at) {
        CORPSES.removeIf(corpse -> corpse.at().distanceToSqr(at) < 1.0E-4);
    }

    public static List<Zombie> minionsOf(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        List<Zombie> mine = new ArrayList<>();
        for (int id : VampireState.budsOf(caster, level.getGameTime())) {
            if (level.getEntity(id) instanceof Zombie zombie && zombie.isAlive()
                    && TIERS.containsKey(zombie.getUUID())) {
                mine.add(zombie);
            }
        }
        return mine;
    }

    public static Tier tierOf(Zombie minion) {
        return TIERS.get(minion.getUUID());
    }

    public static Zombie raise(ServerPlayer caster, Vec3 at, Tier tier) {
        return raise(caster, at, tier, true);
    }

    public static Zombie raise(ServerPlayer caster, Vec3 at, Tier tier, boolean emerge) {
        ServerLevel level = caster.serverLevel();

        RisenZombie minion = tier == Tier.AMALGAM
                ? ModRegistries.AMALGAMATION.get().create(level)
                : ModRegistries.RISEN_ZOMBIE.get().create(level);
        if (minion == null) {
            return null;
        }

        minion.moveTo(at.x, at.y, at.z, caster.getYRot(), 0F);
        minion.finalizeSpawn(level, level.getCurrentDifficultyAt(minion.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);

        minion.raisedBy(caster, tier.lifetime(), emerge);
        minion.rememberTier(tier);
        minion.setPersistenceRequired();

        set(minion, Attributes.MAX_HEALTH, tier.health());
        set(minion, Attributes.ATTACK_DAMAGE, tier.damage());
        set(minion, Attributes.SCALE, tier.scale());
        minion.setHealth((float) tier.health());

        level.addFreshEntity(minion);

        TIERS.put(minion.getUUID(), tier);

        VampireState.plantBud(minion, caster, tier.lifetime());

        return minion;
    }

    public record Fused(Tier tier, Vec3 where) {
        public String translationKey() {
            return tier.translationKey();
        }
    }

    public static Zombie[] pair(ServerPlayer caster) {
        double reach = FUSE_RANGE * FUSE_RANGE;
        List<Zombie> mine = minionsOf(caster).stream()
                .filter(minion -> minion.isAlive() && minion.distanceToSqr(caster) <= reach)
                .toList();

        Zombie[] best = null;
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < mine.size(); i++) {
            for (int j = i + 1; j < mine.size(); j++) {
                Zombie a = mine.get(i);
                Zombie b = mine.get(j);
                if (resultOf(a, b) == null) {
                    continue;
                }
                double total = a.distanceToSqr(caster) + b.distanceToSqr(caster);
                if (total < nearest) {
                    nearest = total;
                    best = new Zombie[] {a, b};
                }
            }
        }
        return best;
    }

    public static Fused fuse(ServerPlayer caster, Zombie first, Zombie second, Vec3 at) {
        if (!first.isAlive() || !second.isAlive()) {
            return null;
        }
        Tier result = resultOf(first, second);
        if (result == null) {
            return null;
        }

        forget(first);
        forget(second);
        first.discard();
        second.discard();

        return raise(caster, at, result, false) == null ? null : new Fused(result, at);
    }

    private static Tier resultOf(Zombie a, Zombie b) {
        Tier first = TIERS.get(a.getUUID());
        Tier second = TIERS.get(b.getUUID());
        if (first == null || second == null) {
            return null;
        }
        Tier higher = first.ordinal() >= second.ordinal() ? first : second;
        return higher.next();
    }

    public static void forget(Zombie minion) {
        TIERS.remove(minion.getUUID());
        VampireState.dropBud(minion.getUUID());
    }

    public static void readopt(ServerPlayer owner, Zombie minion, Tier tier, int ticksLeft) {
        if (TIERS.containsKey(minion.getUUID()) && VampireState.plantedBy(minion, owner)) {
            return;
        }
        TIERS.put(minion.getUUID(), tier);
        VampireState.plantBud(minion, owner, ticksLeft);
    }

    private static void set(Zombie minion, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = minion.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(RAISED);
        instance.addPermanentModifier(
                new AttributeModifier(RAISED, value - instance.getBaseValue(),
                        AttributeModifier.Operation.ADD_VALUE));
    }

    public static int countNear(ServerPlayer caster, double radius) {
        AABB box = caster.getBoundingBox().inflate(radius);
        int found = 0;
        for (Zombie minion : minionsOf(caster)) {
            if (box.intersects(minion.getBoundingBox())) {
                found++;
            }
        }
        return found;
    }
}
