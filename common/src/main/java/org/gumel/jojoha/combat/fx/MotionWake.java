package org.gumel.jojoha.combat.fx;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MotionWake {
    public enum Style {
        DASH,
        LIFT
    }

    private static final class Wake {
        final Style style;
        final int colour;
        Vec3 last;
        int left;

        Wake(Style style, int colour, Vec3 at, int ticks) {
            this.style = style;
            this.colour = colour;
            this.last = at;
            this.left = ticks;
        }
    }

    private static final Map<UUID, Wake> WAKES = new HashMap<>();

    private static final double STILL_SQ = 0.01;

    private MotionWake() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            for (Iterator<Map.Entry<UUID, Wake>> it = WAKES.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Wake> entry = it.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null || !player.isAlive() || entry.getValue().left-- <= 0) {
                    it.remove();
                    continue;
                }
                step(player, entry.getValue());
            }
        });
    }

    public static void follow(ServerPlayer player, Style style, int colour, int ticks) {
        WAKES.put(player.getUUID(), new Wake(style, colour, centre(player), ticks));
    }

    private static void step(ServerPlayer player, Wake wake) {
        ServerLevel level = player.serverLevel();
        Vec3 now = centre(player);
        Vec3 moved = now.subtract(wake.last);

        if (moved.lengthSqr() > STILL_SQ) {
            StandFx.trail(level, wake.last, now, wake.colour);

            if (wake.style == Style.DASH) {
                if (wake.left % 2 == 0) {
                    level.sendParticles(ParticleTypes.SMALL_GUST, wake.last.x, wake.last.y,
                            wake.last.z, 1, 0.1, 0.1, 0.1, 0.0);
                }
                Vec3 back = moved.normalize().reverse();
                StandFx.kick(level, wake.last, back, wake.colour, 3, 0.18);
            } else {
                level.sendParticles(ParticleTypes.CLOUD, wake.last.x, wake.last.y - 0.8,
                        wake.last.z, 1, 0.12, 0.02, 0.12, 0.01);
            }
        }
        wake.last = now;
    }

    private static Vec3 centre(ServerPlayer player) {
        return player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
    }
}
