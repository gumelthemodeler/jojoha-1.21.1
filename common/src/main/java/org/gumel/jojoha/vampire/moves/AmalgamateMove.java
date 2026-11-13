package org.gumel.jojoha.vampire.moves;

import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMinions;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;

public final class AmalgamateMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "amalgamate");
    public static final AmalgamateMove INSTANCE = new AmalgamateMove();

    private static final int COOLDOWN_TICKS = 300;
    private static final float ENERGY_COST = 25F;

    private static final int PULL_TICKS = 6;
    private static final double SIDE = 1.4;
    private static final double SIDE_AHEAD = 0.5;
    private static final double FRONT = 1.3;

    private static final List<Fusion> FUSIONS = new ArrayList<>();

    private AmalgamateMove() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> FUSIONS.removeIf(Fusion::advance));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        if (FUSIONS.stream().anyMatch(fusion -> fusion.caster == player)) {
            return;
        }
        Zombie[] pair = VampireMinions.pair(player);
        if (pair == null) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.vampire.nothing_to_fuse",
                            (int) VampireMinions.FUSE_RANGE), true);
            return;
        }

        MovementFx.pose(player, VampirePoses.AMALGAMATE);
        FUSIONS.add(new Fusion(player, pair[0], pair[1]));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_INFECT,
                SoundSource.PLAYERS, 0.8F, 0.4F);
    }

    private static Vec3 forward(ServerPlayer player) {
        return Vec3.directionFromRotation(0.0F, player.getYRot());
    }

    private static Vec3 side(ServerPlayer player, double hand) {
        Vec3 ahead = forward(player);
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return player.position().add(right.scale(SIDE * hand)).add(ahead.scale(SIDE_AHEAD));
    }

    private static Vec3 front(ServerPlayer player) {
        return player.position().add(forward(player).scale(FRONT));
    }

    private static Vec3 groundAt(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int down = 0; down <= 3; down++) {
            BlockPos below = pos.below(down + 1);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(at.x, below.getY() + 1.0, at.z);
            }
        }
        return at;
    }

    private static final class Fusion {
        private final ServerPlayer caster;
        private final Zombie right;
        private final Zombie left;
        private final Vec3 rightFrom;
        private final Vec3 leftFrom;
        private int elapsed;

        private Fusion(ServerPlayer caster, Zombie right, Zombie left) {
            this.caster = caster;
            this.right = right;
            this.left = left;
            this.rightFrom = right.position();
            this.leftFrom = left.position();
            right.setNoAi(true);
            left.setNoAi(true);
        }

        private boolean advance() {
            if (!caster.isAlive() || caster.isRemoved() || !right.isAlive() || !left.isAlive()
                    || right.level() != caster.level() || left.level() != caster.level()) {
                release();
                return true;
            }
            ServerLevel level = caster.serverLevel();
            elapsed++;

            if (elapsed >= VampirePoses.AMALGAMATE_FUSE_TICKS) {
                fuse(level);
                return true;
            }

            Vec3 rightSide = side(caster, 1.0);
            Vec3 leftSide = side(caster, -1.0);
            Vec3 rightAt;
            Vec3 leftAt;
            if (elapsed <= PULL_TICKS) {
                double t = ease(elapsed / (double) PULL_TICKS);
                rightAt = rightFrom.lerp(rightSide, t);
                leftAt = leftFrom.lerp(leftSide, t);
            } else if (elapsed <= VampirePoses.AMALGAMATE_SPREAD_TICKS) {
                rightAt = rightSide;
                leftAt = leftSide;
            } else {
                double t = ease((elapsed - VampirePoses.AMALGAMATE_SPREAD_TICKS)
                        / (double) (VampirePoses.AMALGAMATE_FUSE_TICKS - VampirePoses.AMALGAMATE_SPREAD_TICKS));
                Vec3 meet = front(caster);
                rightAt = rightSide.lerp(meet, t);
                leftAt = leftSide.lerp(meet, t);
            }
            hold(level, right, rightAt);
            hold(level, left, leftAt);
            return false;
        }

        private static double ease(double t) {
            double clamped = Mth.clamp(t, 0.0, 1.0);
            return clamped * clamped * (3.0 - 2.0 * clamped);
        }

        private void hold(ServerLevel level, Zombie minion, Vec3 at) {
            Vec3 ground = groundAt(level, at);
            minion.teleportTo(ground.x, ground.y, ground.z);
            minion.setDeltaMovement(Vec3.ZERO);
            Vec3 look = caster.position().subtract(ground);
            float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90.0F;
            minion.setYRot(yaw);
            minion.setYBodyRot(yaw);
            minion.setYHeadRot(yaw);
            VampireFx.blood(level, ground.x, ground.y + 1.0, ground.z, 2, 0.25, 0.02);
        }

        private void release() {
            if (right.isAlive()) {
                right.setNoAi(false);
            }
            if (left.isAlive()) {
                left.setNoAi(false);
            }
        }

        private void fuse(ServerLevel level) {
            Vec3 seam = groundAt(level, front(caster));
            VampireMinions.Fused made = VampireMinions.fuse(caster, right, left, seam);
            if (made == null) {
                release();
                return;
            }

            caster.displayClientMessage(
                    Component.translatable("message.jojoha.vampire.fused",
                            Component.translatable(made.translationKey())), true);

            VampireFx.blood(level, seam.x, seam.y + 1.0, seam.z, 34, 0.7, 0.3);
            for (double out = 0.6; out <= 2.4; out += 0.6) {
                VampireFx.ring(level, seam.x, seam.y + 1.0, seam.z, out, out > 1.5 ? 2 : 1);
            }
            VampireFx.aura(level, seam.x, seam.y + 1.2, seam.z, 22);
            level.playSound(null, BlockPos.containing(seam), SoundEvents.ZOMBIE_INFECT,
                    SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }
}
