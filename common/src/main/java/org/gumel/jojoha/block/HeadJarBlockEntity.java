package org.gumel.jojoha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.vampire.Headless;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class HeadJarBlockEntity extends BlockEntity {
    private static final int CHECK_EVERY = 20;

    @Nullable
    private UUID occupant;
    private String name = "";

    public HeadJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.HEAD_JAR_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public UUID occupant() {
        return occupant;
    }

    public String name() {
        return name;
    }

    public void fill(UUID head, String headName) {
        occupant = head;
        name = headName;
        changed();
    }

    public void empty() {
        if (occupant == null) {
            return;
        }
        occupant = null;
        name = "";
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState shown = getBlockState().setValue(HeadJarBlock.FULL, occupant != null);
            level.setBlock(worldPosition, shown, Block.UPDATE_ALL);
            level.sendBlockUpdated(worldPosition, shown, shown, Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeadJarBlockEntity box) {
        if (box.occupant != null && level.getGameTime() % CHECK_EVERY == 0
                && !Headless.placedAt(box.occupant, level, pos)) {
            box.empty();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (occupant != null) {
            tag.putUUID("head", occupant);
            tag.putString("name", name);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        occupant = tag.hasUUID("head") ? tag.getUUID("head") : null;
        name = tag.getString("name");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
