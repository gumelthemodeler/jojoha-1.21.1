package org.gumel.jojoha.stand.hierophant;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.PossessionInputPacket;
import org.gumel.jojoha.network.packet.PossessionStatePacket;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.moves.BodyPossessionSkill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Possession {
    private static final int MOB_TICKS = 30 * 20;
    private static final int PLAYER_TICKS = 3 * 20;

    private static final float TEAR_OUT_DAMAGE = 8F;

    public static final int INTERNAL_EVERY = 5 * 20;
    private static final float INTERNAL_DAMAGE = 5F;

    private static final double LEASH = 48.0;

    public static final float GUARDED_HEALTH = 0.5F;

    private static final double MOST_PER_TICK = 8.0;

    private static final int ATTACK_REST = 10;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private Possession() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(Possession::tick);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player instanceof ServerPlayer quitting) {
                end(quitting, null);
                for (Map.Entry<UUID, Session> entry : Map.copyOf(SESSIONS).entrySet()) {
                    if (entry.getValue().body.equals(quitting.getUUID())) {
                        ServerPlayer holder = quitting.server.getPlayerList().getPlayer(entry.getKey());
                        if (holder != null) {
                            end(holder, "message.jojoha.possession.lost");
                        }
                    }
                }
            }
        });
    }

    public static boolean possessing(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static boolean possessed(Entity body) {
        for (Session session : SESSIONS.values()) {
            if (session.body.equals(body.getUUID())) {
                return true;
            }
        }
        return false;
    }

    public static String canTake(ServerPlayer user, LivingEntity body) {
        if (body == user || body instanceof StandEntity || body instanceof ArmorStand
                || body instanceof EnderDragon || body instanceof WitherBoss || !body.isAlive()) {
            return "message.jojoha.possession.cannot";
        }
        if (possessed(body)) {
            return "message.jojoha.possession.taken";
        }
        if (body instanceof ServerPlayer player) {
            if (player.isSpectator() || player.isCreative() || possessing(player)) {
                return "message.jojoha.possession.cannot";
            }
            JojohaPlayerData data = PlayerDataAccess.get(player);
            boolean guarded = data.stand.isPresent() || data.discipline != Discipline.NONE
                    || Fate.undead(data);
            if (guarded && player.getHealth() > player.getMaxHealth() * GUARDED_HEALTH) {
                return "message.jojoha.possession.too_strong";
            }
        }
        return null;
    }

    public static boolean merged(Player player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.merged;
    }

    public static void merge(ServerPlayer user, LivingEntity body, int mobTicks, int playerTicks) {
        open(user, body, body instanceof Player ? playerTicks : mobTicks, true);
        NetworkManager.sendToPlayer(user, new PossessionStatePacket(body.getId(), false, true, true));
        if (body instanceof ServerPlayer victim) {
            NetworkManager.sendToPlayer(victim, new PossessionStatePacket(user.getId(), true, true, true));
            victim.displayClientMessage(Component.translatable("message.jojoha.infiltrate.taken_over",
                    user.getDisplayName()), true);
        }
    }

    private static Session open(ServerPlayer user, LivingEntity body, int ticks, boolean merged) {
        ServerLevel level = user.serverLevel();
        boolean wasNoAi = body instanceof Mob mob && mob.isNoAi();
        if (body instanceof Mob mob && merged && mob.getTarget() == user) {
            mob.setTarget(null);
        }
        if (body instanceof Mob mob && !merged) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setNoAi(true);
        }
        Session session = new Session(body.getUUID(), level, level.getGameTime(), ticks, wasNoAi, merged);
        session.yaw = body.getYHeadRot();
        session.pitch = body.getXRot();
        SESSIONS.put(user.getUUID(), session);
        return session;
    }

    public static void begin(ServerPlayer user, LivingEntity body, StandEntity stand) {
        ServerLevel level = user.serverLevel();
        open(user, body, body instanceof Player ? PLAYER_TICKS : MOB_TICKS, false);

        if (stand != null) {
            Tendrils.line(level, Tendrils.newKey(), stand.position().add(0.0, stand.getBbHeight() * 0.5, 0.0),
                    body.position().add(0.0, body.getBbHeight() * 0.5, 0.0), Tendrils.THICK, 12, Tendrils.tintOf(user));
            stand.setInvisible(true);
        }
        NetworkManager.sendToPlayer(user, new PossessionStatePacket(body.getId(), false, true, false));
        if (body instanceof ServerPlayer victim) {
            NetworkManager.sendToPlayer(victim, new PossessionStatePacket(user.getId(), true, true, false));
            victim.displayClientMessage(Component.translatable("message.jojoha.possession.taken_over",
                    user.getDisplayName()), true);
        }
        user.displayClientMessage(Component.translatable("message.jojoha.possession.begun", body.getDisplayName()),
                true);
        level.sendParticles(Tendrils.sparkle(Tendrils.tintOf(user)), body.getX(), body.getY() + body.getBbHeight() * 0.5, body.getZ(), 30,
                0.3, body.getBbHeight() * 0.3, 0.3, 0.02);
        level.playSound(null, body.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    public static void end(ServerPlayer user, String why) {
        Session session = SESSIONS.remove(user.getUUID());
        if (session == null) {
            return;
        }
        Entity body = session.level.getEntity(session.body);
        if (body instanceof Mob mob) {
            mob.setNoAi(session.wasNoAi);
        }
        NetworkManager.sendToPlayer(user, new PossessionStatePacket(body == null ? -1 : body.getId(), false, false,
                session.merged));
        if (body instanceof ServerPlayer victim) {
            NetworkManager.sendToPlayer(victim, new PossessionStatePacket(user.getId(), true, false, session.merged));
        }
        if (session.merged) {
            PillarMen.burst(user);
            if (why != null) {
                user.displayClientMessage(Component.translatable(why), true);
            }
            return;
        }
        if (body instanceof ServerPlayer victim) {
            tearOut(user, victim, session.level);
        }
        StandEntity stand = StandEntity.findFor(user);
        if (stand != null && body != null && body.level() == stand.level() && user.isAlive()) {
            PossessionMotion.exit(user, body, stand);
        } else if (stand != null) {
            stand.setInvisible(false);
        }
        if (body != null) {
            session.level.sendParticles(Tendrils.sparkle(Tendrils.tintOf(user)), body.getX(), body.getY() + body.getBbHeight() * 0.5, body.getZ(),
                    20, 0.3, body.getBbHeight() * 0.3, 0.3, 0.02);
        }

        JojohaPlayerData data = PlayerDataAccess.get(user);
        data.setMoveCooldown(BodyPossessionSkill.ID, user.level().getGameTime(), BodyPossessionSkill.COOLDOWN_TICKS);
        PlayerDataAccess.set(user, data);
        PlayerDataAccess.sync(user);
        if (why != null) {
            user.displayClientMessage(Component.translatable(why), true);
        }
    }

    private static void tearOut(ServerPlayer user, ServerPlayer victim, ServerLevel level) {
        if (!victim.isAlive() || victim.hasDisconnected() || !user.isAlive() || user.hasDisconnected()) {
            return;
        }
        HitWindow.open(victim);
        victim.hurt(level.damageSources().indirectMagic(user, user), DamageScale.stand(user, victim, TEAR_OUT_DAMAGE));
        level.sendParticles(Tendrils.sparkle(Tendrils.tintOf(user)), victim.getX(),
                victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 36, victim.getBbWidth() * 0.3,
                victim.getBbHeight() * 0.3, victim.getBbWidth() * 0.3, 0.12);
        level.playSound(null, victim.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    public static void steer(ServerPlayer user, float yaw, float pitch, double x, double y, double z) {
        Session session = SESSIONS.get(user.getUUID());
        if (session == null || session.merged) {
            return;
        }
        session.yaw = yaw;
        session.pitch = Mth.clamp(pitch, -90F, 90F);
        Entity found = session.level.getEntity(session.body);
        if (!(found instanceof LivingEntity body)) {
            return;
        }
        Vec3 to = new Vec3(x, y, z);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || to.distanceToSqr(body.position()) > MOST_PER_TICK * MOST_PER_TICK) {
            return;
        }
        if (body instanceof ServerPlayer victim) {
            NetworkManager.sendToPlayer(victim, new PossessionInputPacket(yaw, session.pitch, x, y, z));
            return;
        }
        body.move(MoverType.SELF, to.subtract(body.position()));
        body.setPos(x, y, z);
        turn(body, session);
    }

    public static void attack(ServerPlayer user) {
        Session session = SESSIONS.get(user.getUUID());
        if (session == null || session.merged || session.level.getGameTime() < session.nextAttackAt) {
            return;
        }
        Entity found = session.level.getEntity(session.body);
        if (!(found instanceof LivingEntity body)) {
            return;
        }
        session.nextAttackAt = session.level.getGameTime() + ATTACK_REST;
        body.swing(InteractionHand.MAIN_HAND, true);
        LivingEntity target = inFront(body, user);
        if (target == null) {
            return;
        }
        if (body instanceof ServerPlayer victim) {
            victim.attack(target);
        } else if (body instanceof Mob mob && mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            mob.doHurtTarget(target);
        } else {
            target.hurt(session.level.damageSources().mobAttack(body), 1F);
        }
    }

    private static LivingEntity inFront(LivingEntity body, ServerPlayer user) {
        Vec3 eye = body.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(body.getXRot(), body.getYHeadRot());
        double reach = 2.5 + body.getBbWidth();
        AABB box = body.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity other : body.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != body && e != user && e.isAlive() && !(e instanceof StandEntity))) {
            Vec3 to = other.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along <= 0 || along > reach + other.getBbWidth()) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off < other.getBbWidth() * 0.5 + 0.8 && along < bestDistance) {
                best = other;
                bestDistance = along;
            }
        }
        return best;
    }

    private static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Session>> it = Map.copyOf(SESSIONS).entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayer user = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (user == null) {
                SESSIONS.remove(entry.getKey());
                continue;
            }
            Entity found = session.level.getEntity(session.body);
            long now = session.level.getGameTime();
            if (!(found instanceof LivingEntity body) || !body.isAlive()) {
                end(user, "message.jojoha.possession.lost");
                continue;
            }
            if (!user.isAlive() || user.level() != session.level
                    || (!session.merged && user.distanceToSqr(body) > LEASH * LEASH)) {
                end(user, "message.jojoha.possession.too_far");
                continue;
            }
            if (now - session.startedAt >= session.ticks) {
                end(user, "message.jojoha.possession.expired");
                continue;
            }

            if (session.merged) {
                if (body instanceof Mob mob && mob.getTarget() == user) {
                    mob.setTarget(null);
                }
            } else if (!(body instanceof Player)) {
                turn(body, session);
            }

            long inside = now - session.startedAt;
            if (!session.merged && inside > 0 && inside % INTERNAL_EVERY == 0) {
                HitWindow.open(body);
                body.hurt(session.level.damageSources().indirectMagic(user, user), DamageScale.stand(user, body, INTERNAL_DAMAGE));
                session.level.sendParticles(Tendrils.sparkle(Tendrils.tintOf(user)), body.getX(), body.getY() + body.getBbHeight() * 0.5,
                        body.getZ(), 24, body.getBbWidth() * 0.3, body.getBbHeight() * 0.3, body.getBbWidth() * 0.3,
                        0.05);
                session.level.playSound(null, body.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK,
                        SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }
    }

    private static void turn(LivingEntity body, Session session) {
        body.setYRot(session.yaw);
        body.setYHeadRot(session.yaw);
        body.setYBodyRot(session.yaw);
        body.setXRot(session.pitch);
    }

    private static final class Session {
        final UUID body;
        final ServerLevel level;
        final long startedAt;
        final int ticks;
        final boolean wasNoAi;
        final boolean merged;
        float yaw;
        float pitch;
        long nextAttackAt;

        Session(UUID body, ServerLevel level, long startedAt, int ticks, boolean wasNoAi, boolean merged) {
            this.body = body;
            this.level = level;
            this.startedAt = startedAt;
            this.ticks = ticks;
            this.wasNoAi = wasNoAi;
            this.merged = merged;
        }
    }
}
