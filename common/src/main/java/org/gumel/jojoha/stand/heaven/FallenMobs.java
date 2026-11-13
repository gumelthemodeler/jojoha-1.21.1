package org.gumel.jojoha.stand.heaven;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class FallenMobs {
    public static final double WATCH_RANGE = 64.0;
    public static final int KEPT = 48;
    public static final int REMEMBERED_TICKS = 6000;

    public static final String RESTORED_TAG = "jojoha.restored";
    private static final double DROPS_RADIUS = 8.0;

    private static final Deque<Fallen> FALLEN = new ArrayDeque<>();
    private static final Deque<FallenPlayer> PLAYERS = new ArrayDeque<>();

    private FallenMobs() {
    }

    static void init() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            remember(entity);
            return EventResult.pass();
        });
    }

    private static void rememberPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!watched(level, player.position())) {
            return;
        }
        boolean kept = level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        ListTag items = kept ? null : player.getInventory().save(new ListTag());
        PLAYERS.removeIf(fallen -> fallen.player.equals(player.getUUID()));
        PLAYERS.addFirst(new FallenPlayer(player.getUUID(), level.dimension(), player.position(), level.getGameTime(),
                items, kept ? -1 : player.experienceLevel, player.experienceProgress));
        while (PLAYERS.size() > KEPT) {
            PLAYERS.removeLast();
        }
    }

    public static ServerPlayer revivePlayer(ServerLevel level, Vec3 near, double radius) {
        long now = level.getGameTime();
        Iterator<FallenPlayer> iterator = PLAYERS.iterator();
        while (iterator.hasNext()) {
            FallenPlayer fallen = iterator.next();
            if (now - fallen.time > REMEMBERED_TICKS) {
                iterator.remove();
                continue;
            }
            if (fallen.level != level.dimension() || fallen.at.distanceToSqr(near) > radius * radius) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(fallen.player);
            if (player == null || !player.isAlive()) {
                continue;
            }
            iterator.remove();
            if (fallen.items != null) {
                takeUpDrops(level, fallen);
                List<ItemStack> carried = new ArrayList<>();
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (!stack.isEmpty()) {
                        carried.add(stack.copy());
                    }
                }
                player.getInventory().load(fallen.items);
                for (ItemStack stack : carried) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
            if (fallen.levels >= 0) {
                player.setExperienceLevels(fallen.levels);
                player.experienceProgress = fallen.progress;
                player.giveExperiencePoints(0);
            }
            player.teleportTo(level, fallen.at.x, fallen.at.y, fallen.at.z, player.getYRot(), player.getXRot());
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5F);
            player.clearFire();
            player.removeAllEffects();
            player.inventoryMenu.broadcastChanges();
            return player;
        }
        return null;
    }

    private static void takeUpDrops(ServerLevel level, FallenPlayer fallen) {
        AABB around = new AABB(fallen.at, fallen.at).inflate(DROPS_RADIUS);
        List<ItemEntity> lying = level.getEntitiesOfClass(ItemEntity.class, around, ItemEntity::isAlive);
        for (int i = 0; i < fallen.items.size(); i++) {
            ItemStack wanted = ItemStack.parseOptional(level.registryAccess(), fallen.items.getCompound(i));
            int left = wanted.getCount();
            for (ItemEntity drop : lying) {
                if (left <= 0) {
                    break;
                }
                ItemStack there = drop.getItem();
                if (drop.isRemoved() || !ItemStack.isSameItemSameComponents(there, wanted)) {
                    continue;
                }
                int take = Math.min(left, there.getCount());
                left -= take;
                there.shrink(take);
                if (there.isEmpty()) {
                    drop.discard();
                } else {
                    drop.setItem(there);
                }
            }
        }
        if (fallen.levels >= 0) {
            level.getEntitiesOfClass(ExperienceOrb.class, around).forEach(Entity::discard);
        }
    }

    private static void remember(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            rememberPlayer(player);
            return;
        }
        if (!(entity instanceof Mob mob) || entity instanceof StandEntity
                || !(entity.level() instanceof ServerLevel level) || !watched(level, entity.position())) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        if (!mob.save(tag)) {
            return;
        }
        FALLEN.addFirst(new Fallen(level.dimension(), entity.position(), level.getGameTime(), tag));
        while (FALLEN.size() > KEPT) {
            FALLEN.removeLast();
        }
        RealityBend.fell(level, entity, tag);
    }

    private static boolean watched(ServerLevel level, Vec3 at) {
        for (ServerPlayer player : level.players()) {
            if (Heaven.is(player) && player.position().distanceToSqr(at) <= WATCH_RANGE * WATCH_RANGE) {
                return true;
            }
        }
        return false;
    }

    public static Entity revive(ServerLevel level, Vec3 near, double radius) {
        long now = level.getGameTime();
        Iterator<Fallen> iterator = FALLEN.iterator();
        while (iterator.hasNext()) {
            Fallen fallen = iterator.next();
            if (now - fallen.time > REMEMBERED_TICKS) {
                iterator.remove();
                continue;
            }
            if (fallen.level != level.dimension() || fallen.at.distanceToSqr(near) > radius * radius) {
                continue;
            }
            Entity risen = restore(level, fallen.tag, fallen.at);
            if (risen != null) {
                iterator.remove();
                risen.addTag(RESTORED_TAG);
                return risen;
            }
        }
        return null;
    }

    public static Entity restore(ServerLevel level, CompoundTag saved, Vec3 at) {
        CompoundTag tag = saved.copy();
        tag.remove("UUID");
        tag.remove("DeathTime");
        tag.remove("HurtTime");
        tag.remove("Fire");
        Entity risen = EntityType.loadEntityRecursive(tag, level, entity -> {
            entity.moveTo(at.x, at.y, at.z, entity.getYRot(), entity.getXRot());
            return entity;
        });
        if (risen == null) {
            return null;
        }
        if (risen instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
            living.deathTime = 0;
        }
        if (!level.addFreshEntity(risen)) {
            return null;
        }
        return risen;
    }

    public static boolean restored(Entity entity) {
        return entity.getTags().contains(RESTORED_TAG);
    }

    private record Fallen(ResourceKey<Level> level, Vec3 at, long time, CompoundTag tag) {
    }

    private record FallenPlayer(UUID player, ResourceKey<Level> level, Vec3 at, long time,
                                ListTag items, int levels, float progress) {
    }
}
