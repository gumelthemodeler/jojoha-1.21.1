package org.gumel.jojoha.stand;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.BarrageStrugglePacket;
import org.gumel.jojoha.registry.ModSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class BarrageStruggle {
    public static final int MAX_TICKS = 100;
    private static final int TENSION_EVERY_TICKS = 20;

    public static final float POWER_PUSH = 0.01F;
    public static final float ON_BEAT_PUSH = 0.14F;
    public static final float OFF_BEAT_PUSH = 0.03F;

    public static final int BEAT_TICKS = 6;
    public static final int BEAT_SLACK = 1;

    private static final double DRIFT = 0.07;

    public static final float MOB_POWER = 1.2F;

    public static final float LOSS_DAMAGE = 6.0F;
    private static final double LOSS_SHOVE = 1.4;
    private static final double LOSS_LIFT = 0.4;

    private static final int MARK_EVERY_TICKS = 3;
    private static final double BREAK_DISTANCE = 8.0;
    public static final double MEET_DISTANCE = 6.0;
    public static final double MEET_FACING = 0.5;

    private static final class Struggle {
        final LivingEntity a;
        final LivingEntity b;
        float meter;
        int ticks;
        int windowA = -1;
        int windowB = -1;
        int pressesA;
        int pressesB;
        boolean onBeatA;
        boolean onBeatB;

        Struggle(LivingEntity a, LivingEntity b) {
            this.a = a;
            this.b = b;
        }

        boolean has(LivingEntity e) {
            return a == e || b == e;
        }

        LivingEntity other(LivingEntity e) {
            return a == e ? b : a;
        }
    }

    private static final List<Struggle> ACTIVE = new ArrayList<>();

    private BarrageStruggle() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            seek(server);
            tick();
        });
    }

    public static boolean in(LivingEntity user) {
        return user != null && find(user) != null;
    }

    public static boolean between(LivingEntity one, LivingEntity other) {
        Struggle s = find(one);
        return s != null && s.has(other);
    }

    private static Struggle find(LivingEntity user) {
        for (Struggle s : ACTIVE) {
            if (s.has(user)) {
                return s;
            }
        }
        return null;
    }

    public static boolean barraging(LivingEntity user) {
        if (user instanceof Player player) {
            StandEntity stand = StandEntity.findFor(player);
            return stand != null && stand.isHeldBarrageRunning();
        }
        return false;
    }

    public static boolean begin(LivingEntity a, LivingEntity b, Vec3 at) {
        if (a == null || b == null || a == b || in(a) || in(b) || !barraging(a) || !barraging(b)) {
            return false;
        }
        ACTIVE.add(new Struggle(a, b));
        root(a, true);
        root(b, true);
        ServerLevel level = (ServerLevel) a.level();
        Impacts.landed(level, at, b.position().subtract(a.position()), ImpactSpec.CLASH, Impacts.PLAYER);
        level.playSound(null, at.x, at.y, at.z, ModSounds.STAND_HIT.get(), SoundSource.PLAYERS, 1.2F, 0.6F);
        return true;
    }

    public static boolean mash(ServerPlayer player) {
        Struggle s = find(player);
        if (s == null) {
            return false;
        }
        int window = (s.ticks + BEAT_TICKS / 2) / BEAT_TICKS;
        int offset = (s.ticks + BEAT_TICKS / 2) % BEAT_TICKS - BEAT_TICKS / 2;
        boolean onBeat = Math.abs(offset) <= BEAT_SLACK;
        float push = onBeat ? ON_BEAT_PUSH : OFF_BEAT_PUSH;
        if (s.a == player) {
            if (s.windowA != window) {
                s.windowA = window;
                s.meter += push;
                s.pressesA++;
                s.onBeatA = onBeat;
            }
        } else if (s.windowB != window) {
            s.windowB = window;
            s.meter -= push;
            s.pressesB++;
            s.onBeatB = onBeat;
        }
        return true;
    }

    private static float power(LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            return Math.max(0F, PlayerDataAccess.get(player).stand.powerScale());
        }
        return MOB_POWER;
    }

    private static void seek(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer a = players.get(i);
            if (in(a) || !barraging(a)) {
                continue;
            }
            for (int j = i + 1; j < players.size(); j++) {
                ServerPlayer b = players.get(j);
                if (b.level() == a.level() && !in(b) && barraging(b) && meet(a, b)) {
                    begin(a, b, a.getEyePosition().lerp(b.getEyePosition(), 0.5));
                    break;
                }
            }
        }
    }

    private static boolean meet(ServerPlayer a, ServerPlayer b) {
        if (a.distanceTo(b) > MEET_DISTANCE || !a.hasLineOfSight(b)) {
            return false;
        }
        Vec3 across = b.position().subtract(a.position()).multiply(1, 0, 1);
        if (across.lengthSqr() < 1.0E-6) {
            return true;
        }
        Vec3 toward = across.normalize();
        return facing(a, toward) >= MEET_FACING && facing(b, toward.reverse()) >= MEET_FACING;
    }

    private static double facing(ServerPlayer player, Vec3 toward) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 1.0E-6 ? 1.0 : look.normalize().dot(toward);
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        for (Iterator<Struggle> it = ACTIVE.iterator(); it.hasNext(); ) {
            Struggle s = it.next();
            if (!s.a.isAlive() || !s.b.isAlive() || s.a.level() != s.b.level()
                    || s.a.distanceTo(s.b) > BREAK_DISTANCE) {
                it.remove();
                release(s);
                continue;
            }
            boolean aHolds = barraging(s.a), bHolds = barraging(s.b);
            if (!aHolds || !bHolds) {
                it.remove();
                if (aHolds) {
                    decide(s, s.a, s.b);
                } else if (bHolds) {
                    decide(s, s.b, s.a);
                } else {
                    release(s);
                }
                continue;
            }

            if (s.ticks % TENSION_EVERY_TICKS == 0) {
                if (s.a instanceof ServerPlayer one) {
                    NetworkHandler.broadcastTension(one);
                }
                if (s.b instanceof ServerPlayer other) {
                    NetworkHandler.broadcastTension(other);
                }
            }
            s.ticks++;
            s.meter += (power(s.a) - power(s.b)) * POWER_PUSH;
            s.meter = Math.max(-1F, Math.min(1F, s.meter));
            drift(s);

            ServerLevel level = (ServerLevel) s.a.level();
            if (s.ticks % MARK_EVERY_TICKS == 0) {
                mark(level, s);
            }
            show(s);

            if (s.meter >= 1F || s.meter <= -1F || s.ticks >= MAX_TICKS) {
                it.remove();
                if (s.meter > 0F) {
                    decide(s, s.a, s.b);
                } else if (s.meter < 0F) {
                    decide(s, s.b, s.a);
                } else {
                    release(s);
                }
            }
        }
    }

    private static Vec3 meeting(Struggle s) {
        Vec3 a = s.a.position().add(0, s.a.getBbHeight() * 0.6, 0);
        Vec3 b = s.b.position().add(0, s.b.getBbHeight() * 0.6, 0);
        return a.lerp(b, 0.5 + 0.3 * s.meter);
    }

    private static void mark(ServerLevel level, Struggle s) {
        Vec3 at = meeting(s);
        Vec3 across = s.b.position().subtract(s.a.position());
        Impacts.landed(level, at, s.ticks % (MARK_EVERY_TICKS * 2) == 0 ? across : across.scale(-1),
                ImpactSpec.STRUGGLE, Impacts.PLAYER);
        if (s.ticks % (MARK_EVERY_TICKS * 2) == 0) {
            level.playSound(null, at.x, at.y, at.z, ModSounds.STAND_HIT.get(), SoundSource.PLAYERS, 0.7F,
                    0.9F + level.random.nextFloat() * 0.3F);
        }
    }

    private static void drift(Struggle s) {
        Vec3 across = s.b.position().subtract(s.a.position());
        Vec3 flat = new Vec3(across.x, 0.0, across.z);
        Vec3 slide = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize().scale(s.meter * DRIFT);
        for (LivingEntity user : new LivingEntity[] {s.a, s.b}) {
            if (user instanceof Player) {
                user.setDeltaMovement(slide.x, Math.min(0, user.getDeltaMovement().y), slide.z);
                user.hurtMarked = true;
            }
        }
    }

    private static void show(Struggle s) {
        send(s, s.a, s.b, s.meter, true, BarrageStrugglePacket.NONE);
        send(s, s.b, s.a, -s.meter, true, BarrageStrugglePacket.NONE);
    }

    private static void send(Struggle s, LivingEntity to, LivingEntity against, float mine, boolean active, int result) {
        if (to instanceof ServerPlayer player) {
            boolean first = s.a == to;
            NetworkHandler.sendBarrageStruggle(player,
                    new BarrageStrugglePacket(active, mine, colourOf(to), colourOf(against), result,
                            s.ticks, first ? s.pressesA : s.pressesB, first ? s.onBeatA : s.onBeatB));
        }
    }

    private static int colourOf(LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            return StandShadowForm.auraColourFor(PlayerDataAccess.get(player).stand, player.getUUID()) & 0xFFFFFF;
        }
        return MOB_COLOUR;
    }

    private static final int MOB_COLOUR = 0xFFD23F;

    private static void decide(Struggle s, LivingEntity winner, LivingEntity loser) {
        release(s);
        ServerLevel level = (ServerLevel) winner.level();

        StandEntity winning = winner instanceof Player p ? StandEntity.findFor(p) : null;
        if (winning != null) {
            winning.triggerPunchAt(loser, true);
        }

        Vec3 away = loser.position().subtract(winner.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Impacts.landed(level, loser, flat, ImpactSpec.FINISHER, Impacts.PLAYER);

        DamageSource source = winner instanceof Player p
                ? level.damageSources().playerAttack(p)
                : level.damageSources().mobAttack(winner);
        loser.invulnerableTime = 0;
        loser.hurt(source, DamageScale.stand(winner, loser, LOSS_DAMAGE));
        if (flat.lengthSqr() > 1.0E-6) {
            Vec3 shove = flat.normalize().scale(LOSS_SHOVE);
            Vec3 launch = new Vec3(shove.x, LOSS_LIFT, shove.z);
            loser.setDeltaMovement(launch);
            loser.hurtMarked = true;
            ThrownBodies.launch(loser, launch);
        }
        Stun.stagger(loser);
        level.playSound(null, loser.getX(), loser.getY(), loser.getZ(), ModSounds.GUARD_BREAK.get(),
                SoundSource.PLAYERS, 1.0F, 0.7F);

        send(s, winner, loser, 1F, false, BarrageStrugglePacket.WON);
        send(s, loser, winner, -1F, false, BarrageStrugglePacket.LOST);
    }

    private static void release(Struggle s) {
        send(s, s.a, s.b, s.meter, false, BarrageStrugglePacket.NONE);
        send(s, s.b, s.a, -s.meter, false, BarrageStrugglePacket.NONE);
        for (LivingEntity user : new LivingEntity[] {s.a, s.b}) {
            root(user, false);
            if (user instanceof Player player) {
                StandEntity stand = StandEntity.findFor(player);
                if (stand != null && stand.isHeldBarrageRunning()) {
                    stand.endHeldBarrageQuietly();
                }
            }
        }
    }

    private static void root(LivingEntity user, boolean rooted) {
        if (!(user instanceof Player) || !rooted) {
            return;
        }
        user.setDeltaMovement(0, Math.min(0, user.getDeltaMovement().y), 0);
        user.hurtMarked = true;
    }
}
