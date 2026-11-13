package org.gumel.jojoha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CameraBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final String CONTROLLER = "print";
    public static final String PRINT_ANIMATION = "photo_print";

    private static final RawAnimation PRINT =
            RawAnimation.begin().thenPlayAndHold(PRINT_ANIMATION);

    private static final int PRINT_TICKS = 21;

    private static final int BREAK_TICKS = 8;

    private static final double EJECT_SPEED = 0.16;

    private int printing;
    private ItemStack photo = ItemStack.EMPTY;

    public CameraBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.CAMERA_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isPrinting() {
        return printing > 0;
    }

    public void beginPrint(ItemStack picture) {
        this.photo = picture;
        this.printing = PRINT_TICKS + BREAK_TICKS;
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  CameraBlockEntity camera) {
        if (camera.printing <= 0) {
            return;
        }

        camera.printing--;
        Direction facing = state.getValue(CameraBlock.FACING);

        if (camera.printing == BREAK_TICKS) {
            ItemStack picture = camera.photo;
            camera.photo = ItemStack.EMPTY;
            eject(level, pos, facing, picture);
            return;
        }

        if (camera.printing <= 0) {
            breakCamera(level, pos);
        }
    }

    private static void eject(Level level, BlockPos pos, Direction facing, ItemStack picture) {
        Vec3 at = Vec3.atCenterOf(pos)
                .add(facing.getStepX() * 0.45, -0.15, facing.getStepZ() * 0.45);

        if (!picture.isEmpty()) {
            ItemEntity dropped = new ItemEntity(level, at.x, at.y, at.z, picture);
            dropped.setDeltaMovement(facing.getStepX() * EJECT_SPEED, 0.12,
                    facing.getStepZ() * EJECT_SPEED);
            dropped.setPickUpDelay(10);
            level.addFreshEntity(dropped);
        }

        if (level instanceof ServerLevel server) {
            server.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 14,
                    0.25, 0.25, 0.25, 0.02);
        }

        level.playSound(null, pos, ModSounds.STAND_HIT.get(), SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    private static void breakCamera(Level level, BlockPos pos) {
        level.removeBlock(pos, false);

        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atCenterOf(pos);
            server.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 18,
                    0.3, 0.3, 0.3, 0.03);
            server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.03);
        }

        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.9F, 0.8F);
    }

    public ItemStack takePending() {
        ItemStack held = photo;
        photo = ItemStack.EMPTY;
        printing = 0;
        return held;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Printing", printing);
        if (!photo.isEmpty()) {
            tag.put("Photo", photo.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        printing = tag.getInt("Printing");
        photo = tag.contains("Photo")
                ? ItemStack.parse(registries, tag.getCompound("Photo")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 0,
                state -> printing > 0 ? state.setAndContinue(PRINT) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
