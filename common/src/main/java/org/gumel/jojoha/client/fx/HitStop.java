package org.gumel.jojoha.client.fx;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.PlayerMoveAnimation;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HitStop {
    private static final double JUDDER = 0.05;

    private static final long FLASH_MS = 2 * ImpactDirector.FRAME_MS;

    private static final Map<Integer, Hold> HOLDS = new HashMap<>();

    private record Hold(long start, int ms, Vec3 pin, Vec3 blow) {
    }

    private HitStop() {
    }

    static void begin(Entity entity, int ms, Vec3 blow) {
        if (ms <= 0) {
            return;
        }
        long now = Util.getMillis();
        Hold held = HOLDS.get(entity.getId());

        Vec3 pin = held != null && now - held.start < held.ms
                ? held.pin
                : entity.getPosition(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        HOLDS.put(entity.getId(), new Hold(now, ms, pin, blow));
    }

    private static final double STAND_REACH = 10.0;

    static void strike(Entity striker, Entity target, int ms) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ms <= 0) {
            return;
        }
        int ticks = Math.max(1, Math.round(ms / (float) ImpactDirector.FRAME_MS));
        float now = (float) minecraft.level.getGameTime();
        StandEntity.renderPartialTick =
                () -> minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        for (Entity held : new Entity[]{striker, target}) {
            if (held == null) {
                continue;
            }
            if (held instanceof StandEntity stand) {
                stand.holdAnimation(ticks);
                continue;
            }
            PlayerMoveAnimation.freeze(held.getId(), ticks, now);
            for (Entity near : minecraft.level.getEntities(held, held.getBoundingBox().inflate(STAND_REACH),
                    entity -> entity instanceof StandEntity)) {
                StandEntity stand = (StandEntity) near;
                Player owner = stand.getOwner();
                if (owner != null && owner.getId() == held.getId()) {
                    stand.holdAnimation(ticks);
                }
            }
        }
    }

    public static boolean active() {
        if (HOLDS.isEmpty()) {
            return false;
        }
        long now = Util.getMillis();
        HOLDS.values().removeIf(hold -> now - hold.start >= hold.ms);
        return !HOLDS.isEmpty();
    }

    public static Vec3 offset(Entity entity, float partialTick) {
        Hold hold = HOLDS.get(entity.getId());
        if (hold == null) {
            return null;
        }
        long elapsed = Util.getMillis() - hold.start;
        if (elapsed >= hold.ms) {
            return null;
        }

        double side = (elapsed / ImpactDirector.FRAME_MS) % 2 == 0 ? JUDDER : -JUDDER;
        Vec3 flat = new Vec3(hold.blow.x, 0, hold.blow.z);
        Vec3 judder = flat.lengthSqr() < 1.0E-6 ? new Vec3(side, 0, 0) : flat.normalize().scale(side);
        return hold.pin.subtract(entity.getPosition(partialTick)).add(judder);
    }

    public static boolean flashing(Entity entity) {
        Hold hold = HOLDS.get(entity.getId());
        return hold != null && Util.getMillis() - hold.start < Math.min(hold.ms, FLASH_MS);
    }

    static void clear() {
        HOLDS.clear();
    }

    static void prune() {
        long now = Util.getMillis();
        Iterator<Hold> it = HOLDS.values().iterator();
        while (it.hasNext()) {
            Hold hold = it.next();
            if (now - hold.start >= hold.ms) {
                it.remove();
            }
        }
    }
}
