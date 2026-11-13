package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.TimeStopLine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TimeStopChargers {
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");

    private record Charging(long began, int max) {
    }

    private static final Map<Integer, Charging> CHARGING = new HashMap<>();

    private static final Map<Integer, Long> BURSTS = new HashMap<>();

    private static final float GLOW_MAX = 0.9F;
    private static final float GLOW_RISE_TICKS = 4F;
    private static final int BURST_TICKS = 8;
    private static final int BURST_SLACK_TICKS = 4;
    private static final int BURST_SPARKS = 28;
    private static final double BURST_SPEED = 0.35;
    private static final double STAND_SEARCH = 8.0;

    private static final double FELT_RANGE = 24.0;
    private static final int HEARTBEAT_TICKS = 16;
    private static final int MOTE_EVERY = 3;
    private static final int BUILD_STEPS = 3;
    private static final int PULSE_TICKS = 10;
    private static final float PULSE_DEPTH = 0.55F;
    private static final float STEADY_DEPTH = 0.3F;

    private static long pulseAt = Long.MIN_VALUE / 2;
    private static float pulseStrength;
    private static float steady;
    private static float steadyLast;

    private TimeStopChargers() {
    }

    public static void heard(int entityId, int max) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (max > 0) {
            CHARGING.put(entityId, new Charging(now, max));
            BURSTS.remove(entityId);
            return;
        }
        Charging was = CHARGING.remove(entityId);
        if (was != null && now - was.began() >= was.max() - BURST_SLACK_TICKS) {
            BURSTS.put(entityId, now);
            sparks(minecraft.level, entityId);
        }
    }

    public static float glow(int ownerId, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Charging charging = CHARGING.get(ownerId);
        if (charging == null || minecraft.level == null) {
            return 0F;
        }
        float elapsed = minecraft.level.getGameTime() - charging.began() + partialTick;
        int steps = Math.max(1, Mth.ceil(charging.max() / TimeStopLine.CHARGE_CYCLE_TICKS));
        int step = Math.min(steps - 1, (int) (elapsed / TimeStopLine.CHARGE_CYCLE_TICKS));
        float into = elapsed - step * TimeStopLine.CHARGE_CYCLE_TICKS;
        float rise = Mth.clamp(into / GLOW_RISE_TICKS, 0F, 1F);
        rise = rise * rise * (3F - 2F * rise);
        return Mth.clamp((step + rise) / steps, 0F, 1F) * GLOW_MAX;
    }

    public static float burst(int ownerId, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Long at = BURSTS.get(ownerId);
        if (at == null || minecraft.level == null) {
            return 0F;
        }
        float age = minecraft.level.getGameTime() - at + partialTick;
        return age >= BURST_TICKS ? 0F : 1F - Mth.clamp(age / BURST_TICKS, 0F, 1F);
    }

    private static void sparks(ClientLevel level, int ownerId) {
        Entity owner = level.getEntity(ownerId);
        if (owner == null) {
            return;
        }
        for (StandEntity stand : level.getEntitiesOfClass(StandEntity.class, owner.getBoundingBox().inflate(STAND_SEARCH),
                candidate -> candidate.getOwner() == owner)) {
            double middle = stand.getY() + stand.getBbHeight() * 0.5;
            for (int i = 0; i < BURST_SPARKS; i++) {
                double yaw = level.random.nextDouble() * Mth.TWO_PI;
                double pitch = (level.random.nextDouble() - 0.5) * Math.PI;
                double flat = Math.cos(pitch);
                level.addParticle(ParticleTypes.END_ROD, stand.getX(), middle, stand.getZ(),
                        Math.cos(yaw) * flat * BURST_SPEED, Math.sin(pitch) * BURST_SPEED,
                        Math.sin(yaw) * flat * BURST_SPEED);
            }
        }
    }

    public static void clear() {
        CHARGING.clear();
        BURSTS.clear();
        steady = 0F;
        steadyLast = 0F;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        steadyLast = steady;
        if (level == null || minecraft.player == null) {
            CHARGING.clear();
            BURSTS.clear();
            steady = 0F;
            return;
        }
        long now = level.getGameTime();
        BURSTS.values().removeIf(at -> now - at > BURST_TICKS);
        float felt = 0F;
        Iterator<Map.Entry<Integer, Charging>> each = CHARGING.entrySet().iterator();
        while (each.hasNext()) {
            Map.Entry<Integer, Charging> entry = each.next();
            Entity charger = level.getEntity(entry.getKey());
            int max = entry.getValue().max();
            int elapsed = (int) (now - entry.getValue().began());
            if (charger == null || !charger.isAlive() || elapsed > max + 40) {
                each.remove();
                continue;
            }
            boolean near = charger.distanceTo(minecraft.player) <= FELT_RANGE;
            int build = Math.min(BUILD_STEPS, elapsed * BUILD_STEPS / Math.max(1, max));
            if (elapsed == max && near) {
                pulse(now, 1F);
            }
            if (elapsed > 0 && elapsed % HEARTBEAT_TICKS == 0) {
                level.playLocalSound(charger.getX(), charger.getY(), charger.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.PLAYERS, 0.35F + build * 0.1F, 0.9F + build * 0.05F, false);
            }
            if (build >= 1 && elapsed % MOTE_EVERY == 0) {
                for (int mote = 0; mote < build; mote++) {
                    double angle = level.random.nextDouble() * Mth.TWO_PI;
                    level.addParticle(ModRegistries.TIMESTOP_MOTE.get(),
                            charger.getX() + Math.cos(angle), charger.getY() + 0.6 + level.random.nextDouble() * 1.2,
                            charger.getZ() + Math.sin(angle), 0, 0, 0);
                }
            }
            if (near) {
                felt = Math.max(felt, Mth.clamp(elapsed / (float) max, 0F, 1F));
            }
        }
        steady = felt;
    }

    private static void pulse(long now, float strength) {
        pulseAt = now;
        pulseStrength = strength;
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float age = minecraft.level.getGameTime() - pulseAt + partialTick;
        float pulse = age < PULSE_TICKS ? (1F - age / PULSE_TICKS) * pulseStrength * PULSE_DEPTH : 0F;
        float depth = Mth.clamp(Mth.lerp(partialTick, steadyLast, steady) * STEADY_DEPTH + pulse, 0F, 1F);
        if (depth <= 0.001F) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        graphics.setColor(depth, depth, depth * 0.8F, 1F);
        graphics.blit(VIGNETTE, 0, 0, -90, 0F, 0F, graphics.guiWidth(), graphics.guiHeight(),
                graphics.guiWidth(), graphics.guiHeight());
        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
