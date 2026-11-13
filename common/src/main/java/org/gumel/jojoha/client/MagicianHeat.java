package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

public final class MagicianHeat {
    private static final int SCAN_EVERY = 10;
    private static final int SCAN_RADIUS = 10;
    private static final int SCAN_HEIGHT = 5;
    private static final int MOST_TRACKED = 64;
    private static final float FLAMES_FOR_FULL = 10F;
    private static final double ZONE_MARGIN = 10.0;
    private static final float EASE = 0.06F;
    private static final int SHIMMERS_PER_TICK = 2;

    public static final float FOG_RED = 0.78F;
    public static final float FOG_GREEN = 0.24F;
    public static final float FOG_BLUE = 0.08F;
    public static final float FOG_TINT = 0.6F;
    public static final float FOG_PULL = 0.35F;

    private static final List<BlockPos> FLAMES = new ArrayList<>();
    private static float target;
    private static float heat;
    private static float heatO;

    private MagicianHeat() {
    }

    public static float heat(float partialTick) {
        return Mth.lerp(partialTick, heatO, heat);
    }

    public static void clear() {
        FLAMES.clear();
        target = 0F;
        heat = 0F;
        heatO = 0F;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            return;
        }
        Vec3 eye = mc.player.getEyePosition();
        if (level.getGameTime() % SCAN_EVERY == 0) {
            scan(level, mc.player.blockPosition(), eye);
        }
        heatO = heat;
        heat += (target - heat) * EASE;
        if (FLAMES.isEmpty() || mc.isPaused()) {
            return;
        }
        for (int i = 0; i < SHIMMERS_PER_TICK; i++) {
            BlockPos at = FLAMES.get(level.random.nextInt(FLAMES.size()));
            level.addParticle(ModRegistries.HEAT_SHIMMER.get(),
                    at.getX() + 0.15 + level.random.nextDouble() * 0.7, at.getY() + 0.8 + level.random.nextDouble() * 0.4,
                    at.getZ() + 0.15 + level.random.nextDouble() * 0.7, 0.0, 0.0, 0.0);
        }
    }

    private static void scan(ClientLevel level, BlockPos centre, Vec3 eye) {
        FLAMES.clear();
        double nearest = Double.MAX_VALUE;
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-SCAN_RADIUS, -SCAN_HEIGHT, -SCAN_RADIUS),
                centre.offset(SCAN_RADIUS, SCAN_HEIGHT, SCAN_RADIUS))) {
            if (!level.getBlockState(at).is(ModRegistries.MAGICIAN_FLAME.get())) {
                continue;
            }
            nearest = Math.min(nearest, at.distToCenterSqr(eye));
            if (FLAMES.size() < MOST_TRACKED) {
                FLAMES.add(at.immutable());
            }
        }
        float flames = 0F;
        if (!FLAMES.isEmpty()) {
            float near = (float) Mth.clamp(1.0 - Math.sqrt(nearest) / SCAN_RADIUS, 0.0, 1.0);
            flames = Math.min(1F, FLAMES.size() / FLAMES_FOR_FULL) * (0.4F + 0.6F * near);
        }
        target = Math.max(flames, MagicianFx.zoneHeat(eye, ZONE_MARGIN));
    }
}
