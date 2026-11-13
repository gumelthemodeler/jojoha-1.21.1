package org.gumel.jojoha.item;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatGrowth;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Provisions {
    public static final int WILD_DOUBLE_PERCENT = 10;

    public static final int GARLIC_SECONDS = 60;
    public static final double GARLIC_RANGE = 8.0;
    public static final int GARLIC_SICK_SECONDS = 10;

    public static final float STAKE_BONUS = 4F;

    public static final int BRANDY_SECONDS = 30;
    public static final int TEQUILA_SECONDS = 30;
    public static final int HANGOVER_SECONDS = 8;

    public static final int COLA_SECONDS = 30;
    public static final int COFFEE_SECONDS = 60;
    public static final int COFFEE_FOOD = 2;
    public static final float COFFEE_SATURATION = 0.6F;
    public static final int TEA_REGEN_SECONDS = 5;

    public static final float THREAD_HEAL = 4F;

    public static final double PUFF_RADIUS = 3.0;
    public static final int PUFF_TICKS = 120;
    public static final int PUFF_NAUSEA_TICKS = 100;
    private static final int PUFF_EVERY = 5;

    public static final double CROWN_LUCK = 1.0;
    public static final float CROWN_DISCOUNT = 0.1F;

    public static final double RING_SLOW = -0.1;
    public static final float RING_TRAINING = 1.25F;

    private static final ResourceLocation CROWN_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "royal_crown_luck");
    private static final ResourceLocation RING_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heavy_ring_weight");

    private record Puff(ServerLevel level, Vec3 at, long until, UUID owner) {
    }

    private static final List<Puff> PUFFS = new ArrayList<>();

    private static final Map<UUID, Long> GARLIC = new HashMap<>();

    private static final Set<UUID> NAPKINS = new HashSet<>();

    private Provisions() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server) {
                tick(server);
            }
        });
        TickEvent.SERVER_POST.register(server -> puffs());
        PlayerEvent.PLAYER_QUIT.register(player -> {
            GARLIC.remove(player.getUUID());
            NAPKINS.remove(player.getUUID());
        });
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, pitch);
    }

    private static void give(LivingEntity body, Holder<MobEffect> effect,
                             int ticks, int amplifier) {
        body.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, true, true));
    }

    private static void mug(ServerPlayer player) {
        ItemStack mug = new ItemStack(ModItems.EMPTY_MUG.get());
        if (!player.getInventory().add(mug)) {
            player.drop(mug, false);
        }
    }

    public static boolean gem(ServerPlayer player, int stat) {
        if (StatGrowth.raise(player, false, stat, 1, false) <= 0) {
            player.displayClientMessage(Component.translatable("message.jojoha.stat_crystal.full"), true);
            return false;
        }
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
        return true;
    }

    public static boolean wildGem(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        List<Integer> open = new ArrayList<>();
        for (int stat = 0; stat < StatPoints.COUNT; stat++) {
            if (StatPoints.playerStat(data, stat) < StatPoints.MAX_STAT) {
                open.add(stat);
            }
        }
        if (open.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.jojoha.stat_crystal.full"), true);
            return false;
        }
        int levels = player.getRandom().nextInt(100) < WILD_DOUBLE_PERCENT ? 2 : 1;
        StatGrowth.raise(player, false, open.get(player.getRandom().nextInt(open.size())), levels, false);
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, levels == 2 ? 1.6F : 1.0F);
        return true;
    }

    public static boolean garlic(ServerPlayer player) {
        if (Fate.undead(PlayerDataAccess.get(player))) {
            give(player, MobEffects.CONFUSION, GARLIC_SICK_SECONDS * 20, 0);
            player.displayClientMessage(Component.translatable("message.jojoha.garlic.sick"), true);
            return true;
        }
        GARLIC.put(player.getUUID(), player.level().getGameTime() + GARLIC_SECONDS * 20L);
        return true;
    }

    public static boolean brandywine(ServerPlayer player) {
        give(player, MobEffects.DAMAGE_BOOST, BRANDY_SECONDS * 20, 0);
        StandBeat.at(BRANDY_SECONDS * 20, player, () -> give(player, MobEffects.CONFUSION, HANGOVER_SECONDS * 20, 0));
        return true;
    }

    public static boolean tequila(ServerPlayer player) {
        give(player, MobEffects.DAMAGE_RESISTANCE, TEQUILA_SECONDS * 20, 0);
        StandBeat.at(TEQUILA_SECONDS * 20, player, () -> give(player, MobEffects.CONFUSION, HANGOVER_SECONDS * 20, 0));
        return true;
    }

    public static boolean cola(ServerPlayer player) {
        give(player, MobEffects.MOVEMENT_SPEED, COLA_SECONDS * 20, 0);
        return true;
    }

    public static boolean coffee(ServerPlayer player) {
        give(player, MobEffects.DIG_SPEED, COFFEE_SECONDS * 20, 0);
        player.getFoodData().eat(COFFEE_FOOD, COFFEE_SATURATION);
        mug(player);
        return true;
    }

    public static boolean tea(ServerPlayer player) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(ModEffects.bleed());
        give(player, MobEffects.REGENERATION, TEA_REGEN_SECONDS * 20, 0);
        mug(player);
        return true;
    }

    public static boolean thread(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        player.heal(THREAD_HEAL);
        player.removeEffect(ModEffects.bleed());
        for (int limb : Limbs.EACH) {
            if (Limbs.lost(data, limb)) {
                data.limbs &= ~limb;
                data.limbRegrow = 0;
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
                player.displayClientMessage(Component.translatable("message.jojoha.zombie_horse.stitched",
                        Component.translatable(Limbs.nameKey(limb))), true);
                break;
            }
        }
        sound(player, SoundEvents.SHEEP_SHEAR, 1.3F);
        return true;
    }

    public static boolean napkin(ServerPlayer player) {
        if (!NAPKINS.add(player.getUUID())) {
            return false;
        }
        sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F);
        player.displayClientMessage(Component.translatable("message.jojoha.valentines_napkin.blessed"), true);
        return true;
    }

    public static boolean blessed(Player player) {
        return player != null && NAPKINS.remove(player.getUUID());
    }

    public static void puff(ServerPlayer player) {
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        PUFFS.add(new Puff(player.serverLevel(), at, player.level().getGameTime() + PUFF_TICKS, player.getUUID()));
        player.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y, at.z,
                30, 0.8, 0.6, 0.8, 0.02);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                0.5F, 1.6F);
    }

    private static void puffs() {
        if (PUFFS.isEmpty()) {
            return;
        }
        Iterator<Puff> each = PUFFS.iterator();
        while (each.hasNext()) {
            Puff puff = each.next();
            long now = puff.level().getGameTime();
            if (now > puff.until()) {
                each.remove();
                continue;
            }
            if (now % PUFF_EVERY != 0) {
                continue;
            }
            Vec3 at = puff.at();
            puff.level().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y, at.z,
                    6, PUFF_RADIUS * 0.4, PUFF_RADIUS * 0.3, PUFF_RADIUS * 0.4, 0.004);
            puff.level().sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z,
                    8, PUFF_RADIUS * 0.5, PUFF_RADIUS * 0.4, PUFF_RADIUS * 0.5, 0.01);
            AABB box = new AABB(at, at).inflate(PUFF_RADIUS);
            for (LivingEntity body : puff.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity.isAlive() && entity.position().distanceTo(at) <= PUFF_RADIUS + 1.0)) {
                if (!body.getUUID().equals(puff.owner())) {
                    give(body, MobEffects.CONFUSION, PUFF_NAUSEA_TICKS, 0);
                }
            }
            for (Mob mob : puff.level().getEntitiesOfClass(Mob.class, box.inflate(16.0),
                    mob -> mob.getTarget() != null)) {
                LivingEntity target = mob.getTarget();
                if (target.position().distanceTo(at) <= PUFF_RADIUS || mob.position().distanceTo(at) <= PUFF_RADIUS) {
                    mob.setTarget(null);
                }
            }
        }
    }

    public static float incoming(LivingEntity victim, DamageSource source, float amount) {
        if (staked(source) && undead(victim)) {
            return amount + STAKE_BONUS;
        }
        return amount;
    }

    public static boolean staked(DamageSource source) {
        return source.getEntity() instanceof LivingEntity striker && source.getDirectEntity() == striker
                && striker.getMainHandItem().is(ModItems.STAKE.get());
    }

    private static boolean undead(LivingEntity body) {
        if (body instanceof ServerPlayer player) {
            return Fate.undead(PlayerDataAccess.get(player));
        }
        return body.getType().is(EntityTypeTags.UNDEAD);
    }

    public static void crowned(Villager villager, Player player) {
        if (!Accessories.wearing(player, ModItems.ROYAL_CROWN.get())) {
            return;
        }
        for (MerchantOffer offer : villager.getOffers()) {
            offer.addToSpecialPriceDiff(-Math.max(1, Math.round(offer.getBaseCostA().getCount() * CROWN_DISCOUNT)));
        }
    }

    public static int trained(Player player, int amount) {
        return Accessories.wearing(player, ModItems.HEAVY_RING.get())
                ? (int) Math.ceil(amount * RING_TRAINING) : amount;
    }

    private static void tick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            modifier(player, Attributes.LUCK, CROWN_ID,
                    Accessories.wearing(player, ModItems.ROYAL_CROWN.get()) ? CROWN_LUCK : 0.0,
                    AttributeModifier.Operation.ADD_VALUE);
            modifier(player, Attributes.MOVEMENT_SPEED, RING_ID,
                    Accessories.wearing(player, ModItems.HEAVY_RING.get()) ? RING_SLOW : 0.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        Long garlic = GARLIC.get(player.getUUID());
        if (garlic == null) {
            return;
        }
        if (player.level().getGameTime() > garlic) {
            GARLIC.remove(player.getUUID());
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        for (PathfinderMob mob : player.serverLevel().getEntitiesOfClass(PathfinderMob.class,
                player.getBoundingBox().inflate(GARLIC_RANGE),
                mob -> mob.isAlive() && mob.getType().is(EntityTypeTags.UNDEAD))) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            Vec3 away = DefaultRandomPos.getPosAway(mob, 12, 7, player.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            }
        }
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute,
                                 ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    public static boolean sunlit(ServerLevel level, BlockPos pos) {
        return level.isDay() && level.canSeeSky(pos) && !level.isRaining();
    }

}
