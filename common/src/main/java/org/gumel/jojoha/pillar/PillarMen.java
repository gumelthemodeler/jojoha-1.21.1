package org.gumel.jojoha.pillar;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.VampireTraits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.moves.BrilliantBladesMove;
import org.gumel.jojoha.pillar.moves.ContortionMove;
import org.gumel.jojoha.pillar.moves.HeatBodyMove;
import org.gumel.jojoha.pillar.moves.InfiltrateMove;
import org.gumel.jojoha.pillar.moves.WindCloakMove;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.hierophant.Possession;
import org.gumel.jojoha.vampire.VampireFx;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PillarMen {
    public static final int PETRIFY_SECONDS = 10;
    public static final int PETRIFY_RECOVERY = 2;
    public static final float PETRIFIED_DAMAGE_SCALE = 1.5F;
    public static final float SUN_BURN_BASE = 1.0F;
    public static final float SUN_BURN_PER_SECOND = 0.4F;
    private static final int HELMET_WEAR_PER_SECOND = 1;

    public static final float FLESH_HEAL = 1.0F;
    public static final float ZOMBIE_ABSORB_HEAL = 2.0F;
    public static final float ZOMBIE_ABSORB_DAMAGE = 4.0F;

    public static final float HEAT_BONUS = 1.0F;
    public static final int BLADE_BLEED_EVERY = 3;
    public static final double BLADE_REACH = 0.5;
    public static final double WIND_FALL = 6.0;
    public static final double WIND_SENSE_RANGE = 12.0;
    public static final double CLOAK_TURN_DEGREES = 70.0;

    public static final int HIDE_MOB_TICKS = 15 * 20;
    public static final int HIDE_PLAYER_TICKS = 3 * 20;
    public static final int INFILTRATE_COOLDOWN = 300;
    private static final double BURST_CLEARANCE = 0.45;
    public static final int DRAIN_EVERY = 20;
    public static final float DRAIN_DAMAGE = 1.5F;
    public static final float BURST_DAMAGE = 8.0F;

    public static final int STILL_TICKS = 10;
    private static final double STILL_SPEED = 0.003;
    private static final int CLOAK_EFFECT_TICKS = 6;
    private static final double UNSEEN_RANGE = 48.0;

    public static final int MELT_EVERY = 10;
    public static final int MELT_RADIUS = 2;

    public enum Toggle {
        CONTORT,
        CLOAK,
        HEAT,
        BLADES
    }

    private static final ResourceLocation WIND_FALL_ID = id("wind_mode_fall");
    private static final ResourceLocation BLADE_REACH_ID = id("brilliant_blades_reach");
    private static final ResourceLocation CONTORT_CRAWL_ID = id("contortion_crawl");
    private static final ResourceLocation CONTORT_SPEED_ID = id("contortion_speed");

    private static final Map<UUID, Integer> STONE = new HashMap<>();
    private static final Map<UUID, Integer> BURNING = new HashMap<>();
    private static final Map<Toggle, Set<UUID>> TOGGLES = new HashMap<>();
    private static final Map<UUID, Hide> HIDING = new HashMap<>();
    private static final Map<UUID, Integer> BLADE_HITS = new HashMap<>();
    private static final Map<UUID, Integer> STILL = new HashMap<>();
    private static final Map<UUID, Swing> SWINGS = new HashMap<>();
    private static final Map<UUID, Integer> SHOWN = new HashMap<>();
    private static final int STATE_REFRESH = 40;

    private record Hide(int host, long since) {
    }

    private record Swing(int target, long at) {
    }

    static {
        for (Toggle toggle : Toggle.values()) {
            TOGGLES.put(toggle, new HashSet<>());
        }
    }

    private PillarMen() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer served) {
                tick(served);
            }
        });
        EntityEvent.LIVING_HURT.register(PillarMen::hurt);
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) -> {
            if (!(player instanceof ServerPlayer served)) {
                return EventResult.pass();
            }
            if (petrified(served)) {
                return EventResult.interruptFalse();
            }
            STILL.remove(served.getUUID());
            SWINGS.put(served.getUUID(), new Swing(target.getId(), served.level().getGameTime()));
            return EventResult.pass();
        });
        PlayerEvent.PLAYER_QUIT.register(PillarMen::forget);
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                forget(player);
            }
            return EventResult.pass();
        });
    }

    public static boolean petrified(Player player) {
        return STONE.getOrDefault(player.getUUID(), 0) >= PETRIFY_SECONDS;
    }

    public static int stone(Player player) {
        return STONE.getOrDefault(player.getUUID(), 0);
    }

    public static boolean on(Player player, Toggle toggle) {
        return TOGGLES.get(toggle).contains(player.getUUID());
    }

    public static boolean holding(Player player) {
        for (Set<UUID> set : TOGGLES.values()) {
            if (set.contains(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isContorting(Player player) {
        return on(player, Toggle.CONTORT);
    }

    public static boolean flip(ServerPlayer player, Toggle toggle) {
        boolean now = !on(player, toggle);
        set(player, toggle, now);
        return now;
    }

    public static void set(ServerPlayer player, Toggle toggle, boolean on) {
        boolean changed = on ? TOGGLES.get(toggle).add(player.getUUID()) : TOGGLES.get(toggle).remove(player.getUUID());
        if (!changed) {
            return;
        }
        if (toggle == Toggle.CONTORT) {
            NetworkHandler.sendClimbing(player, Climbing.Grip.CONTORT, on);
            player.refreshDimensions();
        }
        if (toggle == Toggle.BLADES && !on) {
            BLADE_HITS.remove(player.getUUID());
        }
        if (toggle == Toggle.CLOAK && !on) {
            STILL.remove(player.getUUID());
        }
    }

    public static boolean hiding(Player player) {
        return HIDING.containsKey(player.getUUID());
    }

    public static boolean unseen(Player player) {
        return hiding(player) || (on(player, Toggle.CLOAK) && STILL.getOrDefault(player.getUUID(), 0) >= STILL_TICKS);
    }

    private static void loseAggro(ServerPlayer player) {
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(UNSEEN_RANGE),
                mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            if (mob instanceof NeutralMob angry) {
                angry.stopBeingAngry();
            }
        }
    }

    public static void enter(ServerPlayer player, LivingEntity host) {
        HIDING.put(player.getUUID(), new Hide(host.getId(), player.level().getGameTime()));
        NetworkHandler.sendPillarFx(player.serverLevel(), PillarFxKind.SINK, player.getId(), host.getId(),
                host instanceof Player ? HIDE_PLAYER_TICKS : HIDE_MOB_TICKS, player.position(),
                new Vec3(player.yBodyRot, 0, 0));
        player.setInvisible(true);
        follow(player, host);
        loseAggro(player);
        Possession.merge(player, host, HIDE_MOB_TICKS, HIDE_PLAYER_TICKS);

        ServerLevel level = player.serverLevel();
        Vec3 at = host.getBoundingBox().getCenter();
        VampireFx.blood(level, at.x, at.y, at.z, 24, 0.35, 0.2);
        level.playSound(null, host.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, host.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    public static void burst(ServerPlayer player) {
        Hide hide = HIDING.remove(player.getUUID());
        if (hide == null) {
            return;
        }
        if (Possession.merged(player)) {
            Possession.end(player, null);
        }
        player.setInvisible(player.hasEffect(MobEffects.INVISIBILITY));
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.setMoveCooldown(InfiltrateMove.ID, player.level().getGameTime(),
                INFILTRATE_COOLDOWN);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        ServerLevel level = player.serverLevel();
        Entity found = level.getEntity(hide.host());
        Vec3 out = player.getLookAngle().multiply(1, 0, 1);
        if (found instanceof LivingEntity host) {
            float yaw = host.getYHeadRot();
            Vec3 facing = Vec3.directionFromRotation(0F, yaw);
            Vec3 behind = host.position().subtract(facing.scale(host.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + BURST_CLEARANCE));
            Vec3 exit = level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(behind))
                    ? behind : host.position();
            player.connection.teleport(exit.x, exit.y, exit.z, yaw + 180F, 0F);
            out = facing.scale(-1);
            if (host.isAlive()) {
                HitWindow.open(host);
                host.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, host, BURST_DAMAGE));
                Bleed.apply(host);
                Impacts.landed(level, host, facing.scale(-1),
                        ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
            }
            NetworkHandler.sendPillarFx(level, PillarFxKind.BURST, player.getId(), host.getId(), 0);
            Vec3 wound = host.getBoundingBox().getCenter().subtract(facing.scale(host.getBbWidth() * 0.5));
            for (int i = 0; i < 14; i++) {
                Vec3 fly = facing.scale(-0.25 - level.random.nextDouble() * 0.2)
                        .add((level.random.nextDouble() - 0.5) * 0.3, 0.15 + level.random.nextDouble() * 0.25,
                                (level.random.nextDouble() - 0.5) * 0.3);
                PillarFx.fling(level, i % 4 == 0 ? PillarFx.bone() : PillarFx.flesh(), wound, fly);
            }
        }

        Vec3 back = out.lengthSqr() > 1.0E-4 ? out.normalize().scale(0.35) : Vec3.ZERO;
        player.setDeltaMovement(back.x, 0.42, back.z);
        player.hurtMarked = true;

        Vec3 at = player.position().add(0, 1.0, 0);
        VampireFx.blood(level, at.x, at.y, at.z, 34, 0.5, 0.3);
        VampireFx.ring(level, at.x, at.y, at.z, 1.6, 1);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static void follow(ServerPlayer player, LivingEntity host) {
        player.connection.teleport(host.getX(), host.getY(), host.getZ(), player.getYRot(), player.getXRot(),
                EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
    }

    public static float adjust(LivingEntity victim, DamageSource source, float amount) {
        float dealt = amount;
        if (victim instanceof Player player && petrified(player) && !source.is(DamageTypeTags.IS_FIRE)) {
            dealt = capped(dealt, dealt * PETRIFIED_DAMAGE_SCALE);
        }
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim
                && (victim.isOnFire() || victim.hasEffect(ModEffects.burning()))
                && heatMode(PlayerDataAccess.get(attacker))) {
            dealt = capped(dealt, dealt + HEAT_BONUS);
        }
        return dealt;
    }

    private static float capped(float before, float after) {
        float cap = JojohaServerConfig.MAX_HIT.value();
        return cap > 0F ? Math.min(after, Math.max(before, cap)) : after;
    }

    public static boolean windMode(JojohaPlayerData data) {
        return Fate.pillar(data) && data.hasHamonPath(HamonPaths.WIND_MODE.id());
    }

    public static boolean heatMode(JojohaPlayerData data) {
        return Fate.pillar(data) && data.hasHamonPath(HamonPaths.HEAT_MODE.id());
    }

    public static boolean lightMode(JojohaPlayerData data) {
        return Fate.pillar(data) && data.hasHamonPath(HamonPaths.LIGHT_MODE.id());
    }

    public static void parried(ServerPlayer defender, LivingEntity attacker) {
        if (!on(defender, Toggle.BLADES) || !defender.getMainHandItem().isEmpty()) {
            return;
        }
        Bleed.apply(attacker);
        ServerLevel level = defender.serverLevel();
        Vec3 at = attacker.getBoundingBox().getCenter();
        PillarFx.spray(level, PillarFx.glint(), at, 10, 0.3, 0.25);
        level.playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    public static ProjectileDeflection cloakDeflect(ServerPlayer player, Projectile projectile) {
        if (!on(player, Toggle.CLOAK)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = projectile.position();
        PillarFx.spray(level, PillarFx.gust(), at, 3, 0.2, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_DEFLECT, SoundSource.PLAYERS, 0.8F, 1.3F);
        return (shot, hit, random) -> {
            Vec3 velocity = shot.getDeltaMovement();
            double turn = Math.toRadians(CLOAK_TURN_DEGREES) * (random.nextBoolean() ? 1 : -1);
            double cos = Math.cos(turn);
            double sin = Math.sin(turn);
            shot.setDeltaMovement(velocity.x * cos - velocity.z * sin, velocity.y, velocity.x * sin + velocity.z * cos);
        };
    }

    private static EventResult hurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide()) {
            return EventResult.pass();
        }
        if (victim instanceof ServerPlayer player) {
            if (hiding(player) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !hamon(source)) {
                return EventResult.interruptFalse();
            }
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (Fate.pillar(data)) {
                if (heatMode(data) && (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                        || source.is(DamageTypes.CAMPFIRE) || source.is(DamageTypes.HOT_FLOOR))) {
                    player.clearFire();
                    return EventResult.interruptFalse();
                }
                if (source.getDirectEntity() instanceof Zombie zombie && source.getEntity() == zombie
                        && !petrified(player)) {
                    absorb(player, zombie);
                    return EventResult.interruptFalse();
                }
                if (on(player, Toggle.HEAT) && source.getDirectEntity() instanceof LivingEntity attacker
                        && source.getEntity() == attacker && attacker != player) {
                    Burning.apply(attacker);
                    Vec3 at = attacker.getBoundingBox().getCenter();
                    PillarFx.spray(player.serverLevel(), PillarFx.bubble(), at, 6, 0.25, 0.04);
                    PillarFx.spray(player.serverLevel(), PillarFx.steam(), at, 2, 0.2, 0.03);
                }
            }
        }

        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && attacker != victim && swung(attacker, victim) && attacker.getMainHandItem().isEmpty()
                && Fate.pillar(PlayerDataAccess.get(attacker))) {
            attacker.heal(FLESH_HEAL);
            Vec3 at = victim.getBoundingBox().getCenter();
            VampireFx.blood(attacker.serverLevel(), at.x, at.y, at.z, 4, 0.15, 0.04);
            if (on(attacker, Toggle.BLADES)) {
                bladeHit(attacker, victim);
            }
        }
        return EventResult.pass();
    }

    private static boolean swung(ServerPlayer attacker, LivingEntity victim) {
        Swing swing = SWINGS.get(attacker.getUUID());
        return swing != null && swing.target() == victim.getId()
                && attacker.level().getGameTime() - swing.at() <= 1;
    }

    private static boolean hamon(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer striker
                && Fate.breathesHamon(PlayerDataAccess.get(striker));
    }

    private static void absorb(ServerPlayer player, Zombie zombie) {
        ServerLevel level = player.serverLevel();
        zombie.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, zombie, ZOMBIE_ABSORB_DAMAGE));
        player.heal(ZOMBIE_ABSORB_HEAL);
        Vec3 at = zombie.getBoundingBox().getCenter();
        VampireFx.blood(level, at.x, at.y, at.z, 14, 0.3, 0.12);
        level.playSound(null, zombie.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.9F, 0.5F);
    }

    private static void bladeHit(ServerPlayer attacker, LivingEntity victim) {
        int hits = BLADE_HITS.merge(attacker.getUUID(), 1, Integer::sum);
        ServerLevel level = attacker.serverLevel();
        Vec3 at = victim.getBoundingBox().getCenter();
        PillarFx.spray(level, PillarFx.glint(), at, 6, 0.3, 0.2);
        level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.4F);
        if (hits % BLADE_BLEED_EVERY == 0) {
            Bleed.apply(victim);
        }
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!Fate.pillar(data) || player.isSpectator()) {
            if (known(player.getUUID())) {
                forget(player);
            }
            if (player.tickCount % 20 == 0) {
                clearModifiers(player);
            }
            return;
        }

        hide(player);
        drain(player, data);

        modifier(player, Attributes.SAFE_FALL_DISTANCE, WIND_FALL_ID, WIND_FALL,
                AttributeModifier.Operation.ADD_VALUE, windMode(data));
        modifier(player, Attributes.ENTITY_INTERACTION_RANGE, BLADE_REACH_ID, BLADE_REACH,
                AttributeModifier.Operation.ADD_VALUE, on(player, Toggle.BLADES) && player.getMainHandItem().isEmpty());
        modifier(player, Attributes.SNEAKING_SPEED, CONTORT_CRAWL_ID, ContortionMove.CRAWL_BONUS,
                AttributeModifier.Operation.ADD_VALUE, on(player, Toggle.CONTORT));
        modifier(player, Attributes.MOVEMENT_SPEED, CONTORT_SPEED_ID, ContortionMove.SPEED_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on(player, Toggle.CONTORT));
        if (heatMode(data) && player.isOnFire() && !player.isInLava()) {
            player.clearFire();
        }

        if (on(player, Toggle.HEAT)) {
            heatBody(player);
        }
        if (on(player, Toggle.CLOAK)) {
            cloak(player);
        }

        if (player.tickCount % 20 == 0) {
            if (data.vampireStage == VampireStage.PILLAR_MAN) {
                petrify(player);
            } else {
                unstone(player);
            }
        }
        show(player);
    }

    public static int toggleMask(Player player) {
        int mask = 0;
        for (Toggle toggle : Toggle.values()) {
            if (on(player, toggle)) {
                mask |= 1 << toggle.ordinal();
            }
        }
        if (hiding(player)) {
            mask |= 1 << Toggle.values().length;
        }
        return mask;
    }

    private static void show(ServerPlayer player) {
        int mask = toggleMask(player);
        int stone = stone(player);
        int packed = mask | (stone << 8);
        Integer last = SHOWN.get(player.getUUID());
        boolean changed = last == null ? packed != 0 : last != packed;
        if (changed || (packed != 0 && player.tickCount % STATE_REFRESH == 0)) {
            SHOWN.put(player.getUUID(), packed);
            NetworkHandler.sendPillarState(player, mask, stone);
        }
    }

    private static boolean known(UUID id) {
        if (STONE.containsKey(id) || HIDING.containsKey(id)) {
            return true;
        }
        for (Set<UUID> set : TOGGLES.values()) {
            if (set.contains(id)) {
                return true;
            }
        }
        return false;
    }

    public static void forget(Player player) {
        if (player instanceof ServerPlayer served) {
            burst(served);
            for (Toggle toggle : Toggle.values()) {
                set(served, toggle, false);
            }
            unstone(served);
            clearModifiers(served);
        }
        UUID id = player.getUUID();
        if (SHOWN.remove(id) != null && player instanceof ServerPlayer served) {
            NetworkHandler.sendPillarState(served, 0, 0);
        }
        HIDING.remove(id);
        STONE.remove(id);
        BURNING.remove(id);
        BLADE_HITS.remove(id);
        STILL.remove(id);
        SWINGS.remove(id);
        for (Set<UUID> set : TOGGLES.values()) {
            set.remove(id);
        }
    }

    private static void clearModifiers(ServerPlayer player) {
        modifier(player, Attributes.SAFE_FALL_DISTANCE, WIND_FALL_ID, 0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(player, Attributes.ENTITY_INTERACTION_RANGE, BLADE_REACH_ID, 0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(player, Attributes.SNEAKING_SPEED, CONTORT_CRAWL_ID, 0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(player, Attributes.MOVEMENT_SPEED, CONTORT_SPEED_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
    }

    private static void drain(ServerPlayer player, JojohaPlayerData data) {
        float drain = 0F;
        if (on(player, Toggle.CONTORT)) {
            drain += ContortionMove.DRAIN_PER_TICK;
        }
        if (on(player, Toggle.CLOAK)) {
            drain += WindCloakMove.DRAIN_PER_TICK;
        }
        if (on(player, Toggle.HEAT)) {
            drain += HeatBodyMove.DRAIN_PER_TICK;
        }
        if (on(player, Toggle.BLADES)) {
            drain += BrilliantBladesMove.DRAIN_PER_TICK;
        }
        if (drain <= 0F || player.isCreative()) {
            return;
        }
        data.specEnergy -= drain;
        if (data.specEnergy <= 0F) {
            data.specEnergy = 0F;
            for (Toggle toggle : Toggle.values()) {
                set(player, toggle, false);
            }
        }
        PlayerDataAccess.set(player, data);
    }

    private static void hide(ServerPlayer player) {
        Hide hide = HIDING.get(player.getUUID());
        if (hide == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long held = level.getGameTime() - hide.since();
        Entity found = level.getEntity(hide.host());
        if (!(found instanceof LivingEntity host) || !host.isAlive() || petrified(player)
                || !Possession.merged(player)) {
            burst(player);
            return;
        }

        follow(player, host);
        player.setInvisible(true);

        if (held > 0 && held % DRAIN_EVERY == 0) {
            host.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, host, DRAIN_DAMAGE));
            player.heal(DRAIN_DAMAGE);
            level.playSound(null, host.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8F, 0.5F);
        }
        if (held % 4 == 0) {
            Vec3 at = host.getBoundingBox().getCenter();
            VampireFx.blood(level, at.x, at.y, at.z, 1, host.getBbWidth() * 0.4, 0.02);
        }
    }

    private static void heatBody(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.setTicksFrozen(0);
        if (player.tickCount % MELT_EVERY != 0) {
            return;
        }
        BlockPos centre = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-MELT_RADIUS, -1, -MELT_RADIUS),
                centre.offset(MELT_RADIUS, 2, MELT_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            BlockState melted = null;
            if (block == Blocks.ICE || block == Blocks.FROSTED_ICE) {
                melted = level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState()
                        : Blocks.WATER.defaultBlockState();
            } else if (block == Blocks.SNOW || block == Blocks.POWDER_SNOW || block == Blocks.SNOW_BLOCK) {
                melted = Blocks.AIR.defaultBlockState();
            }
            if (melted != null) {
                level.setBlockAndUpdate(pos, melted);
                PillarFx.spray(level, PillarFx.steam(), Vec3.atCenterOf(pos), 3, 0.25, 0.03);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.6F);
            }
        }
    }

    private static void cloak(ServerPlayer player) {
        UUID id = player.getUUID();
        Vec3 moving = player.getDeltaMovement();
        boolean still = moving.x * moving.x + moving.z * moving.z < STILL_SPEED && player.onGround();
        int ticks = still ? STILL.getOrDefault(id, 0) + 1 : 0;
        STILL.put(id, ticks);
        if (ticks == STILL_TICKS) {
            loseAggro(player);
        }
        if (ticks >= STILL_TICKS) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, CLOAK_EFFECT_TICKS, 0, true, false, false));
        }
    }

    private static void petrify(ServerPlayer player) {
        UUID id = player.getUUID();
        int was = STONE.getOrDefault(id, 0);
        boolean shaded = !VampireTraits.inSunlight(player);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!shaded && !helmet.isEmpty()) {
            if (helmet.isDamageableItem()) {
                helmet.hurtAndBreak(HELMET_WEAR_PER_SECOND, player, EquipmentSlot.HEAD);
            }
            shaded = true;
        }
        if (player.isCreative()) {
            shaded = true;
        }

        int stone = shaded ? Math.max(0, was - PETRIFY_RECOVERY) : Math.min(PETRIFY_SECONDS, was + 1);
        if (stone == was && stone == 0) {
            return;
        }
        if (stone == 0) {
            STONE.remove(id);
        } else {
            STONE.put(id, stone);
        }
        if (stone != was) {
            NetworkHandler.sendSunScorch(player, stone / (float) PETRIFY_SECONDS);
        }
        if (stone >= PETRIFY_SECONDS && !shaded) {
            int burning = BURNING.merge(id, 1, Integer::sum);
            player.hurt(player.serverLevel().damageSources().onFire(),
                    SUN_BURN_BASE + (burning - 1) * SUN_BURN_PER_SECOND);
        } else {
            BURNING.remove(id);
        }

        ServerLevel level = player.serverLevel();
        if (stone > was) {
            PillarFx.stone(level, player.getX(), player.getY() + 1.0, player.getZ(), 2 + stone);
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_STEP, SoundSource.PLAYERS, 0.6F, 0.6F);
        }
        if (stone >= PETRIFY_SECONDS && was < PETRIFY_SECONDS) {
            burst(player);
            player.displayClientMessage(Component.translatable("message.jojoha.pillar.turned_to_stone"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
        if (was >= PETRIFY_SECONDS && stone < PETRIFY_SECONDS) {
            player.displayClientMessage(Component.translatable("message.jojoha.pillar.stone_cracks"), true);
            PillarFx.stone(level, player.getX(), player.getY() + 1.0, player.getZ(), 24);
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    private static void unstone(ServerPlayer player) {
        BURNING.remove(player.getUUID());
        if (STONE.remove(player.getUUID()) != null) {
            NetworkHandler.sendSunScorch(player, 0F);
        }
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation key,
                                 double amount, AttributeModifier.Operation operation, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(key);
        if (!on) {
            if (existing != null) {
                instance.removeModifier(key);
            }
            return;
        }
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(key);
        instance.addTransientModifier(new AttributeModifier(key, amount, operation));
    }
}
