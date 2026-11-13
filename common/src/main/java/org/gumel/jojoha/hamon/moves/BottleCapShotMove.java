package org.gumel.jojoha.hamon.moves;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class BottleCapShotMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "bottle_cap_shot");
    public static final BottleCapShotMove INSTANCE = new BottleCapShotMove();

    public static final TagKey<Item> CAPPED_BOTTLES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "capped_bottles"));

    public static final float DAMAGE = 5.0F;
    public static final float UNDEAD_DAMAGE = 8.0F;
    public static final double RANGE = 24.0;
    private static final double STEP = 0.5;
    private static final double HIT_RADIUS = 0.4;
    private static final int TRAIL_EVERY = 2;

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = 10F;

    private BottleCapShotMove() {
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

    public static boolean holdsBottle(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(CAPPED_BOTTLES)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        if (!holdsBottle(player)) {
            player.displayClientMessage(Component.translatable("message.jojoha.hamon.no_bottle"), true);
            return false;
        }
        MovementFx.pose(player, MovementFx.THROW);
        StandBeat.at(MovementFx.THROW_RELEASE_TICKS
                - MovementFx.PALM_CONTACT_TICKS, player,
                () -> NetworkHandler.sendFirstPersonPose(player, "palm"));
        StandBeat.at(MovementFx.THROW_RELEASE_TICKS, player,
                () -> flick(player));
        return true;
    }

    private static void flick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        HitResult wall = level.clip(new ClipContext(eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double reach = wall.getType() == HitResult.Type.MISS ? RANGE : eye.distanceTo(wall.getLocation());

        LivingEntity struck = null;
        Vec3 at = eye.add(look.scale(reach));
        int step = 0;
        for (double travelled = 1.0; travelled <= reach && struck == null; travelled += STEP, step++) {
            Vec3 point = eye.add(look.scale(travelled));
            if (step % TRAIL_EVERY == 0) {
                HamonFx.fineSpark(level, point.x, point.y, point.z, 1, 0.02, 0.0);
            }
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(point, point).inflate(HIT_RADIUS),
                    entity -> entity != player && entity.isAlive()
                            && !(entity instanceof StandEntity))) {
                struck = candidate;
                at = point;
                break;
            }
        }

        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET)),
                at.x, at.y, at.z, 4, 0.05, 0.05, 0.05, 0.08);
        HamonFx.ring(level, at.x, at.y, at.z, 0.8, struck == null ? 0 : 1);
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.7F, 1.9F);

        if (struck == null) {
            return;
        }
        float damage = HamonCombat.isUndead(struck) ? UNDEAD_DAMAGE : DAMAGE;
        struck.hurt(level.damageSources().playerAttack(player),
                DamageScale.hamon(player, struck, HamonCombat.scaled(player, damage)));
        Impacts.landed(level, struck, look,
                ImpactSpec.MEDIUM, Impacts.HAMON);
        HamonFx.ripple(level, at.x, at.y, at.z, 3);
        HamonFx.sound(level, player, 0.6F, 1.4F);
    }
}
