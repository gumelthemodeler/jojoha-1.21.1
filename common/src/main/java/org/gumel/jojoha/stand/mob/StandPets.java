package org.gumel.jojoha.stand.mob;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StandPets extends SavedData {
    private static final String NAME = "jojoha_stand_pets";

    private static final Factory<StandPets> FACTORY = new Factory<>(StandPets::new, StandPets::load, null);

    private final Map<UUID, UUID> pets = new HashMap<>();

    public static void init() {
        EntityEvent.ANIMAL_TAME.register((animal, player) -> {
            if (!(animal instanceof TamableAnimal pet) || !(player instanceof ServerPlayer owner)
                    || !MobStands.has(pet)) {
                return EventResult.pass();
            }
            if (taken(owner.server, owner.getUUID(), pet)) {
                owner.displayClientMessage(Component.translatable("message.jojoha.stand_pet.wont_follow")
                        .withStyle(ChatFormatting.GRAY), true);
                return EventResult.interruptFalse();
            }
            of(owner.server).bind(owner.getUUID(), pet.getUUID());
            return EventResult.pass();
        });
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof TamableAnimal pet && pet.getOwnerUUID() != null
                    && entity.level() instanceof ServerLevel level && MobStands.has(pet)) {
                of(level.getServer()).free(pet.getOwnerUUID(), pet.getUUID());
            }
            return EventResult.pass();
        });
    }

    public static StandPets of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static StandPets load(CompoundTag tag, HolderLookup.Provider registries) {
        StandPets record = new StandPets();
        for (Tag entry : tag.getList("pets", Tag.TAG_COMPOUND)) {
            CompoundTag saved = (CompoundTag) entry;
            record.pets.put(saved.getUUID("owner"), saved.getUUID("pet"));
        }
        return record;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        pets.forEach((owner, pet) -> {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("owner", owner);
            saved.putUUID("pet", pet);
            list.add(saved);
        });
        tag.put("pets", list);
        return tag;
    }

    public void bind(UUID owner, UUID pet) {
        pets.put(owner, pet);
        setDirty();
    }

    public void free(UUID owner, UUID pet) {
        if (pet.equals(pets.get(owner))) {
            pets.remove(owner);
            setDirty();
        }
    }

    public static boolean taken(MinecraftServer server, UUID owner, @Nullable Mob except) {
        StandPets record = of(server);
        UUID pet = record.pets.get(owner);
        if (pet == null || except != null && pet.equals(except.getUUID())) {
            return false;
        }
        Entity found = find(server, pet);
        if (found == null) {
            return true;
        }
        if (!found.isAlive() || !(found instanceof TamableAnimal animal) || !MobStands.has(animal)
                || !owner.equals(animal.getOwnerUUID())) {
            record.free(owner, pet);
            return false;
        }
        return true;
    }

    @Nullable
    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
