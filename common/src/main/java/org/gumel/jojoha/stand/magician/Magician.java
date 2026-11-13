package org.gumel.jojoha.stand.magician;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.MagicianFxPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Magician {
    public static final int FLAME = 0xFF8A1E;

    public static final double BLAZE_REACH = 14.0;
    public static final double BLAZE_RADIUS = 4.5;
    public static final int BLAZE_TICKS = 240;
    public static final float WALL_DAMAGE = 2.0F;
    private static final int BLAZE_CONTACT_TICKS = 10;
    private static final double WALL_THICKNESS = 0.9;
    private static final double WALL_HEIGHT = 3.0;
    private static final int WALL_EVERY = 10;
    private static final double BLAZE_TRAVEL_SPEED = 0.9;
    private static final int RETURN_AFTER_TICKS = 14;

    public static final double DOME_RADIUS = 9.0;
    public static final int DOME_TICKS = 3000;
    private static final int DOME_CONTACT_TICKS = 10;
    private static final int DOME_EVERY = 40;
    private static final int ASH_SLOW_TICKS = 60;

    public static final float ENCROACH_SHARE = 0.5F;
    private static final double ENCROACH_STEP = 0.1;
    private static final int ZONE_SYNC_EVERY = 20;
    private static final int COMFORT_EVERY = 10;
    private static final int COMFORT_TICKS = 30;
    private static final double AREA_HEIGHT = 6.0;

    public static final float TERRITORIAL_DAMAGE = 1.15F;
    public static final float PRESSURE_CHANCE = 0.3F;
    public static final int INFERNO_TICKS = 240;
    public static final float INFERNO_DAMAGE = 1.25F;

    public static final float KICK_DAMAGE = 5.0F;
    public static final double KICK_RADIUS = 3.0;
    private static final double KICK_AHEAD = 2.5;
    private static final int KICK_CONTACT_TICKS = 6;
    private static final double KICK_LIFT = 0.35;
    private static final double KICK_SHOVE = 0.3;
    private static final double KICK_TRAVEL_SPEED = 1.2;

    public static final float ANKH_DAMAGE = 6.0F;
    public static final float SPECIAL_ANKH_DAMAGE = 3.5F;
    public static final int SPECIAL_ANKHS = 5;
    private static final double ANKH_SPEED = 1.3;
    private static final int ANKH_TICKS = 24;
    private static final double ANKH_REACH = 28.0;
    private static final int ANKH_CONTACT_TICKS = 12;

    private static final int SPECIAL_ANKH_CONTACT_TICKS = 16;
    private static final int SPECIAL_EVERY = 4;
    private static final double SPECIAL_FAN = 0.16;
    private static final float ANKH_GIRTH = 0.8F;

    public static final int BARRIER_TICKS = 200;
    public static final double BARRIER_LENGTH = 16.0;
    private static final double BARRIER_REACH = 12.0;
    private static final int BARRIER_CONTACT_TICKS = 10;
    private static final int BARRIER_DRAW_TICKS = 30;
    private static final double BARRIER_STEP = 1.0;
    private static final int BARRIER_STEPS_PER_TICK = 3;
    private static final double BARRIER_SHOVE = 0.45;

    public static final double BIND_REACH = 10.0;
    public static final int BIND_TICKS = 40;
    private static final int BIND_CONTACT_TICKS = 10;

    public static final double SENSE_RADIUS = 24.0;
    public static final int SENSE_TICKS = 120;

    public static final double HEAT_RADIUS = 4.0;
    public static final int HEAT_BLOCKS = 48;

    private static final double FX_RANGE = 128.0;

    private static final class Zone {
        final int id;
        final UUID owner;
        final ServerLevel level;
        final Vec3 centre;
        final double base;
        final long until;
        final boolean dome;
        double radius;

        Zone(int id, UUID owner, ServerLevel level, Vec3 centre, double radius, long until, boolean dome) {
            this.id = id;
            this.owner = owner;
            this.level = level;
            this.centre = centre;
            this.base = radius;
            this.radius = radius;
            this.until = until;
            this.dome = dome;
        }

        boolean holds(Entity entity) {
            if (entity.level() != level) {
                return false;
            }
            if (dome) {
                return entity.position().distanceToSqr(centre) <= radius * radius;
            }
            double dx = entity.getX() - centre.x;
            double dz = entity.getZ() - centre.z;
            return dx * dx + dz * dz <= radius * radius && Math.abs(entity.getY() - centre.y) <= AREA_HEIGHT;
        }

        MagicianFxPacket packet(long now) {
            return MagicianFxPacket.at(dome ? MagicianFxPacket.ZONE_DOME : MagicianFxPacket.ZONE_RING, id, centre,
                    (float) radius, (int) Math.max(1, until - now));
        }
    }

    private static final class Ankh {
        final int id;
        final ServerPlayer owner;
        final ServerLevel level;
        final Vec3 velocity;
        final float damage;
        final String key;
        Vec3 at;
        int left = ANKH_TICKS;

        Ankh(int id, ServerPlayer owner, Vec3 at, Vec3 velocity, float damage, String key) {
            this.id = id;
            this.owner = owner;
            this.level = owner.serverLevel();
            this.at = at;
            this.velocity = velocity;
            this.damage = damage;
            this.key = key;
        }
    }

    private static final class Barrier {
        final int id;
        final ServerPlayer owner;
        final ServerLevel level;
        final List<Vec3> points = new ArrayList<>();
        final long until;
        double length;
        boolean gone;

        Barrier(int id, ServerPlayer owner, long until) {
            this.id = id;
            this.owner = owner;
            this.level = owner.serverLevel();
            this.until = until;
        }
    }

    private static final List<Barrier> BARRIERS = new ArrayList<>();
    private static final List<Zone> ZONES = new ArrayList<>();
    private static final List<Ankh> ANKHS = new ArrayList<>();
    private static final Map<UUID, Long> INFERNO_UNTIL = new HashMap<>();
    private static int nextId = 1;

    private Magician() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(Magician::tick);
        MagicianFlames.init();
    }

    public static boolean is(StandType type) {
        return type != null && StandTypes.MAGICIANS_RED_ID.equals(type.id());
    }

    public static boolean is(StandEntity stand) {
        return stand != null && is(stand.getStandType());
    }

    private static StandEntity stand(ServerPlayer player) {
        StandEntity stand = StandEntity.findFor(player);
        return is(stand) && !stand.isDismissing() ? stand : null;
    }

    public static boolean inArea(ServerPlayer player) {
        for (Zone zone : ZONES) {
            if (zone.owner.equals(player.getUUID()) && zone.holds(player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean inferno(ServerPlayer player) {
        Long until = INFERNO_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    private static void broadcast(ServerLevel level, Vec3 near, MagicianFxPacket packet) {
        NetworkHandler.broadcastMagician(level, near, FX_RANGE, packet);
    }

    private static void voice(ServerPlayer player, SoundEvent sound, float pitch) {
        player.serverLevel().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    private static float blow(ServerPlayer player, LivingEntity target, String key, float base) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        float raw = StandTuning.damage(key, base) * data.stand.powerScale();
        return Math.min(10F, StandPassives.scaleOutgoing(player, data, target, raw));
    }

    private static boolean strike(ServerPlayer player, LivingEntity target, float amount) {
        HitWindow.open(target);
        boolean hurt = target.hurt(player.damageSources().playerAttack(player),
                DamageScale.stand(player, target, amount));
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        PlayerDataAccess.set(player, data);
        return hurt;
    }

    private static boolean foe(ServerPlayer owner, Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && entity != owner
                && !(entity instanceof StandEntity) && !entity.isSpectator() && StandHits.hittable(owner, entity);
    }

    private static Vec3 ground(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int down = 0; down <= 8; down++) {
            BlockPos below = pos.below(down);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(at.x, below.getY() + 1.0, at.z);
            }
        }
        return at;
    }

    private static Vec3 aimPoint(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
    }

    private static void comeBack(ServerPlayer player, int after) {
        StandBeat.at(after, player, () -> {
            StandEntity out = stand(player);
            if (out != null) {
                out.clearWork();
            }
        });
    }

    private static void open(ServerPlayer player, Vec3 centre, double radius, int ticks, boolean dome) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone old = it.next();
            if (old.owner.equals(player.getUUID()) && old.dome == dome) {
                broadcast(old.level, old.centre, MagicianFxPacket.at(MagicianFxPacket.ZONE_END, old.id, old.centre, 0F, 0));
                it.remove();
            }
        }
        Zone zone = new Zone(nextId++, player.getUUID(), level, centre, radius, now + ticks, dome);
        ZONES.add(zone);
        broadcast(level, centre, zone.packet(now));
        if (dome) {
            MagicianFlames.scatter(level, centre, radius, MagicianFlames.DOME_SEEDS, MagicianFlames.DOME_SPREAD);
        } else {
            MagicianFlames.ring(level, centre, radius, MagicianFlames.BLAZE_SPREAD);
        }
    }

    public static boolean fingerSnap(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        MagicianFlames.snap(player);
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone zone = it.next();
            if (zone.owner.equals(player.getUUID())) {
                broadcast(zone.level, zone.centre, MagicianFxPacket.at(MagicianFxPacket.ZONE_END, zone.id, zone.centre, 0F, 0));
                it.remove();
            }
        }
        for (Barrier barrier : BARRIERS) {
            if (barrier.owner == player) {
                barrier.gone = true;
            }
        }
        INFERNO_UNTIL.remove(player.getUUID());
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,
                0.9F, 1.9F);
        return true;
    }

    public static boolean controlledBlaze(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = ground(level, aimPoint(player, BLAZE_REACH));
        stand.sendToWork(centre.add(0.0, 0.3, 0.0), BLAZE_TRAVEL_SPEED);
        stand.triggerStrikeForward("controlled_blaze");
        voice(player, ModSounds.MR_REDIRECT.get(), 1.0F);
        StandBeat.at(BLAZE_CONTACT_TICKS, player, () -> {
            open(player, centre, StandTuning.value("controlled_blaze_radius", (float) BLAZE_RADIUS),
                    StandTuning.ticks("controlled_blaze_ticks", BLAZE_TICKS), false);
            level.playSound(null, BlockPos.containing(centre), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                    1.0F, 0.7F);
        });
        comeBack(player, BLAZE_CONTACT_TICKS + RETURN_AFTER_TICKS);
        return true;
    }

    public static boolean sweepKick(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 point = player.position().add(flat.scale(KICK_AHEAD));
        stand.sendToWork(point, KICK_TRAVEL_SPEED);
        stand.triggerStrikeForward("sweep_kick");
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 0.8F);
        StandBeat.at(KICK_CONTACT_TICKS, player, () -> {
            double radius = KICK_RADIUS;
            broadcast(level, point, MagicianFxPacket.at(MagicianFxPacket.KICK, 0, point, (float) radius, 8));
            AABB reach = new AABB(point.subtract(radius, 1.0, radius), point.add(radius, 2.0, radius));
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach, entity -> foe(player, entity))) {
                Vec3 out = new Vec3(target.getX() - point.x, 0.0, target.getZ() - point.z);
                if (out.length() > radius) {
                    continue;
                }
                if (strike(player, target, blow(player, target, "sweep_kick", KICK_DAMAGE))) {
                    Stun.stagger(target);
                }
                Vec3 shove = out.lengthSqr() < 1.0E-6 ? Vec3.ZERO : out.normalize().scale(KICK_SHOVE);
                target.setDeltaMovement(target.getDeltaMovement().add(shove.x, KICK_LIFT, shove.z));
                target.hurtMarked = true;
                Impacts.landed(level, target, new Vec3(out.x, 0.4, out.z), ImpactSpec.MEDIUM, FLAME);
            }
        });
        comeBack(player, KICK_CONTACT_TICKS + RETURN_AFTER_TICKS);
        return true;
    }

    public static boolean containment(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        for (Barrier old : BARRIERS) {
            if (old.owner == player) {
                old.gone = true;
            }
        }
        Barrier barrier = new Barrier(nextId++, player,
                level.getGameTime() + StandTuning.ticks("containment_ticks", BARRIER_TICKS));
        BARRIERS.add(barrier);
        stand.triggerStrikeForward("controlled_blaze");
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        StandBeat.during(BARRIER_CONTACT_TICKS, BARRIER_CONTACT_TICKS + BARRIER_DRAW_TICKS, player,
                tick -> extend(player, barrier));
        return true;
    }

    private static void extend(ServerPlayer player, Barrier barrier) {
        if (barrier.gone || player.level() != barrier.level) {
            return;
        }
        Vec3 aim = ground(barrier.level, aimPoint(player, BARRIER_REACH).add(0.0, 0.5, 0.0));
        if (barrier.points.isEmpty()) {
            raise(barrier, aim);
            return;
        }
        for (int step = 0; step < BARRIER_STEPS_PER_TICK && barrier.length < BARRIER_LENGTH; step++) {
            Vec3 last = barrier.points.get(barrier.points.size() - 1);
            Vec3 toward = new Vec3(aim.x - last.x, 0.0, aim.z - last.z);
            if (toward.length() < BARRIER_STEP) {
                return;
            }
            Vec3 next = last.add(toward.normalize().scale(BARRIER_STEP));
            raise(barrier, ground(barrier.level, new Vec3(next.x, Math.max(last.y, aim.y) + 1.5, next.z)));
            barrier.length += BARRIER_STEP;
        }
    }

    private static void raise(Barrier barrier, Vec3 point) {
        barrier.points.add(point);
        MagicianFlames.kindle(barrier.level, point, MagicianFlames.WALL_SPREAD);
        broadcast(barrier.level, point, MagicianFxPacket.at(MagicianFxPacket.WALL, barrier.id, point, 0F,
                (int) Math.max(1, barrier.until - barrier.level.getGameTime())));
        barrier.level.playSound(null, BlockPos.containing(point), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                0.35F, 0.6F + barrier.points.size() * 0.03F);
    }

    private static boolean hold(Barrier barrier) {
        long now = barrier.level.getGameTime();
        if (barrier.gone || !barrier.owner.isAlive() || barrier.owner.level() != barrier.level || now >= barrier.until) {
            barrier.gone = true;
            Vec3 at = barrier.points.isEmpty() ? barrier.owner.position() : barrier.points.get(0);
            broadcast(barrier.level, at, MagicianFxPacket.at(MagicianFxPacket.WALL_END, barrier.id, at, 0F, 0));
            return true;
        }
        if (now % WALL_EVERY != 0 || barrier.points.size() < 2) {
            return false;
        }
        AABB reach = new AABB(barrier.points.get(0), barrier.points.get(0));
        for (Vec3 point : barrier.points) {
            reach = reach.minmax(new AABB(point, point));
        }
        reach = reach.inflate(WALL_THICKNESS, 0.0, WALL_THICKNESS).expandTowards(0.0, WALL_HEIGHT, 0.0)
                .expandTowards(0.0, -1.0, 0.0);
        float amount = StandTuning.damage("containment", WALL_DAMAGE);
        for (LivingEntity target : barrier.level.getEntitiesOfClass(LivingEntity.class, reach,
                entity -> foe(barrier.owner, entity))) {
            Vec3 away = offWall(barrier, target.position());
            if (away == null) {
                continue;
            }
            target.hurt(barrier.owner.damageSources().playerAttack(barrier.owner),
                    DamageScale.stand(barrier.owner, target, amount));
            Burning.apply(target);
            target.setDeltaMovement(target.getDeltaMovement().add(away.x * BARRIER_SHOVE, 0.1, away.z * BARRIER_SHOVE));
            target.hurtMarked = true;
        }
        return false;
    }

    private static Vec3 offWall(Barrier barrier, Vec3 at) {
        double best = WALL_THICKNESS;
        Vec3 away = null;
        for (int i = 0; i + 1 < barrier.points.size(); i++) {
            Vec3 a = barrier.points.get(i);
            Vec3 b = barrier.points.get(i + 1);
            double low = Math.min(a.y, b.y) - 1.0;
            double high = Math.max(a.y, b.y) + WALL_HEIGHT;
            if (at.y < low || at.y > high) {
                continue;
            }
            double sx = b.x - a.x;
            double sz = b.z - a.z;
            double span = sx * sx + sz * sz;
            double along = span < 1.0E-6 ? 0.0 : Mth.clamp(((at.x - a.x) * sx + (at.z - a.z) * sz) / span, 0.0, 1.0);
            double ox = at.x - (a.x + sx * along);
            double oz = at.z - (a.z + sz * along);
            double gap = Math.sqrt(ox * ox + oz * oz);
            if (gap <= best) {
                best = gap;
                away = gap < 1.0E-3 ? new Vec3(-sz, 0.0, sx).normalize() : new Vec3(ox / gap, 0.0, oz / gap);
            }
        }
        return away;
    }

    private static void launch(ServerPlayer player, float damage, String key, double spread) {
        StandEntity stand = stand(player);
        if (stand == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 mouth = stand.position().add(0.0, stand.getBbHeight() * 0.8, 0.0).add(look.scale(0.6));
        Vec3 heading = aimPoint(player, ANKH_REACH).subtract(mouth);
        heading = heading.lengthSqr() < 1.0E-4 ? look : heading.normalize();
        if (spread != 0.0) {
            double cos = Math.cos(spread);
            double sin = Math.sin(spread);
            heading = new Vec3(heading.x * cos - heading.z * sin, heading.y, heading.x * sin + heading.z * cos);
        }
        Vec3 velocity = heading.scale(ANKH_SPEED);
        Ankh ankh = new Ankh(nextId++, player, mouth, velocity, damage, key);
        ANKHS.add(ankh);
        broadcast(level, mouth, new MagicianFxPacket(MagicianFxPacket.ANKH, ankh.id, mouth.x, mouth.y, mouth.z,
                (float) velocity.x, (float) velocity.y, (float) velocity.z, damage >= ANKH_DAMAGE ? 1.0F : 0.85F,
                ANKH_TICKS));
        level.playSound(null, BlockPos.containing(mouth), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F,
                0.9F + level.random.nextFloat() * 0.2F);
    }

    public static boolean crossfire(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        stand.triggerStrikeForward("cross_fire_hurricane");
        voice(player, ModSounds.MR_CROSSFIRE.get(), 1.0F);
        StandBeat.at(ANKH_CONTACT_TICKS, player, () -> launch(player, ANKH_DAMAGE, "crossfire_hurricane", 0.0));
        return true;
    }

    public static boolean crossfireSpecial(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        stand.triggerStrikeForward("cross_fire_hurricane_special");
        voice(player, ModSounds.MR_CROSSFIRE.get(), 0.9F);
        int last = SPECIAL_ANKH_CONTACT_TICKS + (SPECIAL_ANKHS - 1) * SPECIAL_EVERY;
        StandBeat.during(SPECIAL_ANKH_CONTACT_TICKS, last, player, tick -> {
            if ((tick - SPECIAL_ANKH_CONTACT_TICKS) % SPECIAL_EVERY == 0) {
                int index = (tick - SPECIAL_ANKH_CONTACT_TICKS) / SPECIAL_EVERY;
                launch(player, SPECIAL_ANKH_DAMAGE, "crossfire_hurricane_special",
                        (index - (SPECIAL_ANKHS - 1) / 2.0) * SPECIAL_FAN);
            }
        });
        return true;
    }

    public static boolean redBind(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity target = StandAim.lookTarget(player, BIND_REACH);
        if (target == null || !foe(player, target)) {
            return false;
        }
        stand.triggerStrikeAt(target, "red_bind");
        voice(player, ModSounds.MR_RED_BIND.get(), 1.0F);
        StandBeat.after(BIND_CONTACT_TICKS, player, target, (binder, bound) -> {
            int ticks = StandTuning.ticks("red_bind_ticks", BIND_TICKS);
            Stun.lock(bound, ticks);
            Burning.apply(bound);
            MagicianFlames.kindle(binder.serverLevel(), bound.position(), 1);
            JojohaPlayerData bond = PlayerDataAccess.get(binder);
            bond.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
            PlayerDataAccess.set(binder, bond);
            broadcast(binder.serverLevel(), bound.position(), MagicianFxPacket.at(MagicianFxPacket.BIND,
                    bound.getId(), bound.position(), bound.getBbWidth(), ticks));
            binder.serverLevel().playSound(null, bound.blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.8F, 1.3F);
        });
        return true;
    }

    public static boolean beginInferno(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        int ticks = StandTuning.ticks("inferno_ticks", INFERNO_TICKS);
        INFERNO_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
        stand.triggerStrikeForward("inferno");
        voice(player, ModSounds.MR_HEAVY.get(), 1.0F);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS,
                0.9F, 0.6F);
        broadcast(player.serverLevel(), player.position(), MagicianFxPacket.at(MagicianFxPacket.INFERNO,
                stand.getId(), stand.position(), 0F, ticks));
        return true;
    }

    public static boolean lifeSense(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        stand.triggerStrikeForward("life_sense");
        voice(player, ModSounds.MR_DETECTOR.get(), 1.0F);
        Vec3 at = stand.position();
        broadcast(player.serverLevel(), at, new MagicianFxPacket(MagicianFxPacket.RADAR, stand.getId(), at.x, at.y, at.z,
                player.getId(), 0F, 0F, (float) SENSE_RADIUS, StandTuning.ticks("life_sense_ticks", SENSE_TICKS)));
        return true;
    }

    public static boolean increaseTemperature(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        if (!player.mayBuild()) {
            return false;
        }
        Vec3 centre = stand.position().add(0.0, 0.5, 0.0);
        BlockPos middle = BlockPos.containing(centre);
        int reach = (int) Math.ceil(HEAT_RADIUS);
        List<BlockPos> near = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-reach, -reach, -reach), middle.offset(reach, reach, reach))) {
            if (pos.distToCenterSqr(centre) <= HEAT_RADIUS * HEAT_RADIUS) {
                near.add(pos.immutable());
            }
        }
        near.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(centre)));

        int changed = 0;
        for (BlockPos pos : near) {
            if (changed >= HEAT_BLOCKS) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0F
                    || !exposed(level, pos) || !level.mayInteract(player, pos)) {
                continue;
            }
            BlockState heated = heated(level, state);
            if (heated == null) {
                continue;
            }
            level.setBlockAndUpdate(pos, heated);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2,
                    0.25, 0.1, 0.25, 0.01);
            changed++;
        }
        if (changed == 0) {
            return false;
        }
        stand.triggerStrikeForward("inferno");
        level.playSound(null, middle, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, middle, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 0.6F);
        broadcast(level, centre, MagicianFxPacket.at(MagicianFxPacket.HEAT, 0, centre, (float) HEAT_RADIUS, 12));
        return true;
    }

    private static boolean exposed(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (!level.getBlockState(next).isSolidRender(level, next)) {
                return true;
            }
        }
        return false;
    }

    private static BlockState heated(ServerLevel level, BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD) || state.is(Blocks.MOSS_BLOCK)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.is(Blocks.DIRT)) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return Blocks.WATER.defaultBlockState();
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return Blocks.ICE.defaultBlockState();
        }
        if (state.is(Blocks.BLUE_ICE)) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)
                || state.is(BlockTags.LEAVES) || state.getBlock() instanceof BushBlock || state.is(Blocks.COBWEB)
                || state.is(Blocks.VINE) || state.is(BlockTags.WOOL_CARPETS)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.is(Blocks.WET_SPONGE)) {
            return Blocks.SPONGE.defaultBlockState();
        }
        if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA) || state.is(Blocks.WATER) || state.is(Blocks.FIRE)) {
            return null;
        }
        ItemStack item = new ItemStack(state.getBlock().asItem());
        if (item.isEmpty()) {
            return null;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(item), level)
                .map(recipe -> recipe.value().getResultItem(level.registryAccess()))
                .filter(result -> result.getItem() instanceof BlockItem)
                .map(result -> ((BlockItem) result.getItem()).getBlock().defaultBlockState())
                .orElse(null);
    }

    public static boolean infiniteInferno(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position();
        stand.triggerStrikeForward("infinite_inferno_fire");
        voice(player, ModSounds.MR_ULT.get(), 1.0F);
        StandBeat.at(DOME_CONTACT_TICKS, player, () -> {
            open(player, centre, StandTuning.value("infinite_inferno_fire_radius", (float) DOME_RADIUS),
                    StandTuning.ticks("infinite_inferno_fire_ticks", DOME_TICKS), true);
            level.playSound(null, BlockPos.containing(centre), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.5F);
            level.playSound(null, BlockPos.containing(centre), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                    0.7F, 0.7F);
        });
        return true;
    }

    private static void tick(MinecraftServer server) {
        ANKHS.removeIf(Magician::fly);
        BARRIERS.removeIf(Magician::hold);

        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone zone = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(zone.owner);
            long now = zone.level.getGameTime();
            if (owner == null || !owner.isAlive() || owner.level() != zone.level || now >= zone.until) {
                broadcast(zone.level, zone.centre, MagicianFxPacket.at(MagicianFxPacket.ZONE_END, zone.id, zone.centre, 0F, 0));
                it.remove();
                continue;
            }
            JojohaPlayerData data = PlayerDataAccess.get(owner);
            boolean inside = zone.holds(owner);
            if (now % ZONE_SYNC_EVERY == 0) {
                if (inside && zone.radius < zone.base * (1F + ENCROACH_SHARE)
                        && StandPassives.grants(data, MagicianPassive.ENCROACHMENT.id())) {
                    zone.radius = Math.min(zone.base * (1F + ENCROACH_SHARE), zone.radius + ENCROACH_STEP);
                }
                broadcast(zone.level, zone.centre, zone.packet(now));
            }
            if (inside && now % COMFORT_EVERY == 0) {
                comfort(owner, data);
            }
            scorch(zone, owner, now);
        }

        if (!INFERNO_UNTIL.isEmpty() && server.getTickCount() % 100 == 0) {
            INFERNO_UNTIL.entrySet().removeIf(entry -> {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                return player == null || player.level().getGameTime() >= entry.getValue();
            });
        }
    }

    private static void comfort(ServerPlayer owner, JojohaPlayerData data) {
        if (StandPassives.grants(data, MagicianPassive.TERRITORIAL.id())) {
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, COMFORT_TICKS, 0, true, false, false));
        }
        if (StandPassives.grants(data, MagicianPassive.FLAME_IMMUNITY.id())) {
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, COMFORT_TICKS, 0, true, false, false));
            owner.clearFire();
        }
    }

    private static void scorch(Zone zone, ServerPlayer owner, long now) {
        double radius = zone.radius;
        if (zone.dome) {
            if (now % DOME_EVERY != 0) {
                return;
            }
            AABB reach = new AABB(zone.centre.subtract(radius, radius, radius), zone.centre.add(radius, radius, radius));
            for (LivingEntity target : zone.level.getEntitiesOfClass(LivingEntity.class, reach,
                    entity -> foe(owner, entity) && zone.holds(entity))) {
                Burning.apply(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ASH_SLOW_TICKS, 0, false, false, true));
            }
            return;
        }
        if (now % WALL_EVERY != 0) {
            return;
        }
        double out = radius + WALL_THICKNESS;
        AABB reach = new AABB(zone.centre.subtract(out, 1.0, out), zone.centre.add(out, WALL_HEIGHT, out));
        float amount = StandTuning.damage("controlled_blaze", WALL_DAMAGE);
        for (LivingEntity target : zone.level.getEntitiesOfClass(LivingEntity.class, reach, entity -> foe(owner, entity))) {
            double dx = target.getX() - zone.centre.x;
            double dz = target.getZ() - zone.centre.z;
            if (Math.abs(Math.sqrt(dx * dx + dz * dz) - radius) > WALL_THICKNESS) {
                continue;
            }
            target.hurt(owner.damageSources().playerAttack(owner), DamageScale.stand(owner, target, amount));
            Burning.apply(target);
        }
    }

    private static boolean fly(Ankh ankh) {
        if (!ankh.owner.isAlive() || ankh.owner.level() != ankh.level || --ankh.left < 0) {
            end(ankh, ankh.at);
            return true;
        }
        Vec3 next = ankh.at.add(ankh.velocity);
        BlockHitResult block = ankh.level.clip(new ClipContext(ankh.at, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ankh.owner));
        Vec3 stop = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : next;
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(ankh.level, ankh.owner, ankh.at, stop,
                new AABB(ankh.at, stop).inflate(1.0), entity -> foe(ankh.owner, entity), ANKH_GIRTH);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            strike(ankh.owner, target, blow(ankh.owner, target, ankh.key, ankh.damage));
            Burning.apply(target);
            Impacts.landed(ankh.level, target, ankh.velocity, ImpactSpec.MEDIUM, FLAME);
            end(ankh, hit.getLocation());
            return true;
        }
        if (block.getType() == HitResult.Type.BLOCK) {
            ankh.level.playSound(null, BlockPos.containing(stop), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    0.5F, 1.4F);
            end(ankh, stop);
            return true;
        }
        ankh.at = next;
        return false;
    }

    private static void end(Ankh ankh, Vec3 at) {
        broadcast(ankh.level, at, MagicianFxPacket.at(MagicianFxPacket.ANKH_END, ankh.id, at, 0F, 0));
        MagicianFlames.kindle(ankh.level, at, MagicianFlames.ANKH_SPREAD);
    }
}
