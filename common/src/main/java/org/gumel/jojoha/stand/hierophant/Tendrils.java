package org.gumel.jojoha.stand.hierophant;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.TendrilPacket;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.mob.MobStandHolder;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.ArrayList;
import java.util.List;

public final class Tendrils {
    private static final double VIEW_RANGE = 96.0;

    public static final float THIN = 0.05F;
    public static final float THICK = 0.1F;

    public static final int AS_PAINTED = 0;

    public static final int PAINTED_GLOW = 0x4FD34F;

    private static final ParticleOptions PAINTED_CHIPS =
            new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD));

    private static int nextKey = 1;

    private Tendrils() {
    }

    public static int newKey() {
        int key = nextKey++;
        if (nextKey == Integer.MAX_VALUE) {
            nextKey = 1;
        }
        return key;
    }

    public static int tintOf(Player player) {
        if (!(player instanceof ServerPlayer served)) {
            return AS_PAINTED;
        }
        JojohaPlayerData data = PlayerDataAccess.get(served);
        if (!data.stand.isPresent() || data.stand.skin() == 0) {
            return AS_PAINTED;
        }
        int colour = StandTypes.byIdOrDefault(data.stand.standId()).auraColorFor(data.stand.skin()) & 0xFFFFFF;
        return colour == 0 ? 0x010101 : colour;
    }

    public static int glowOf(int tint) {
        return tint == AS_PAINTED ? PAINTED_GLOW : tint;
    }

    public static EmeraldSparkle sparkle(int tint) {
        return new EmeraldSparkle(tint == AS_PAINTED ? 0xFFFFFF : tint);
    }

    public static ParticleOptions chips(int tint) {
        return tint == AS_PAINTED ? PAINTED_CHIPS : new EmeraldChip(tint);
    }

    public static int tintOf(Mob mob) {
        if (!(mob instanceof MobStandHolder holder) || holder.jojoha$standId() == null
                || holder.jojoha$standSkin() == 0) {
            return AS_PAINTED;
        }
        int colour = StandTypes.byIdOrDefault(MobStands.standId(mob)).auraColorFor(holder.jojoha$standSkin())
                & 0xFFFFFF;
        return colour == 0 ? 0x010101 : colour;
    }

    public static void show(ServerLevel level, int key, List<Vec3> points, float width, int ticks, int tint) {
        if (points.size() < 2) {
            return;
        }
        TendrilPacket packet = packet(key, points, width, ticks, tint);
        Vec3 at = points.get(0);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(at) < VIEW_RANGE * VIEW_RANGE) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void line(ServerLevel level, int key, Vec3 from, Vec3 to, float width, int ticks, int tint) {
        show(level, key, List.of(from, to), width, ticks, tint);
    }

    public static void clear(ServerLevel level, int key, Vec3 near) {
        TendrilPacket packet = new TendrilPacket(key, near.x, near.y, near.z, List.of(0F, 0F, 0F), 0F, 0, AS_PAINTED);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(near) < VIEW_RANGE * VIEW_RANGE) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    private static TendrilPacket packet(int key, List<Vec3> points, float width, int ticks, int tint) {
        Vec3 start = points.get(0);
        List<Float> offsets = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            Vec3 point = points.get(i);
            offsets.add((float) (point.x - start.x));
            offsets.add((float) (point.y - start.y));
            offsets.add((float) (point.z - start.z));
        }
        return new TendrilPacket(key, start.x, start.y, start.z, offsets, width, ticks, tint);
    }
}
