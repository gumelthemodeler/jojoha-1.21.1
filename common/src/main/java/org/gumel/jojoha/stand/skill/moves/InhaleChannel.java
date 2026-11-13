package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class InhaleChannel {
    private static final int PARTICLE_INTERVAL_TICKS = 5;

    static final double RADIUS = 14.0;
    private static final double CONE_COS = 0.5;

    private static final int RAMP_TICKS = 30;
    private static final double DRAW_SPEED_START = 0.05;
    private static final double DRAW_SPEED_FULL = 0.32;

    private static final double PULL_STRENGTH = 0.11;
    private static final double BLOW_STRENGTH = 0.16;

    private static final double HOLD_DISTANCE = 1.6;

    static final int MAX_HOLD_TICKS = 160;

    static final float DRAIN_PER_TICK = 0.5F;

    private static final int HOLD_OVERRUN_TICKS = 12;

    private static final double BLOCK_RADIUS = 7.0;
    private static final int BLOCK_INTERVAL_TICKS = 4;
    private static final int MAX_BLOCK_CHANGES = 16;

    private static final List<Channel> ACTIVE = new ArrayList<>();

    private InhaleChannel() {
    }

    static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    static void begin(ServerPlayer player, StandEntity stand, boolean blowing) {
        ACTIVE.removeIf(channel -> channel.player == player);
        ACTIVE.add(new Channel(player, stand, blowing));
        stand.beginInhale(player, HOLD_OVERRUN_TICKS);
    }

    static boolean isActive(ServerPlayer player) {
        for (Channel channel : ACTIVE) {
            if (channel.player == player) {
                return true;
            }
        }
        return false;
    }

    static void end(ServerPlayer player) {
        Iterator<Channel> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Channel channel = iterator.next();
            if (channel.player == player) {
                iterator.remove();
                channel.finish();
            }
        }
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }

        List<Channel> done = null;
        for (Channel channel : ACTIVE) {
            if (channel.step()) {
                if (done == null) {
                    done = new ArrayList<>();
                }
                done.add(channel);
            }
        }
        if (done != null) {
            ACTIVE.removeAll(done);
            for (Channel channel : done) {
                channel.finish();
            }
        }
    }

    private static final class Channel {
        private final ServerPlayer player;
        private final StandEntity stand;
        private final boolean blowing;

        private int elapsed;

        private Channel(ServerPlayer player, StandEntity stand, boolean blowing) {
            this.player = player;
            this.stand = stand;
            this.blowing = blowing;
        }

        private Vec3 focus() {
            return stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        }

        private boolean step() {
            if (!player.isAlive() || !stand.isAlive() || stand.isRemoved() || stand.isDismissing()
                    || stand.getOwner() != player || ++elapsed > MAX_HOLD_TICKS) {
                return true;
            }

            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (!data.standSummoned || data.standEnergy <= 0F) {
                return true;
            }
            data.standEnergy = Math.max(0F, data.standEnergy - DRAIN_PER_TICK);
            PlayerDataAccess.set(player, data);

            stand.holdInhale(player, HOLD_OVERRUN_TICKS);

            ServerLevel level = player.serverLevel();
            Vec3 axis = player.getLookAngle();
            Vec3 eye = player.getEyePosition();
            Vec3 focus = focus();

            if (elapsed % PARTICLE_INTERVAL_TICKS == 1) {
                InhaleSkill.spawnWindRow(level, focus, axis, blowing);
                InhaleSkill.spawnSmoke(level, focus, axis, blowing);
            }

            double ramp = Math.min(1.0, elapsed / (double) RAMP_TICKS);
            ramp = ramp * ramp * (3.0 - 2.0 * ramp);
            double drawSpeed = DRAW_SPEED_START + (DRAW_SPEED_FULL - DRAW_SPEED_START) * ramp;
            double strength = blowing ? BLOW_STRENGTH : PULL_STRENGTH;

            player.clearFire();

            AABB area = new AABB(focus, focus).inflate(RADIUS);
            for (Entity entity : level.getEntities(player, area,
                    candidate -> candidate.isAlive() && !(candidate instanceof StandEntity))) {

                if (!inCone(eye, axis, entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                    continue;
                }

                Vec3 toFocus = focus.subtract(entity.position());
                double distance = toFocus.length();
                if (distance > RADIUS || distance < 1.0E-4) {
                    continue;
                }

                entity.clearFire();

                if (!blowing && distance < HOLD_DISTANCE) {
                    continue;
                }

                Vec3 heading = toFocus.scale((blowing ? -1.0 : 1.0) / distance);

                if (entity.getDeltaMovement().dot(heading) >= drawSpeed) {
                    continue;
                }

                double falloff = Math.max(0.3, 1.0 - distance / RADIUS);
                Vec3 pull = heading.scale(strength * falloff * ramp);
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull.x, pull.y * 0.5 + 0.02, pull.z));
                entity.hurtMarked = true;
                entity.fallDistance = 0F;
            }

            if (elapsed % BLOCK_INTERVAL_TICKS == 0) {
                takeTheHeat(level, eye, axis, focus);
            }

            return false;
        }

        private static boolean inCone(Vec3 eye, Vec3 axis, Vec3 point) {
            Vec3 to = point.subtract(eye);
            double length = to.length();
            return length < 1.5 || axis.dot(to.scale(1.0 / length)) >= CONE_COS;
        }

        private void takeTheHeat(ServerLevel level, Vec3 eye, Vec3 axis, Vec3 focus) {
            if (!player.mayBuild()) {
                return;
            }

            int changed = 0;
            boolean doused = false;
            boolean cooled = false;
            int reach = (int) Math.ceil(BLOCK_RADIUS);
            BlockPos centre = BlockPos.containing(focus);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int shell = 0; shell <= reach && changed < MAX_BLOCK_CHANGES; shell++) {
                for (int dx = -shell; dx <= shell && changed < MAX_BLOCK_CHANGES; dx++) {
                    for (int dy = -shell; dy <= shell && changed < MAX_BLOCK_CHANGES; dy++) {
                        for (int dz = -shell; dz <= shell && changed < MAX_BLOCK_CHANGES; dz++) {
                            if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != shell) {
                                continue;
                            }
                            pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                            Vec3 middle = Vec3.atCenterOf(pos);
                            if (middle.distanceTo(focus) > BLOCK_RADIUS || !inCone(eye, axis, middle)
                                    || !level.mayInteract(player, pos)) {
                                continue;
                            }

                            BlockState state = level.getBlockState(pos);
                            Result result = cool(level, pos, state);
                            if (result == Result.NONE) {
                                continue;
                            }
                            changed++;
                            doused |= result == Result.DOUSED;
                            cooled |= result == Result.COOLED;
                            level.sendParticles(ModRegistries.INHALE_SMOKE.get(),
                                    middle.x, middle.y + 0.3, middle.z, 0,
                                    0.0, blowing ? -1.0 : 1.0, 0.0, 1.0);
                        }
                    }
                }
            }

            if (doused) {
                level.playSound(null, BlockPos.containing(focus), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS, 0.7F, 1.2F);
            }
            if (cooled) {
                level.playSound(null, BlockPos.containing(focus), SoundEvents.LAVA_EXTINGUISH,
                        SoundSource.BLOCKS, 0.7F, 0.9F);
            }
        }

        private enum Result { NONE, DOUSED, COOLED }

        private static Result cool(ServerLevel level, BlockPos pos, BlockState state) {
            if (state.getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
                return Result.DOUSED;
            }

            if ((state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof AbstractCandleBlock)
                    && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
                if (state.getBlock() instanceof CampfireBlock) {
                    CampfireBlock.dowse(null, level, pos, state);
                }
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 11);
                return Result.DOUSED;
            }

            FluidState fluid = state.getFluidState();
            if (state.is(Blocks.LAVA) && fluid.is(FluidTags.LAVA)) {
                level.setBlock(pos, fluid.isSource() ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : Blocks.AIR.defaultBlockState(), 3);
                return Result.COOLED;
            }
            return Result.NONE;
        }

        private void finish() {
            if (stand.isAlive()) {
                stand.releaseInhale(HOLD_OVERRUN_TICKS);
            }
            if (!player.isAlive()) {
                return;
            }
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.setMoveCooldown(InhaleSkill.ID, player.level().getGameTime(), InhaleSkill.recoveryTicks(data));
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
    }
}
