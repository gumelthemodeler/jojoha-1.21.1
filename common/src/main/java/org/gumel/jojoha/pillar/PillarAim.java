package org.gumel.jojoha.pillar;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PillarAim {
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "bosses"));

    private PillarAim() {
    }

    public static boolean boss(Entity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss || entity.getType().is(BOSSES);
    }

    public static boolean struckable(ServerPlayer player, Entity entity) {
        return entity != player && entity instanceof LivingEntity living && living.isAlive() && !entity.isSpectator()
                && !(entity instanceof ArmorStand) && !(entity instanceof StandEntity);
    }

    public static List<LivingEntity> around(ServerPlayer player, Vec3 centre, double radius) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius + 1.0), entity -> struckable(player, entity))) {
            if (candidate.getBoundingBox().getCenter().distanceTo(centre) <= radius + candidate.getBbWidth() * 0.5) {
                found.add(candidate);
            }
        }
        return found;
    }

    public static List<LivingEntity> along(ServerPlayer player, Vec3 from, Vec3 heading, double range, double radius) {
        Vec3 forward = heading.normalize();
        Vec3 to = from.add(forward.scale(range));
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(from, to).inflate(radius + 1.0), entity -> struckable(player, entity))) {
            Vec3 centre = candidate.getBoundingBox().getCenter();
            double t = Math.max(0.0, Math.min(range, centre.subtract(from).dot(forward)));
            if (from.add(forward.scale(t)).distanceTo(centre) <= radius + candidate.getBbWidth() * 0.5) {
                found.add(candidate);
            }
        }
        found.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)));
        return found;
    }

    public static List<LivingEntity> cone(ServerPlayer player, double range, double dot, int most) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), entity -> struckable(player, entity))) {
            Vec3 toward = candidate.getBoundingBox().getCenter().subtract(eye);
            if (toward.length() > range || toward.lengthSqr() < 1.0E-6
                    || toward.normalize().dot(look) < dot || !player.hasLineOfSight(candidate)) {
                continue;
            }
            found.add(candidate);
        }
        found.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        return most > 0 && found.size() > most ? new ArrayList<>(found.subList(0, most)) : found;
    }

    public static LivingEntity first(ServerPlayer player, double reach, double radius) {
        List<LivingEntity> line = along(player, player.getEyePosition(), player.getLookAngle(), reach, radius);
        for (LivingEntity candidate : line) {
            if (player.hasLineOfSight(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Vec3 hand(ServerPlayer player) {
        float yaw = player.yBodyRot * ((float) Math.PI / 180F);
        Vec3 across = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return player.getEyePosition().add(0, -0.45, 0).add(across.scale(0.35))
                .add(player.getLookAngle().scale(0.4));
    }
}
