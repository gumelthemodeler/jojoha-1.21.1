package org.gumel.jojoha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

/**
 * The camera while it is developing a picture, and the clock that decides when the picture lands.
 *
 * <h2>Why the photograph waits</h2>
 *
 * <p>Because the animation is the move. The picture is already decided the instant the Stand takes
 * hold - the structure has been found, the map is built - and handing it over on that tick would
 * make the camera shaking and printing a piece of decoration playing over an outcome that already
 * happened. Holding the item until the model has finished ejecting it means the thing you watch come
 * out is the thing you get.
 *
 * <p>So the stack is stored here rather than recomputed at the end. Rolling for a structure again
 * when the animation finishes would be a second, different answer - and a search that failed the
 * second time would leave a camera that shook, printed, and gave nothing.
 *
 * <h2>Saved with the block</h2>
 *
 * <p>A print in progress survives a chunk unload or a server restart. The alternative is a player
 * losing a camera and its picture to a reload half a second long, which is the sort of thing that
 * reads as the mod eating your things.
 */
public final class CameraBlockEntity extends BlockEntity implements GeoBlockEntity {
    /** The controller GeckoLib routes triggered animations through, and the animation itself. */
    public static final String CONTROLLER = "print";
    public static final String PRINT_ANIMATION = "photo_print";

    private static final RawAnimation PRINT =
            RawAnimation.begin().thenPlayAndHold(PRINT_ANIMATION);

    /**
     * How long the print takes, in ticks.
     *
     * <p>The animation is 1.0417 seconds long and spends its last frame scaling the model's paper
     * away to nothing - so twenty-one ticks is the moment the drawn photograph disappears, which is
     * exactly when the real one should exist. A shorter wait hands you the item while a copy of it
     * is still visibly sliding out of the camera.
     */
    private static final int PRINT_TICKS = 21;

    /**
     * The beat between the picture arriving and the camera coming apart.
     *
     * <p>These used to happen on the same tick, and that is what made the move read as "the block
     * just breaks": the last thing you saw was the body vanishing, so the print never registered as
     * having finished - it registered as having been interrupted.
     *
     * <p>It also fixes a real race rather than only a feeling. The server counts these ticks from
     * the moment it triggers the animation; the client starts counting when the trigger packet
     * lands, which is at least a tick later and on a real server several. Removing the block the
     * instant the server's clock ran out took the camera away while the client still had frames of
     * it left to draw, so the animation was cut short by exactly the amount of the latency. The tail
     * is the margin that absorbs that.
     */
    private static final int BREAK_TICKS = 8;

    /** How hard the finished picture is nudged out of the slot. */
    private static final double EJECT_SPEED = 0.16;

    private int printing;
    private ItemStack photo = ItemStack.EMPTY;

    public CameraBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.CAMERA_BLOCK_ENTITY.get(), pos, state);
    }

    /** Whether this camera is already busy, so a second use cannot stack two prints on one body. */
    public boolean isPrinting() {
        return printing > 0;
    }

    /**
     * Starts the print, holding the finished picture until the model has ejected it.
     *
     * <p>The client is told by sending the block entity, not by firing a one-off animation trigger.
     * The trigger was the obvious route and it silently did nothing, for a reason worth writing
     * down: {@code setChanged} marks a block entity for saving, it does not send it anywhere. So the
     * only thing the client ever heard about a print was the trigger packet itself, and anything
     * that lost it - arriving before the block entity existed clientside, a player walking into
     * range mid-print, a relog - lost the animation with no way to recover it.
     *
     * <p>State cannot be lost the same way. {@code printing} is part of what the block entity syncs,
     * so the controller can simply ask "is this camera printing" every frame and get the right
     * answer whenever it asks - see registerControllers.
     */
    public void beginPrint(ItemStack picture) {
        this.photo = picture;
        this.printing = PRINT_TICKS + BREAK_TICKS;
        setChanged();
        sync();
    }

    /**
     * Pushes this block entity to everyone who can see it.
     *
     * <p>{@code setChanged} is about saving; this is about telling. The pair is easy to confuse
     * because the names suggest otherwise, and the failure mode is invisible on an integrated
     * server in the common case - nothing throws, the client simply never learns.
     */
    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** Everything a fresh client needs, which is the whole of it. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Runs the print out: the picture at the end of the animation, the camera a beat after that.
     *
     * <p>Two moments rather than one, and the order is the whole readability of the move. The
     * photograph arrives while the camera is still standing there, so it is plainly something the
     * camera produced; the body comes apart afterwards, so that reads as the price rather than as
     * the move being cancelled. See BREAK_TICKS.
     */
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

    /** The finished print, leaving the slot it came out of while the camera still stands. */
    private static void eject(Level level, BlockPos pos, Direction facing, ItemStack picture) {
        // A little way out along the lens, which is the face the model ejects from - see the
        // position track on the photo bone in camera.animation.json.
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

    /** And then the body, which is what the move is named after. */
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

    /**
     * What is lost if the camera is broken mid-print.
     *
     * <p>Handed back rather than deleted: the picture was already paid for, and a player who mines
     * their own camera to move it should not be punished with the loss of the answer as well.
     */
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

    /**
     * One controller, reading the camera rather than waiting to be told.
     *
     * <p>A camera that is printing plays the print; one that is not stands still. Asked every frame,
     * which means a player who arrives halfway through sees the rest of it, and a player who relogs
     * mid-print sees it resume - neither of which a one-shot trigger can do.
     *
     * <p>The client never counts {@code printing} down; it only ever learns that it is non-zero and
     * then that the block is gone. That is enough, because the animation holds on its last frame and
     * the camera is removed while it is still holding.
     */
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
