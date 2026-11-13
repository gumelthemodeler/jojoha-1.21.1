package org.gumel.jojoha.combat;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.ultimate.AdaptiveDefence;
import org.gumel.jojoha.ultimate.UltimateTraits;
import org.gumel.jojoha.vampire.FleshBudControl;
import org.gumel.jojoha.vampire.Headless;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireState;
import org.gumel.jojoha.vampire.moves.RegenerationMove;
import org.gumel.jojoha.vampire.moves.WallCrawlMove;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VampireTraits {
    private static final int SCORCH_TO_BURN = 3;

    private static final int SCORCH_TO_DEATH = 12;

    private static final int SCORCH_RECOVERY = 2;

    private static final Map<UUID, Integer> SCORCH = new HashMap<>();

    private static final float SCORCH_DAMAGE_BASE = 2.0F;
    private static final float SCORCH_DAMAGE_PER_SECOND = 1.6F;

    private static final int HELMET_WEAR_PER_SECOND = 1;

    private VampireTraits() {
    }

    private static final ResourceLocation PHYSIOLOGY =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "undead_physiology");

    private static final double[] STAGE_STRENGTH = {0.0, 2.0, 4.0, 5.5};
    private static final double[] STAGE_SPEED = {0.0, 0.10, 0.18, 0.28};

    private static final double[] STAGE_CLAWS = {0.0, 2.0, 3.0, 3.5};

    private static final ResourceLocation CLAWS =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_claws");

    private static final int[] STAGE_REGEN_INTERVAL = {0, 60, 40, 25};

    private static final float FALL_IGNORED = 8.0F;

    private static final float THIRST_PER_TICK = 0.005F;

    private static final float REGEN_THIRST = 3.0F;

    public static final float ULTIMATE_THIRST_SCALE = 0.08F;

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            } else if (player.level().isClientSide()) {
                WallWalk.settleCape(player);
            }
        });

        EntityEvent.LIVING_HURT.register(
                (victim, source, amount) -> {
                    if (victim instanceof ServerPlayer reformed
                            && VampireState.isReforming(reformed)
                            && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                        return EventResult.interruptFalse();
                    }
                    if (victim instanceof ServerPlayer defender && reform(defender, amount)) {
                        return EventResult.interruptFalse();
                    }
                    return EventResult.pass();
                });
    }

    private static boolean reform(ServerPlayer player, float amount) {
        if (!VampireState.isPrimedToReform(player)) {
            return false;
        }

        if (amount < player.getHealth()) {
            return false;
        }

        VampireState.spendReform(player);

        ServerLevel level = player.serverLevel();
        Vec3 was = player.position();

        double distance = RegenerationMove.REFORM_DISTANCE;
        Vec3 away = player.getLookAngle().scale(-distance);
        player.randomTeleport(was.x + away.x, was.y, was.z + away.z, true);

        player.setHealth(RegenerationMove.REFORM_HEALTH);
        player.clearFire();
        player.setDeltaMovement(Vec3.ZERO);
        VampireState.graceAfterReform(player,
                RegenerationMove.GRACE_TICKS);

        VampireFx.blood(level, was.x, was.y + 1.0, was.z, 40, 0.7, 0.35);
        VampireFx.aura(level, player.getX(), player.getY() + 1.0,
                player.getZ(), 26);
        level.playSound(null, player.blockPosition(),
                ModSounds.VAMPIRE_REANIMATE.get(),
                SoundSource.PLAYERS, 0.9F, 0.7F);
        return true;
    }

    private static void sustainedFx(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        if (VampireState.hasDrive(player) && now % HELD_FX_EVERY == 0) {
            VampireFx.aura(level, player);

            if (now % (HELD_FX_EVERY * 5L) == 0) {
                VampireFx.ring(level, player.getX(),
                        player.getY() + 1.0, player.getZ(), 1.6, 1);
            }
        }

        if (VampireState.isFrozen(player) && now % HELD_FX_EVERY == 0) {
            VampireFx.frost(level, player);
        }
    }

    private static final int HELD_FX_EVERY = 3;

    private static void crawl(ServerPlayer player, JojohaPlayerData data) {
        if (!VampireState.isCrawling(player)) {
            return;
        }

        data.specEnergy -= WallCrawlMove.DRAIN_PER_TICK;
        if (data.specEnergy <= 0F) {
            data.specEnergy = 0F;
            VampireState.setCrawling(player, false);
            PlayerDataAccess.set(player, data);
            return;
        }
        PlayerDataAccess.set(player, data);
        if (WallWalk.touching(player)) {
            player.resetFallDistance();
        }
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.vampireStage == VampireStage.NONE) {
            removePhysiology(player);
            if (player.tickCount % 20 == 0) {
                AdaptiveDefence.strip(player);
            }
            clearNightEyes(player);
            if (SCORCH.remove(player.getUUID()) != null) {
                tellScreen(player, 0);
            }
            return;
        }

        physiology(player, data.vampireStage);

        if (data.vampireStage == VampireStage.ULTIMATE_LIFEFORM) {
            UltimateTraits.tick(player, data);
        } else if (player.tickCount % 20 == 0) {
            AdaptiveDefence.strip(player);
        }

        sustainedFx(player);

        FleshBudControl.tick(player);

        crawl(player, data);

        if (VampireState.runOut(player)) {
            MovementFx.drop(player);
        }

        if (player.tickCount % 20 == 0) {
            regenerate(player, data.vampireStage);
            if (data.vampireStage == VampireStage.ULTIMATE_LIFEFORM) {
                if (SCORCH.remove(player.getUUID()) != null) {
                    tellScreen(player, 0);
                }
            } else if (data.vampireStage == VampireStage.PILLAR_MAN) {
                SCORCH.remove(player.getUUID());
            } else {
                scorch(player);
            }
            nightEyes(player);
        }
    }

    private static final int NIGHT_EYES_TICKS = 260;
    private static final int CAUGHT_LIGHT = 1;

    private static void nightEyes(ServerPlayer player) {
        if (pitchDark(player)) {
            clearNightEyes(player);
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || ours(current)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    NIGHT_EYES_TICKS, 0, true, false, false));
        }
    }

    private static void clearNightEyes(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && ours(current)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static boolean ours(MobEffectInstance effect) {
        return effect.isAmbient() && !effect.showIcon() && effect.getDuration() <= NIGHT_EYES_TICKS;
    }

    private static boolean pitchDark(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight()) {
            return false;
        }
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return level.getBrightness(LightLayer.SKY, eyes) == 0
                && level.getBrightness(LightLayer.BLOCK, eyes) < CAUGHT_LIGHT;
    }

    private static void physiology(ServerPlayer player, VampireStage stage) {
        int rank = stage.ordinal();

        apply(player, Attributes.ATTACK_DAMAGE,
                STAGE_STRENGTH[rank],
                AttributeModifier.Operation.ADD_VALUE);
        apply(player, Attributes.MOVEMENT_SPEED,
                STAGE_SPEED[rank],
                AttributeModifier.Operation
                        .ADD_MULTIPLIED_BASE);
        apply(player, Attributes.SAFE_FALL_DISTANCE,
                FALL_IGNORED,
                AttributeModifier.Operation.ADD_VALUE);

        claws(player, rank);

        player.causeFoodExhaustion(THIRST_PER_TICK);
    }

    private static void claws(ServerPlayer player, int rank) {
        boolean barehanded = player.getMainHandItem().isEmpty();
        apply(player, CLAWS, Attributes.ATTACK_DAMAGE,
                barehanded ? STAGE_CLAWS[rank] : 0.0,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void apply(ServerPlayer player,
                              Holder<Attribute> attribute,
                              double amount,
                              AttributeModifier.Operation operation) {
        apply(player, PHYSIOLOGY, attribute, amount, operation);
    }

    private static void apply(ServerPlayer player, ResourceLocation key,
                              Holder<Attribute> attribute,
                              double amount,
                              AttributeModifier.Operation operation) {
        AttributeInstance instance =
                player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing =
                instance.getModifier(key);
        if (existing != null && existing.amount() == amount) {
            return;
        }

        instance.removeModifier(key);
        if (amount != 0) {
            instance.addPermanentModifier(
                    new AttributeModifier(
                            key, amount, operation));
        }
    }

    private static void removePhysiology(ServerPlayer player) {
        for (Holder<Attribute> attribute
                : List.of(
                        Attributes.ATTACK_DAMAGE,
                        Attributes.MOVEMENT_SPEED,
                        Attributes.SAFE_FALL_DISTANCE)) {
            AttributeInstance instance =
                    player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(PHYSIOLOGY);
                instance.removeModifier(CLAWS);
            }
        }
    }

    private static void regenerate(ServerPlayer player, VampireStage stage) {
        int interval = STAGE_REGEN_INTERVAL[stage.ordinal()];
        if (interval <= 0 || player.tickCount % interval >= 20) {
            return;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        if (VampireBlood.blood(player) <= 0) {
            return;
        }
        player.heal(1.0F);
        player.causeFoodExhaustion(REGEN_THIRST);
    }

    private static void scorch(ServerPlayer player) {
        UUID id = player.getUUID();
        int exposure = SCORCH.getOrDefault(id, 0);

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean shaded = !inSunlight(player);

        if (!shaded && !helmet.isEmpty()) {
            if (helmet.isDamageableItem()) {
                helmet.hurtAndBreak(HELMET_WEAR_PER_SECOND, player, EquipmentSlot.HEAD);
            }
            shaded = true;
        }

        if (shaded) {
            exposure = Math.max(0, exposure - SCORCH_RECOVERY);
            SCORCH.put(id, exposure);
            tellScreen(player, exposure);
            return;
        }

        exposure++;
        SCORCH.put(id, exposure);
        tellScreen(player, exposure);

        if (exposure >= SCORCH_TO_DEATH) {
            player.hurt(player.serverLevel().damageSources().onFire(), Float.MAX_VALUE);
            SCORCH.remove(id);
            return;
        }

        if (exposure >= SCORCH_TO_BURN) {
            float damage = SCORCH_DAMAGE_BASE
                    + (exposure - SCORCH_TO_BURN) * SCORCH_DAMAGE_PER_SECOND;
            player.hurt(player.serverLevel().damageSources().onFire(), damage);
        }
    }

    private static void tellScreen(ServerPlayer player, int exposure) {
        NetworkHandler.sendSunScorch(player,
                Math.min(1F, (float) exposure / SCORCH_TO_DEATH));
    }

    public static boolean inSunlight(Player player) {
        if (Headless.inJar(player)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level) || !level.isDay() || level.isRaining()) {
            return false;
        }

        if (player.isInWaterOrRain() || player.isInPowderSnow) {
            return false;
        }

        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return level.canSeeSky(eyes) && level.getBrightness(LightLayer.SKY, eyes) > 8;
    }
}
