package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.entity.StandKnife;
import org.gumel.jojoha.item.DaggerItem;
import org.gumel.jojoha.item.WeaponClass;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KnifeBarrageSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "knife_barrage");
    public static final KnifeBarrageSkill INSTANCE = new KnifeBarrageSkill();

    public static final int KNIVES = 8;
    private static final int RUPTURE_KNIVES = 3;
    private static final float KNIFE_DAMAGE = 2.0F;
    private static final double SPEED = 2.2;

    public static final int FOCUS_TICKS = 12;

    private static final int RELEASE_TICKS = 14;

    private static final float ARC_DEGREES = 70F;
    private static final float ARC_INACCURACY = 1.0F;

    private static final double FOCUS_REACH = 24.0;
    private static final double FOCUS_SPREAD = 2.4;

    private static final double VOLLEY_STARTUP_SECONDS = 0.25;
    private static final double VOLLEY_THROW_SECONDS = 0.5;
    private static final double VOLLEY_RELEASE_SECONDS = 0.1667;

    public static final int VOLLEY_EVERY_TICKS = 6;

    public static final double VOLLEY_ANIM_SPEED =
            (VOLLEY_STARTUP_SECONDS + VOLLEY_THROW_SECONDS) * 20.0 / VOLLEY_EVERY_TICKS;

    public static final int VOLLEY_RELEASE_TICKS = Math.max(1,
            (int) Math.round((VOLLEY_STARTUP_SECONDS + VOLLEY_RELEASE_SECONDS) * 20.0 / VOLLEY_ANIM_SPEED));

    private static final double VOLLEY_HAND_SIDE = 0.35;

    private static final double QUICK_SPREAD = 1.6;

    private static final double QUICK_RISE = 0.2;

    private static final int COOLDOWN_TICKS = 140;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private KnifeBarrageSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.knife_barrage";
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
    public int chargeMaxTicks() {
        return FOCUS_TICKS;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, 0);
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return carriesKnives(player) ? null : "message.jojoha.skill.no_knives";
    }

    public static boolean carriesKnives(ServerPlayer player) {
        return player.getAbilities().instabuild || countKnives(player) > 0;
    }

    private static int countKnives(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int found = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isKnife(inventory.getItem(i))) {
                found += inventory.getItem(i).getCount();
            }
        }
        return found;
    }

    private static boolean isKnife(ItemStack stack) {
        return WeaponClasses.of(stack) == WeaponClass.DAGGER;
    }

    private static List<ItemStack> spendKnives(ServerPlayer player,
                                                                              int wanted) {
        Inventory inventory = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isKnife(inventory.getItem(i))) {
                slots.add(i);
            }
        }
        slots.sort(Comparator.comparingDouble(slot -> worth(inventory.getItem(slot))));

        List<ItemStack> taken = new ArrayList<>();
        boolean creative = player.getAbilities().instabuild;
        for (int slot : slots) {
            ItemStack stack = inventory.getItem(slot);
            int take = Math.min(wanted - taken.size(), stack.getCount());
            for (int i = 0; i < take; i++) {
                taken.add(stack.copyWithCount(1));
            }
            if (!creative) {
                stack.shrink(take);
            }
            if (taken.size() >= wanted) {
                break;
            }
        }

        if (creative) {
            List<ItemStack> carried = new ArrayList<>(taken);
            for (int i = taken.size(); i < wanted; i++) {
                taken.add(carried.isEmpty()
                        ? new ItemStack(ModItems.DAGGER_IRON.get())
                        : carried.get(i % carried.size()).copy());
            }
        }
        return taken;
    }

    private static float worth(ItemStack stack) {
        return stack.getItem() instanceof DaggerItem dagger
                ? dagger.throwDamage() : 0F;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                            int chargeTicks) {
        if (stand == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        float perKnife = DamageScale.stand(player, null,
                StandTuning.damage("knife_barrage", KNIFE_DAMAGE) * data.stand.powerScale());
        double base = perKnife / SPEED;

        if (TimeStopThrow.throwing(data)) {
            return quickCast(player, stand, base);
        }

        if (player.isShiftKeyDown()) {
            return volley(player, stand, base);
        }

        Vec3 look = player.getLookAngle();
        Vec3 sideways = look.cross(new Vec3(0.0, 1.0, 0.0));
        final Vec3 right = sideways.lengthSqr() < 1.0E-4
                ? new Vec3(1.0, 0.0, 0.0)
                : sideways.normalize();
        final Vec3 up = right.cross(look).normalize();
        Vec3 chest = stand.position().add(0.0, stand.getBbHeight() * 0.7, 0.0).add(look.scale(0.4));

        boolean focused = chargeTicks >= FOCUS_TICKS;
        Vec3 aim = null;
        if (focused) {
            LivingEntity target = SkillTargeting.lookTarget(player, FOCUS_REACH);
            aim = target != null
                    ? target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
                    : player.getEyePosition().add(look.scale(FOCUS_REACH));
            if (target != null) {
                StandFx.ring(level, aim, 1.2, 1);
                level.sendParticles(ParticleTypes.CRIT, aim.x, aim.y, aim.z, 10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        final List<ItemStack> throwing = spendKnives(player, KNIVES);
        if (throwing.isEmpty()) {
            return false;
        }
        stand.setKnivesHeld(throwing);
        stand.triggerKnifeThrowPose();

        final Vec3 aimAt = aim;
        StandBeat.after(RELEASE_TICKS, player, player,
                (thrower, self) -> loose(thrower, stand, throwing, focused, aimAt, base, chest, look, right, up));

        level.playSound(null, stand.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.7F, 1.4F);
        return true;
    }

    private static boolean volley(ServerPlayer player, StandEntity stand, double base) {
        final List<ItemStack> throwing = spendKnives(player, KNIVES);
        if (throwing.isEmpty()) {
            return false;
        }
        int count = throwing.size();
        stand.setKnivesHeld(throwing);
        stand.triggerKnifeVolleyPose(count);

        int last = (count - 1) * VOLLEY_EVERY_TICKS + VOLLEY_RELEASE_TICKS;
        StandBeat.during(VOLLEY_RELEASE_TICKS, last, player, tick -> {
            int since = tick - VOLLEY_RELEASE_TICKS;
            if (since % VOLLEY_EVERY_TICKS != 0) {
                return;
            }
            int knife = since / VOLLEY_EVERY_TICKS;
            if (knife < count) {
                stand.setKnivesHeld(throwing.subList(knife + 1, count));
                flick(player, stand, throwing.get(knife), base);
            }
        });

        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    private static boolean quickCast(ServerPlayer player, StandEntity stand, double base) {
        List<ItemStack> throwing = spendKnives(player, KNIVES);
        if (throwing.isEmpty()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 sideways = look.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 right = sideways.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : sideways.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 chest = stand.position().add(0.0, stand.getBbHeight() * 0.7, 0.0).add(look.scale(0.4));
        Vec3 aim = TimeStopThrow.aim(player);

        int count = throwing.size();
        StandKnife.Volley volley = count >= RUPTURE_KNIVES ? new StandKnife.Volley(count) : null;
        for (int i = 0; i < count; i++) {
            float across = count == 1 ? 0F : i / (float) (count - 1) - 0.5F;
            Vec3 from = chest.add(right.scale(across * QUICK_SPREAD))
                    .add(up.scale((i % 2 == 0 ? 1 : -1) * QUICK_RISE));
            Vec3 heading = aim.subtract(from);
            if (heading.lengthSqr() < 1.0E-4) {
                heading = look;
            }
            StandKnife knife = new StandKnife(level, player, (float) base, throwing.get(i));
            knife.partOf(volley);
            knife.piercing();
            knife.setPos(from.x, from.y, from.z);
            knife.shoot(heading.x, heading.y, heading.z, (float) SPEED, 0F);
            level.addFreshEntity(knife);
        }

        stand.setKnivesHeld(0);
        stand.triggerKnifeThrowPose();
        level.playSound(null, stand.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS,
                1.0F, 1.1F);
        return true;
    }

    private static void flick(ServerPlayer player, StandEntity stand, ItemStack knife,
                              double base) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 sideways = look.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 right = sideways.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : sideways.normalize();
        Vec3 hand = stand.position().add(0.0, stand.getBbHeight() * 0.7, 0.0)
                .add(look.scale(0.4)).add(right.scale(VOLLEY_HAND_SIDE));

        LivingEntity target = SkillTargeting.lookTarget(player, FOCUS_REACH);
        Vec3 aim = target != null
                ? target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
                : player.getEyePosition().add(look.scale(FOCUS_REACH));
        Vec3 heading = aim.subtract(hand);
        if (heading.lengthSqr() < 1.0E-4) {
            heading = look;
        }

        StandKnife thrown = new StandKnife(level, player, (float) base, knife);
        thrown.setPos(hand.x, hand.y, hand.z);
        thrown.shoot(heading.x, heading.y, heading.z, (float) SPEED, 0F);
        level.addFreshEntity(thrown);

        level.playSound(null, stand.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS,
                0.7F, 1.3F + level.getRandom().nextFloat() * 0.2F);
    }

    public static void throwAt(ServerPlayer owner, StandEntity stand, LivingEntity target) {
        if (stand == null || target == null || !carriesKnives(owner)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(owner);
        float perKnife = DamageScale.stand(owner, null,
                StandTuning.damage("knife_barrage", KNIFE_DAMAGE) * data.stand.powerScale());
        final double base = perKnife / SPEED;

        final List<ItemStack> throwing = spendKnives(owner, KNIVES);
        if (throwing.isEmpty()) {
            return;
        }
        stand.setKnivesHeld(throwing);
        stand.triggerKnifeThrowPose();

        StandBeat.after(RELEASE_TICKS, owner, owner, (thrower, self) -> {
            if (!target.isAlive() || target.level() != stand.level()) {
                stand.setKnivesHeld(0);
                return;
            }

            Vec3 chest = stand.position().add(0.0, stand.getBbHeight() * 0.7, 0.0);
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
            Vec3 toTarget = aim.subtract(chest);
            Vec3 look = toTarget.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : toTarget.normalize();

            Vec3 sideways = look.cross(new Vec3(0.0, 1.0, 0.0));
            Vec3 right = sideways.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : sideways.normalize();
            Vec3 up = right.cross(look).normalize();

            loose(thrower, stand, throwing, true, aim, base, chest.add(look.scale(0.4)), look, right, up);
        });

        owner.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private static void loose(ServerPlayer player, StandEntity stand,
                              List<ItemStack> knives, boolean focused,
                              Vec3 aim, double base, Vec3 chest, Vec3 look, Vec3 right, Vec3 up) {
        ServerLevel level = player.serverLevel();
        stand.setKnivesHeld(0);

        int count = knives.size();
        StandKnife.Volley volley = count >= RUPTURE_KNIVES ? new StandKnife.Volley(count) : null;
        for (int i = 0; i < count; i++) {
            float across = count == 1 ? 0F : i / (float) (count - 1) - 0.5F;
            StandKnife knife = new StandKnife(level, player, (float) base, knives.get(i));
            knife.partOf(volley);
            if (focused) {
                knife.wounding();
            }
            Vec3 from;
            Vec3 heading;
            float inaccuracy;
            if (focused) {
                from = chest.add(right.scale(across * FOCUS_SPREAD))
                        .add(up.scale(Mth.cos(across * Mth.PI) * 0.35 - 0.15));
                heading = aim.subtract(from).normalize();
                inaccuracy = 0F;
            } else {
                from = chest.add(right.scale(across * 0.8));
                heading = rotateAbout(look, new Vec3(0.0, 1.0, 0.0), across * ARC_DEGREES * Mth.DEG_TO_RAD);
                inaccuracy = ARC_INACCURACY;
            }
            knife.setPos(from.x, from.y, from.z);
            knife.shoot(heading.x, heading.y, heading.z, (float) SPEED, inaccuracy);
            level.addFreshEntity(knife);
        }

        level.playSound(null, stand.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS,
                1.0F, focused ? 0.8F : 1.2F);
    }

    private static Vec3 rotateAbout(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }
}
