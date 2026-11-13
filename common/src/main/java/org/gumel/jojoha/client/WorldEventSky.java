package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.worldevent.WorldEvent;

public final class WorldEventSky {
    private static final Vec3 BLOOD_SKY = new Vec3(0.30, 0.02, 0.04);
    private static final float SKY_TINT = 0.8F;
    public static final float FOG_RED = 0.32F;
    public static final float FOG_GREEN = 0.04F;
    public static final float FOG_BLUE = 0.05F;
    public static final float FOG_TINT = 0.55F;
    private static final float EASE = 0.02F;

    private static String active = "";
    private static float blood;
    private static float bloodO;

    private WorldEventSky() {
    }

    public static void set(String id) {
        active = id;
    }

    public static boolean active(WorldEvent event) {
        return event.id().equals(active);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        bloodO = blood;
        if (level == null) {
            active = "";
            blood = 0F;
            bloodO = 0F;
            return;
        }
        float target = active(WorldEvent.VAMPIRE_NIGHT) && level.dimension() == Level.OVERWORLD ? 1F : 0F;
        blood += (target - blood) * EASE;
    }

    public static float blood(ClientLevel level, float partialTick) {
        float eased = Mth.lerp(partialTick, bloodO, blood);
        if (eased <= 0.001F) {
            return 0F;
        }
        float day = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) * 2F + 0.5F, 0F, 1F);
        return eased * (1F - day);
    }

    public static Vec3 sky(ClientLevel level, Vec3 base, float partialTick) {
        float tint = blood(level, partialTick) * SKY_TINT;
        return tint <= 0.001F ? base : base.lerp(BLOOD_SKY, tint);
    }
}
