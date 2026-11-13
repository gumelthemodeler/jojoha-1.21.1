package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.gumel.jojoha.stand.workshop.BarrageShout;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StandBarrageAudio {
    private static final int OVERLAP_TICKS = 8;
    private static final int FALLBACK_TICKS = 60;

    private static final Map<UUID, StandBarrageSound> PLAYING = new HashMap<>();
    private static final Map<ResourceLocation, Integer> LENGTHS = new HashMap<>();
    private static final Map<UUID, Integer> TURNS = new HashMap<>();

    private StandBarrageAudio() {
    }

    public static void clear() {
        PLAYING.clear();
        LENGTHS.clear();
        TURNS.clear();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            PLAYING.clear();
            return;
        }

        Set<UUID> barraging = null;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || !stand.isBarraging()) {
                continue;
            }

            if (barraging == null) {
                barraging = new HashSet<>();
            }
            barraging.add(stand.getUUID());

            StandBarrageSound running = PLAYING.get(stand.getUUID());
            boolean fresh = running == null || running.isStopped() || running.released();
            if (!fresh && running.age() < running.length() - OVERLAP_TICKS) {
                continue;
            }

            BarrageShout chosen = stand.barrageShout();
            int turn = TURNS.merge(stand.getUUID(), 1, Integer::sum) - 1;
            StandType voice = chosen.voices().isEmpty() ? stand.getStandType()
                    : StandTypes.byIdOrDefault(chosen.voice(turn));
            SoundEvent cry = chosen == BarrageShout.SILENT ? null
                    : stand.isKickBarrage() && chosen == BarrageShout.OWN || chosen.kick()
                    ? StandVoices.kickCry(voice)
                    : StandVoices.barrageCry(voice);
            if (cry == null) {
                continue;
            }

            if (fresh) {
                start(minecraft, stand, cry, 0);
            } else {
                running.release(OVERLAP_TICKS);
                start(minecraft, stand, cry, OVERLAP_TICKS);
            }
        }

        if (barraging == null) {
            PLAYING.clear();
            TURNS.clear();
        } else {
            PLAYING.keySet().retainAll(barraging);
            TURNS.keySet().retainAll(barraging);
        }
    }

    private static void start(Minecraft minecraft, StandEntity stand, SoundEvent cry, int rise) {
        StandBarrageSound shout = new StandBarrageSound(stand, cry, rise);
        PLAYING.put(stand.getUUID(), shout);
        minecraft.getSoundManager().play(shout);
        Sound sound = shout.getSound();
        shout.length(Math.max(OVERLAP_TICKS * 2, sound == null ? FALLBACK_TICKS : length(minecraft, sound.getPath())));
    }

    private static int length(Minecraft minecraft, ResourceLocation file) {
        return LENGTHS.computeIfAbsent(file, path -> {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(path);
            if (resource.isEmpty()) {
                return FALLBACK_TICKS;
            }
            try (InputStream in = resource.get().open()) {
                return oggTicks(in.readAllBytes());
            } catch (Exception e) {
                return FALLBACK_TICKS;
            }
        });
    }

    private static int oggTicks(byte[] bytes) {
        int rate = 0;
        long samples = 0L;
        for (int i = 0; i + 27 < bytes.length; i++) {
            if (bytes[i] == 'O' && bytes[i + 1] == 'g' && bytes[i + 2] == 'g' && bytes[i + 3] == 'S') {
                long granule = 0L;
                for (int b = 7; b >= 0; b--) {
                    granule = (granule << 8) | (bytes[i + 6 + b] & 0xFFL);
                }
                if (granule > 0L) {
                    samples = granule;
                }
            } else if (rate == 0 && bytes[i] == 1 && bytes[i + 1] == 'v' && bytes[i + 2] == 'o' && bytes[i + 3] == 'r'
                    && bytes[i + 4] == 'b' && bytes[i + 5] == 'i' && bytes[i + 6] == 's' && i + 15 < bytes.length) {
                rate = (bytes[i + 12] & 0xFF) | (bytes[i + 13] & 0xFF) << 8 | (bytes[i + 14] & 0xFF) << 16
                        | (bytes[i + 15] & 0xFF) << 24;
            }
        }
        if (rate <= 0 || samples <= 0L) {
            return FALLBACK_TICKS;
        }
        return (int) (samples * 20L / rate);
    }
}
