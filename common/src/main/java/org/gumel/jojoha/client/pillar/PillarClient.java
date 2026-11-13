package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.PillarFxPacket;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PillarClient {
    public static final int CONTORT = 1 << PillarMen.Toggle.CONTORT.ordinal();
    public static final int CLOAK = 1 << PillarMen.Toggle.CLOAK.ordinal();
    public static final int HEAT = 1 << PillarMen.Toggle.HEAT.ordinal();
    public static final int BLADES = 1 << PillarMen.Toggle.BLADES.ordinal();
    public static final int INSIDE = 1 << PillarMen.Toggle.values().length;

    private static final Map<Integer, Integer> TOGGLES = new HashMap<>();
    private static final Map<Integer, Integer> STONE = new HashMap<>();
    private static final List<Boil> BOILS = new ArrayList<>();
    private static ClientLevel seen;

    private PillarClient() {
    }

    public static boolean on(int entityId, int toggle) {
        return (TOGGLES.getOrDefault(entityId, 0) & toggle) != 0;
    }

    public static int stone(int entityId) {
        return STONE.getOrDefault(entityId, 0);
    }

    public static boolean petrified(Player player) {
        return stone(player.getId()) >= PillarMen.PETRIFY_SECONDS;
    }

    public static boolean unseen(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player && minecraft.screen != null) {
            return false;
        }
        return on(player.getId(), INSIDE) || (on(player.getId(), CLOAK) && player.isInvisible());
    }

    public static void state(int entityId, int toggles, int stone) {
        if (toggles == 0) {
            TOGGLES.remove(entityId);
        } else {
            TOGGLES.put(entityId, toggles);
        }
        if (stone == 0) {
            STONE.remove(entityId);
        } else {
            STONE.put(entityId, stone);
        }
    }

    public static void refreshSize(UUID id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Player player = level.getPlayerByUUID(id);
        if (player != null) {
            player.refreshDimensions();
        }
    }

    public static Vec3 hand(Entity entity, float partialTick) {
        Vec3 base = entity.getPosition(partialTick);
        float yaw = entity instanceof LivingEntity living ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
                : entity.getYRot();
        double r = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(r), 0, Math.cos(r));
        Vec3 right = new Vec3(-Math.cos(r), 0, -Math.sin(r));
        double height = entity.getBbHeight() * 0.72;
        return base.add(0, height, 0).add(forward.scale(0.45)).add(right.scale(0.33));
    }

    public static Vec3 look(Entity entity, float partialTick) {
        return entity.getViewVector(partialTick);
    }

    public static void fx(PillarFxPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || packet.kind() < 0 || packet.kind() >= PillarFxKind.values().length) {
            return;
        }
        Vec3 at = new Vec3(packet.x(), packet.y(), packet.z());
        Vec3 dir = new Vec3(packet.dx(), packet.dy(), packet.dz());
        switch (PillarFxKind.values()[packet.kind()]) {
            case REACH_IN -> GoopBody.reachIn(level, packet.a(), packet.b(), packet.ticks());
            case SINK -> GoopBody.sink(level, packet.a(), packet.b(), at, packet.dx(), packet.ticks());
            case BURST -> GoopBody.burst(level, packet.a(), packet.b());
            case RIB_BLADES -> RibFx.ribs(packet.a(), packet.ticks());
            case SANDSTORM -> VortexFx.drill(at, dir, dir.length(), packet.ticks());
            case GALE -> ArcFx.gale(at, at.add(dir));
            case TORNADO -> VortexFx.tornado(packet.a(), at, packet.ticks());
            case RIFT -> ArcFx.rift(at, dir.normalize(), packet.b(), packet.ticks(), dir.length());
            case NEEDLE -> VesselFx.add(packet.a(), packet.b(), 3, packet.ticks(), 4, true, packet.ticks() % 3);
            case VESSEL_GRAB -> {
                for (int finger = 0; finger < 3; finger++) {
                    VesselFx.add(packet.a(), packet.b(), 3, packet.ticks(), 6, false, finger);
                }
            }
            case BOIL -> BOILS.add(new Boil(packet.b(), packet.ticks()));
            case SLASH -> ArcFx.slash(at, dir);
            case SAW -> ArcFx.saw(packet.b(), packet.ticks());
            case FLASH -> RibFx.flash(at);
            case CONTORT_CRACK -> crack(level, at, packet.ticks() > 0);
        }
        PillarFlair.fx(level, PillarFxKind.values()[packet.kind()], packet, at, dir);
    }

    private static void crack(ClientLevel level, Vec3 at, boolean folding) {
        for (int i = 0; i < 10; i++) {
            level.addParticle(ModRegistries.PILLAR_BONE.get(), at.x, at.y + 0.6, at.z,
                    (level.random.nextDouble() - 0.5) * 0.3, 0.15 + level.random.nextDouble() * 0.15,
                    (level.random.nextDouble() - 0.5) * 0.3);
        }
        for (int i = 0; i < 6; i++) {
            level.addParticle(ModRegistries.PILLAR_FLESH.get(), at.x, at.y + 0.5, at.z,
                    (level.random.nextDouble() - 0.5) * 0.25, 0.1 + level.random.nextDouble() * 0.1,
                    (level.random.nextDouble() - 0.5) * 0.25);
        }
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F,
                folding ? 0.6F : 0.9F, false);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8F,
                folding ? 0.5F : 0.8F, false);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != seen) {
            seen = level;
            TOGGLES.clear();
            STONE.clear();
            BOILS.clear();
            GoopBody.clear();
            VesselFx.clear();
            ArcFx.clear();
            VortexFx.clear();
            RibFx.clear();
            FlairFx.clear();
        }
        if (level == null || minecraft.isPaused()) {
            return;
        }
        GoopBody.tick(level);
        VesselFx.tick();
        ArcFx.tick(level);
        VortexFx.tick(level);
        RibFx.tick();
        FlairFx.tick(level);
        BOILS.removeIf(boil -> !boil.tick(level));
        ambient(level);
    }

    private static void ambient(ClientLevel level) {
        for (Map.Entry<Integer, Integer> entry : TOGGLES.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || entity.isInvisible()) {
                continue;
            }
            int toggles = entry.getValue();
            long time = level.getGameTime();
            if ((toggles & HEAT) != 0 && time % 3 == 0) {
                level.addParticle(ModRegistries.PILLAR_STEAM.get(),
                        entity.getX() + (level.random.nextDouble() - 0.5) * 0.6,
                        entity.getY() + 0.4 + level.random.nextDouble() * 1.3,
                        entity.getZ() + (level.random.nextDouble() - 0.5) * 0.6, 0, 0.03, 0);
            }
            if ((toggles & CLOAK) != 0 && time % 4 == 0) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                level.addParticle(ModRegistries.PILLAR_GUST.get(), entity.getX() + Math.cos(angle) * 0.7,
                        entity.getY() + 0.2 + level.random.nextDouble() * 1.4, entity.getZ() + Math.sin(angle) * 0.7,
                        -Math.sin(angle) * 0.08, 0.01, Math.cos(angle) * 0.08);
            }
            PillarFlair.ambient(level, entity, (toggles & CLOAK) != 0, (toggles & HEAT) != 0, (toggles & BLADES) != 0);
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        GoopBody.render(poseStack, buffers, camera, partialTick, level);
        VesselFx.render(poseStack, buffers, camera, partialTick, level);
        ArcFx.render(poseStack, buffers, camera, partialTick, level);
        VortexFx.render(poseStack, buffers, camera, partialTick, level);
        RibFx.render(poseStack, buffers, camera, partialTick, level);
        FlairFx.render(poseStack, buffers, camera, level);
    }

    private static final class Boil {
        final int entity;
        final int ticks;
        int age;

        Boil(int entity, int ticks) {
            this.entity = entity;
            this.ticks = ticks;
        }

        boolean tick(ClientLevel level) {
            Entity target = level.getEntity(entity);
            if (target == null || !target.isAlive() || ++age > ticks) {
                return false;
            }
            if (age % 2 == 0) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double r = target.getBbWidth() * 0.55;
                level.addParticle(ModRegistries.PILLAR_BUBBLE.get(), target.getX() + Math.cos(angle) * r,
                        target.getY() + level.random.nextDouble() * target.getBbHeight(),
                        target.getZ() + Math.sin(angle) * r, Math.cos(angle) * 0.02, 0.02, Math.sin(angle) * 0.02);
            }
            if (age % 5 == 0) {
                level.addParticle(ModRegistries.PILLAR_STEAM.get(), target.getX(), target.getY() + target.getBbHeight(),
                        target.getZ(), 0, 0.04, 0);
            }
            return true;
        }
    }
}
