package org.gumel.jojoha.combat.fx;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.ImpactCuePacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandShadowForm;

public final class Impacts {
    private static final double SEE_RANGE = 48.0;

    private Impacts() {
    }

    public static void landed(ServerLevel level, LivingEntity target, Vec3 direction,
                              ImpactSpec spec, int colour) {
        if (target == null) {
            return;
        }
        landed(level, target.position().add(0, target.getBbHeight() * 0.6, 0), direction, spec, colour,
                target.getId());
    }

    public static void landed(ServerLevel level, LivingEntity target, Vec3 at, Vec3 direction,
                              ImpactSpec spec, int colour) {
        if (target == null) {
            return;
        }
        landed(level, at != null ? at : target.position().add(0, target.getBbHeight() * 0.6, 0), direction, spec,
                colour, target.getId());
    }

    public static void landed(ServerLevel level, Vec3 at, Vec3 direction, ImpactSpec spec, int colour) {
        landed(level, at, direction, spec, colour, -1);
    }

    private static void landed(ServerLevel level, Vec3 at, Vec3 direction, ImpactSpec spec, int colour,
                               int targetId) {
        if (level == null || at == null || spec == null) {
            return;
        }

        RandomSource random = level.getRandom();
        Vec3 heading = direction == null || direction.lengthSqr() < 1.0E-8
                ? new Vec3(0, 1, 0)
                : direction.normalize();

        sound(level, at, spec, random);

        int seed = random.nextInt();
        Entity striker = strikerOf(level, targetId);
        int strikerId = striker == null ? -1 : striker.getId();
        ImpactCuePacket cue = new ImpactCuePacket(
                at.x, at.y, at.z, (float) heading.x, (float) heading.y, (float) heading.z,
                spec.tier().ordinal(), spec.sparks(), spec.spread(), spec.ring(), spec.shake(), spec.frame(),
                colour & 0xFFFFFF, targetId, strikerId, seed);
        ImpactCuePacket unframed = spec.frame() <= 0F ? cue
                : new ImpactCuePacket(
                        at.x, at.y, at.z, (float) heading.x, (float) heading.y, (float) heading.z,
                        spec.tier().ordinal(), spec.sparks(), spec.spread(), spec.ring(), spec.shake(), 0F,
                        colour & 0xFFFFFF, targetId, strikerId, seed);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at.x, at.y, at.z) <= SEE_RANGE * SEE_RANGE) {
                NetworkManager.sendToPlayer(player, spec.frame() > 0F && player == striker ? cue : unframed);
            }
        }
    }

    private static final int STRIKE_RECENT_TICKS = 10;

    private static Entity strikerOf(ServerLevel level, int targetId) {
        if (targetId < 0 || !(level.getEntity(targetId) instanceof LivingEntity target)) {
            return null;
        }
        LivingEntity by = target.getLastHurtByMob();
        if (by == null || target.tickCount - target.getLastHurtByMobTimestamp() > STRIKE_RECENT_TICKS) {
            return null;
        }
        return by;
    }

    private static void sound(ServerLevel level, Vec3 at, ImpactSpec spec, RandomSource random) {
        if (spec.volume() <= 0F) {
            return;
        }

        float jitter = 0.94F + random.nextFloat() * 0.12F;
        boolean tap = spec.tier() == ImpactTier.TAP;
        SoundEvent sound = spec.tier() == ImpactTier.GUARD_BREAK
                ? ModSounds.GUARD_BREAK.get()
                : ImpactSpec.perfectParry(spec)
                ? ModSounds.HEAVY_PARRY.get()
                : spec.tier() == ImpactTier.PARRY
                ? ModSounds.PARRY.get()
                : tap ? ModSounds.HIT_TAP.get()
                        : ModSounds.IMPACT_PUNCH.get();
        level.playSound(null, at.x, at.y, at.z, sound,
                SoundSource.PLAYERS, spec.volume(), pitchFor(spec.tier()) * jitter);
    }

    static float pitchFor(ImpactTier tier) {
        return switch (tier) {
            case TAP -> 1.0F;
            case SOLID -> 1.08F;
            case HEAVY -> 0.92F;
            case GUARD_BREAK -> 0.85F;
            case PARRY -> 1.0F;
            case FINISHER -> 0.78F;
            case THUD -> 0.82F;
        };
    }

    public static final int FLESH = 0xFFE0B0;

    public static final int PLAYER = 0xFFFFFF;

    public static final int HAMON = 0xFFDE33;

    public static final int BLOOD = 0xC0141F;

    public static final int RIPPER = 0xFF3D9E;

    public static final int FROST = 0xA8E4FF;

    public static final int STEEL = 0xDDE3EA;

    public static int colourOf(ServerPlayer player) {
        JojohaPlayerData data =
                PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return FLESH;
        }
        return StandShadowForm.auraColourFor(data.stand, player.getUUID());
    }
}
