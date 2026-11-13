package org.gumel.jojoha.ultimate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.DamageScale;

import java.util.ArrayList;
import java.util.List;

public final class AdaptationKit {
    private static final double AIM_ASSIST = 0.96;

    private AdaptationKit() {
    }

    static boolean needsTarget(Adaptation adaptation) {
        return switch (adaptation) {
            case WEB_SHOT, VENOM_WEB, GUARDIAN_BEAM, ELDER_BEAM, SHULKER_BULLET, HEAVE, STING, TONGUE_LASH -> true;
            default -> false;
        };
    }

    static double reachOf(Adaptation adaptation) {
        return switch (adaptation) {
            case WEB_SHOT, VENOM_WEB -> 20.0;
            case GUARDIAN_BEAM, ELDER_BEAM -> 16.0;
            case SHULKER_BULLET -> 24.0;
            case HEAVE, STING -> 4.0;
            case TONGUE_LASH -> 8.0;
            default -> 16.0;
        };
    }

    public static LivingEntity aimed(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = blocks(player, range).getLocation();

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, to,
                player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0),
                entity -> !entity.isSpectator() && entity.isPickable() && entity != player, range * range);
        if (hit != null) {
            LivingEntity living = living(hit.getEntity());
            if (living != null) {
                return living;
            }
        }

        LivingEntity best = null;
        double bestDot = AIM_ASSIST;
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), entity -> entity != player && entity.isAlive())) {
            Vec3 toward = candidate.getBoundingBox().getCenter().subtract(from);
            double distance = toward.length();
            if (distance > range || distance < 1.0E-3) {
                continue;
            }
            double dot = toward.scale(1.0 / distance).dot(look);
            if (dot > bestDot && player.hasLineOfSight(candidate)) {
                bestDot = dot;
                best = candidate;
            }
        }
        return best;
    }

    private static LivingEntity living(Entity entity) {
        if (entity instanceof EnderDragonPart part) {
            return part.parentMob;
        }
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static BlockHitResult blocks(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition();
        return player.level().clip(new ClipContext(from, from.add(player.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    private static Vec3 impact(ServerPlayer player, double range) {
        LivingEntity target = aimed(player, range);
        if (target != null) {
            return target.getBoundingBox().getCenter();
        }
        return blocks(player, range).getLocation();
    }

    private static List<LivingEntity> around(ServerPlayer player, Vec3 centre, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius), entity -> entity != player && entity.isAlive())) {
            if (entity.getBoundingBox().getCenter().distanceTo(centre) <= radius + entity.getBbWidth() * 0.5) {
                out.add(entity);
            }
        }
        return out;
    }

    private static List<LivingEntity> cone(ServerPlayer player, double range, double minDot) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : around(player, from, range)) {
            Vec3 toward = entity.getBoundingBox().getCenter().subtract(from);
            if (toward.lengthSqr() < 1.0E-6 || toward.normalize().dot(look) >= minDot) {
                out.add(entity);
            }
        }
        return out;
    }

    private static List<LivingEntity> path(ServerPlayer player, Vec3 from, Vec3 to, double width) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(from, to).inflate(width), entity -> entity != player && entity.isAlive())) {
            if (entity.getBoundingBox().inflate(width).clip(from, to).isPresent()) {
                out.add(entity);
            }
        }
        return out;
    }

    private static float falloff(Vec3 centre, LivingEntity entity, double radius, float floor) {
        double distance = entity.getBoundingBox().getCenter().distanceTo(centre);
        return (float) Math.max(floor, 1.0 - distance / radius);
    }

    private static void hit(ServerPlayer player, LivingEntity target, float damage) {
        hit(player, target, player.damageSources().playerAttack(player), damage);
    }

    private static void hit(ServerPlayer player, LivingEntity target, DamageSource source, float damage) {
        target.hurt(source, DamageScale.vampire(player, target, damage));
    }

    private static void knock(LivingEntity target, Vec3 from, double strength, double lift) {
        target.knockback(strength, from.x - target.getX(), from.z - target.getZ());
        if (lift > 0) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, lift, 0.0));
        }
        target.hurtMarked = true;
    }

    private static void afflict(LivingEntity target, Holder<MobEffect> effect, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    private static void launch(ServerPlayer player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));
        player.fallDistance = 0F;
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        Vec3 span = to.subtract(from);
        int points = (int) Math.min(80, Math.ceil(span.length() / step));
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(span.scale(points == 0 ? 0 : (double) i / points));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.serverLevel().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void soundAt(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 turned(Vec3 look, float degrees) {
        return look.yRot((float) Math.toRadians(degrees));
    }

    private static Vec3 muzzle(ServerPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0.0, -0.1, 0.0);
    }

    static void fireVolley(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = muzzle(player);
        for (int i = -1; i <= 1; i++) {
            SmallFireball fireball = new SmallFireball(level, player, turned(player.getLookAngle(), i * 8F).normalize());
            fireball.setPos(at.x, at.y, at.z);
            level.addFreshEntity(fireball);
        }
        sound(player, SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
    }

    static void ghastFireball(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 end = impact(player, 40.0);
        line(level, ParticleTypes.FLAME, muzzle(player), end, 0.6);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (LivingEntity target : around(player, end, 2.5)) {
            hit(player, target, 8F * falloff(end, target, 2.5, 0.5F));
            target.igniteForSeconds(4F);
            knock(target, end, 0.8, 0.3);
        }
        sound(player, SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
        soundAt(level, end, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.0F);
    }

    static void detonate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position().add(0.0, 1.0, 0.0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y, centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (LivingEntity target : around(player, centre, 4.0)) {
            hit(player, target, 9F * falloff(centre, target, 4.0, 0.35F));
            knock(target, centre, 1.2, 0.4);
        }
        sound(player, SoundEvents.CREEPER_PRIMED, 1.0F, 1.2F);
        sound(player, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.0F);
    }

    static void arrows(ServerPlayer player, Holder<MobEffect> effect) {
        ServerLevel level = player.serverLevel();
        for (int i = -1; i <= 1; i++) {
            Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 6F, 0F, 3.0F, 1.0F);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            if (effect != null) {
                arrow.addEffect(new MobEffectInstance(effect, 100, 1));
            }
            level.addFreshEntity(arrow);
        }
        sound(player, SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F);
    }

    static void webShot(ServerPlayer player, boolean venom) {
        LivingEntity target = aimed(player, reachOf(Adaptation.WEB_SHOT));
        if (target == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        line(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                muzzle(player), target.getBoundingBox().getCenter(), 0.5);
        hit(player, target, 3F);
        afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
        if (venom) {
            afflict(target, MobEffects.POISON, 80, 1);
        }
        sound(player, SoundEvents.SPIDER_AMBIENT, 1.0F, 1.3F);
    }

    static void hexFlask(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ThrownPotion potion = new ThrownPotion(level, player);
        potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HARMING));
        potion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20F, 0.5F, 1.0F);
        level.addFreshEntity(potion);
        sound(player, SoundEvents.WITCH_THROW, 1.0F, 1.0F);
    }

    static void fangs(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 heading = new Vec3(look.x, 0.0, look.z);
        heading = heading.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0F, player.getYRot()) : heading.normalize();
        float yaw = (float) Math.atan2(heading.z, heading.x);
        for (int i = 1; i <= 8; i++) {
            double x = player.getX() + heading.x * 1.25 * i;
            double z = player.getZ() + heading.z * 1.25 * i;
            Double floor = floor(level, x, player.getY(), z);
            if (floor != null) {
                level.addFreshEntity(new EvokerFangs(level, x, floor, z, yaw, i * 2, player));
            }
        }
        sound(player, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.0F);
    }

    static Double floor(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y + 2, z);
        for (int i = 0; i < 6; i++) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                double top = 0.0;
                if (!level.isEmptyBlock(pos)) {
                    var shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    if (!shape.isEmpty()) {
                        top = shape.max(Direction.Axis.Y);
                    }
                }
                return pos.getY() + top;
            }
            pos = below;
        }
        return null;
    }

    private static void charge(ServerPlayer player, double distance, double speed, float damage,
                               double knock, Holder<MobEffect> effect, SoundEvent sound) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        Vec3 look = player.getLookAngle();
        Vec3 to = from.add(look.scale(distance));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        for (LivingEntity target : path(player, from, to, 0.6)) {
            hit(player, target, damage);
            knock(target, player.position(), knock, 0.2);
            if (effect != null) {
                afflict(target, effect, 200, 1);
            }
        }
        launch(player, look.scale(speed));
        level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 8, 0.3, 0.3, 0.3, 0.05);
        sound(player, sound, 1.0F, 1.0F);
    }

    static void swoop(ServerPlayer player) {
        charge(player, 8.0, 1.6, 6F, 0.6, null, SoundEvents.PHANTOM_SWOOP);
    }

    static void lunge(ServerPlayer player, boolean famished) {
        charge(player, 5.0, 1.2, 6F, 0.5, famished ? MobEffects.HUNGER : null, SoundEvents.PLAYER_ATTACK_STRONG);
    }

    static void ram(ServerPlayer player) {
        charge(player, 5.0, 1.3, 4F, 2.0, null, SoundEvents.GOAT_RAM_IMPACT);
    }

    static void witheringStrike(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : cone(player, 3.5, 0.5)) {
            hit(player, target, 6F);
            afflict(target, MobEffects.WITHER, 100, 1);
        }
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y - 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 12, 0.6, 0.3, 0.6, 0.01);
        sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
    }

    static void witherSkull(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = aimed(player, 32.0);
        Vec3 end = target != null ? target.getBoundingBox().getCenter() : blocks(player, 32.0).getLocation();
        line(level, ParticleTypes.LARGE_SMOKE, muzzle(player), end, 0.6);
        level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
        if (target != null) {
            hit(player, target, 7F);
            afflict(target, MobEffects.WITHER, 120, 1);
        }
        sound(player, SoundEvents.WITHER_SHOOT, 0.8F, 1.0F);
    }

    static void guardianBeam(ServerPlayer player, boolean elder) {
        LivingEntity target = aimed(player, reachOf(Adaptation.GUARDIAN_BEAM));
        if (target == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        line(level, ParticleTypes.GLOW, muzzle(player), target.getBoundingBox().getCenter(), 0.4);
        hit(player, target, player.damageSources().indirectMagic(player, player), 7F);
        if (elder) {
            afflict(target, MobEffects.DIG_SLOWDOWN, 200, 2);
        }
        sound(player, SoundEvents.GUARDIAN_ATTACK, 1.0F, 1.0F);
    }

    static void shulkerBullet(ServerPlayer player) {
        LivingEntity target = aimed(player, reachOf(Adaptation.SHULKER_BULLET));
        if (target == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ShulkerBullet bullet = new ShulkerBullet(level, player, target, Direction.Axis.Y);
        Vec3 at = muzzle(player);
        bullet.setPos(at.x, at.y, at.z);
        level.addFreshEntity(bullet);
        sound(player, SoundEvents.SHULKER_SHOOT, 1.0F, 1.0F);
    }

    static void windBurst(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WindCharge charge = new WindCharge(player, level, player.getX(), player.getEyeY(), player.getZ());
        charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 1.5F, 1.0F);
        level.addFreshEntity(charge);
        sound(player, SoundEvents.BREEZE_SHOOT, 1.0F, 1.0F);
    }

    static void sonicBoom(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(15.0));
        Vec3 span = to.subtract(from);
        for (int i = 1; i <= 15; i++) {
            Vec3 at = from.add(span.scale(i / 15.0));
            level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (LivingEntity target : path(player, from, to, 1.0)) {
            hit(player, target, player.damageSources().sonicBoom(player), 10F);
            double resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            Vec3 push = span.normalize().multiply(2.5 * (1.0 - resist), 0.5 * (1.0 - resist), 2.5 * (1.0 - resist));
            target.push(push.x, push.y, push.z);
            target.hurtMarked = true;
        }
        sound(player, SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
    }

    static void blink(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 before = player.position();
        BlockHitResult hit = blocks(player, 16.0);
        Vec3 target = hit.getLocation().subtract(player.getLookAngle().scale(0.6));
        BlockPos feet = BlockPos.containing(target.x, target.y - 0.5, target.z);
        for (int up = 0; up < 3; up++) {
            BlockPos at = feet.above(up);
            AABB box = player.getBoundingBox().move(at.getX() + 0.5 - player.getX(), at.getY() - player.getY(),
                    at.getZ() + 0.5 - player.getZ());
            if (level.noCollision(player, box)) {
                level.sendParticles(ParticleTypes.PORTAL, before.x, before.y + 1.0, before.z, 30, 0.3, 0.6, 0.3, 0.3);
                soundAt(level, before, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                player.fallDistance = 0F;
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.3, 0.6, 0.3, 0.3);
                sound(player, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                return;
            }
        }
        sound(player, SoundEvents.ENDERMAN_AMBIENT, 0.6F, 1.4F);
    }

    static void roar(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position();
        for (LivingEntity target : around(player, centre.add(0.0, 1.0, 0.0), 5.0)) {
            hit(player, target, 5F);
            knock(target, centre, 1.6, 0.3);
        }
        level.sendParticles(ParticleTypes.POOF, centre.x, centre.y + 1.0, centre.z, 40, 2.0, 0.4, 2.0, 0.08);
        sound(player, SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
    }

    static void heave(ServerPlayer player) {
        LivingEntity target = aimed(player, reachOf(Adaptation.HEAVE));
        if (target == null) {
            return;
        }
        hit(player, target, 8F);
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, 0.9, motion.z);
        target.hurtMarked = true;
        sound(player, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
    }

    static void slam(ServerPlayer player, boolean magma) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position();
        for (LivingEntity target : around(player, centre.add(0.0, 0.5, 0.0), 3.5)) {
            hit(player, target, 6F);
            knock(target, centre, 0.6, 0.6);
            if (magma) {
                target.igniteForSeconds(3F);
            }
        }
        level.sendParticles(magma ? ParticleTypes.FLAME : ParticleTypes.ITEM_SLIME,
                centre.x, centre.y + 0.2, centre.z, 30, 1.6, 0.1, 1.6, 0.05);
        sound(player, magma ? SoundEvents.MAGMA_CUBE_SQUISH : SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
    }

    static void spit(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = muzzle(player);
        for (int i = -1; i <= 1; i++) {
            LlamaSpit spit = EntityType.LLAMA_SPIT.create(level);
            if (spit == null) {
                continue;
            }
            spit.setOwner(player);
            spit.setPos(at.x, at.y, at.z);
            Vec3 aim = turned(player.getLookAngle(), i * 6F);
            spit.shoot(aim.x, aim.y, aim.z, 1.5F, 2.0F);
            level.addFreshEntity(spit);
        }
        sound(player, SoundEvents.LLAMA_SPIT, 1.0F, 1.0F);
    }

    static void snowballs(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 6; i++) {
            Snowball snowball = new Snowball(level, player);
            snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 1.5F, 6.0F);
            level.addFreshEntity(snowball);
        }
        sound(player, SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 1.0F);
    }

    static void sting(ServerPlayer player) {
        LivingEntity target = aimed(player, reachOf(Adaptation.STING));
        if (target == null) {
            return;
        }
        hit(player, target, 3F);
        afflict(target, MobEffects.POISON, 120, 1);
        sound(player, SoundEvents.BEE_STING, 1.0F, 1.0F);
    }

    static void spikes(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position().add(0.0, 1.0, 0.0);
        for (LivingEntity target : around(player, centre, 3.5)) {
            hit(player, target, 2F);
            afflict(target, MobEffects.POISON, 120, 1);
        }
        level.sendParticles(ParticleTypes.CRIT, centre.x, centre.y, centre.z, 40, 1.4, 0.8, 1.4, 0.2);
        sound(player, SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 1.0F);
    }

    static void trident(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ThrownTrident trident = new ThrownTrident(level, player, new ItemStack(Items.TRIDENT));
        trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0F, 2.5F, 1.0F);
        trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        level.addFreshEntity(trident);
        sound(player, SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F);
    }

    static void cleave(ServerPlayer player, boolean maul) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : cone(player, 3.5, 0.4)) {
            hit(player, target, 7F);
            knock(target, player.position(), 0.5, 0.1);
        }
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y - 0.3, at.z, 2, 0.4, 0.1, 0.4, 0.0);
        sound(player, maul ? SoundEvents.POLAR_BEAR_WARNING : SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, maul ? 1.0F : 0.7F);
    }

    static void tongueLash(ServerPlayer player) {
        LivingEntity target = aimed(player, reachOf(Adaptation.TONGUE_LASH));
        if (target == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        line(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PINK_WOOL.defaultBlockState()),
                muzzle(player), target.getBoundingBox().getCenter(), 0.4);
        hit(player, target, 3F);
        Vec3 pull = player.position().subtract(target.position());
        if (pull.lengthSqr() > 1.0E-4) {
            Vec3 motion = pull.normalize().scale(1.2).add(0.0, 0.3, 0.0);
            target.setDeltaMovement(motion);
            target.hurtMarked = true;
        }
        sound(player, SoundEvents.FROG_TONGUE, 1.0F, 1.0F);
    }

    static void dragonBreath(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 end = impact(player, 24.0);
        line(level, ParticleTypes.DRAGON_BREATH, muzzle(player), end, 0.6);
        AreaEffectCloud cloud = new AreaEffectCloud(level, end.x, end.y - 0.5, end.z);
        cloud.setOwner(player);
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(3.0F);
        cloud.setDuration(80);
        cloud.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
        level.addFreshEntity(cloud);
        sound(player, SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
    }
}
