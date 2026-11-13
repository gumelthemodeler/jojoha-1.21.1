package org.gumel.jojoha.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayList;
import java.util.List;

public final class StandDiscs {
    private static final String SKIN_OF = "skin_of";
    private static final String STAND_ID = "stand";
    private static final String SKIN = "skin";
    private static final String OWNER = "owner";

    private static final String STAND = "stand";
    private static final String NODES = "nodes";
    private static final String BAR = "bar";
    private static final String UTILITY_BAR = "utility_bar";

    private StandDiscs() {
    }

    public record Skin(ResourceLocation standId, int skin) {
    }

    public static ItemStack copySkin(ServerPlayer player, ItemStack blank) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            player.displayClientMessage(Component.translatable("message.jojoha.memory_disc.no_stand"), true);
            return ItemStack.EMPTY;
        }
        if (data.standArrowRitualTicks > 0 || data.standSkinSwapTicks > 0) {
            return ItemStack.EMPTY;
        }
        StandData stand = data.stand;
        if (stand.skin() == 0) {
            player.displayClientMessage(Component.translatable("message.jojoha.memory_disc.default_skin"), true);
            return ItemStack.EMPTY;
        }

        CompoundTag skin = new CompoundTag();
        skin.putString(STAND_ID, stand.standId().toString());
        skin.putInt(SKIN, stand.skin());
        CompoundTag tag = new CompoundTag();
        tag.put(SKIN_OF, skin);
        tag.putString(OWNER, player.getGameProfile().getName());

        ItemStack disc = new ItemStack(ModItems.STAND_DISC.get());
        disc.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(Component.translatable("message.jojoha.memory_disc.copied",
                skinName(stand.standId(), stand.skin())), true);
        return disc;
    }

    public static ItemStack commit(ServerPlayer player, ItemStack disc) {
        if (standOn(disc) != null) {
            return restoreStand(player, disc);
        }
        Skin skin = skinOn(disc);
        if (skin == null) {
            player.displayClientMessage(Component.translatable("message.jojoha.stand_disc.blank"), true);
            return ItemStack.EMPTY;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.hasCommittedSkin(skin.standId(), skin.skin())) {
            player.displayClientMessage(Component.translatable("message.jojoha.stand_disc.known",
                    skinName(skin.standId(), skin.skin())), true);
            return ItemStack.EMPTY;
        }

        data.commitSkin(skin.standId(), skin.skin());
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(Component.translatable("message.jojoha.stand_disc.committed",
                skinName(skin.standId(), skin.skin())), true);
        return new ItemStack(ModItems.MEMORY_DISC.get());
    }

    private static ItemStack restoreStand(ServerPlayer player, ItemStack disc) {
        StandData stand = standOn(disc);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.stand.isPresent()) {
            player.displayClientMessage(Component.translatable("message.jojoha.stand_disc.has_stand"), true);
            return ItemStack.EMPTY;
        }
        if (Fate.barredFromStands(data)) {
            player.displayClientMessage(Component.translatable("message.jojoha.ultimate.no_stand"), true);
            return ItemStack.EMPTY;
        }

        CompoundTag tag = disc.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.stand = stand;
        data.setChosenPassives(List.copyOf(stand.equippedPassives()));
        data.getUnlockedNodes().removeAll(SkillTrees.allStandNodeIds());
        for (String id : read(tag, NODES)) {
            ResourceLocation node = ResourceLocation.tryParse(id);
            if (node != null) {
                data.grantNode(node);
            }
        }
        restoreBar(read(tag, BAR), data);
        restoreBar(read(tag, UTILITY_BAR), data);
        data.shareEndurance(false);

        boolean committed = data.commitSkin(stand.standId(), stand.skin());
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(committed
                ? Component.translatable("message.jojoha.stand_disc.restored_committed",
                        skinName(stand.standId(), stand.skin()))
                : Component.translatable("message.jojoha.stand_disc.restored"), true);
        return new ItemStack(ModItems.MEMORY_DISC.get());
    }

    public static Skin skinOn(ItemStack disc) {
        StandData stand = standOn(disc);
        if (stand != null) {
            return new Skin(stand.standId(), stand.skin());
        }
        CustomData custom = disc.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(SKIN_OF, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag skin = tag.getCompound(SKIN_OF);
        ResourceLocation standId = ResourceLocation.tryParse(skin.getString(STAND_ID));
        return standId == null ? null : new Skin(standId, skin.getInt(SKIN));
    }

    public static StandData standOn(ItemStack disc) {
        CustomData custom = disc.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(STAND)) {
            return null;
        }
        StandData stand = StandData.CODEC.parse(NbtOps.INSTANCE, tag.get(STAND)).result().orElse(null);
        return stand != null && stand.isPresent() ? stand : null;
    }

    public static String ownerOf(ItemStack disc) {
        CustomData custom = disc.get(DataComponents.CUSTOM_DATA);
        return custom == null ? "" : custom.copyTag().getString(OWNER);
    }

    public static Component skinName(ResourceLocation standId, int skin) {
        StandType type = StandTypes.byIdOrDefault(standId);
        return type.skinName(skin);
    }

    private static List<String> read(CompoundTag tag, String key) {
        List<String> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    private static void restoreBar(List<String> saved, JojohaPlayerData data) {
        for (int slot = 0; slot < saved.size(); slot++) {
            String id = saved.get(slot);
            if (!id.isEmpty()) {
                data.placeOnBar(ResourceLocation.tryParse(id), slot);
            }
        }
    }
}
