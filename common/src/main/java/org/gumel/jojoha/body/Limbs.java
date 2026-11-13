package org.gumel.jojoha.body;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Limbs {
    public static final int RIGHT_ARM = 1;

    public static final int LEFT_ARM = 2;

    public static final int RIGHT_LEG = 4;

    public static final int LEFT_LEG = 8;

    public static final int ALL = RIGHT_ARM | LEFT_ARM | RIGHT_LEG | LEFT_LEG;

    public static final int[] EACH = {RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG};

    public static final float HEAVY_HIT = 8F;

    public static final float HEAVY_CHANCE = 0.15F;

    public static final float SLICE_HIT = 5F;

    public static final float SLICE_CHANCE = 0.06F;

    public static final float BLEED_CHANCE = 0.4F;

    public static final int BLEED_AMPLIFIER = 3;

    public static final int REGROW_TICKS = 1200;

    public static final int PILLAR_REGROW_TICKS = 200;

    public static final int ULTIMATE_REGROW_TICKS = 100;

    public static final int GROW_TICKS = 80;

    public static final float MEND_HEALTH = 20F;

    public static final int GRACE_TICKS = 1200;

    private static final Map<UUID, Long> LAST_LOSS = new ConcurrentHashMap<>();

    public static final TagKey<EntityType<?>> SEVERABLE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "severable"));

    private static final ResourceLocation MOB_LIMP = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "limb_limp");

    private Limbs() {
    }

    public static void init() {
        Bleed.init();
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server && server.isAlive()) {
                JojohaPlayerData data = PlayerDataAccess.get(server);
                tick(server, data);
                Body.tick(server, data);
            }
        });
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> refused(player, hand)
                ? CompoundEventResult.interruptFalse(player.getItemInHand(hand))
                : CompoundEventResult.pass());
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                refused(player, hand) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (data.limbs != 0) {
                data.limbs = 0;
                data.limbRegrow = 0;
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(Body::forget);
    }

    public static boolean on() {
        return JojohaServerConfig.LIMB_LOSS.on();
    }

    public static boolean lost(JojohaPlayerData data, int limb) {
        return (data.limbs & limb) != 0;
    }

    public static int legsLost(JojohaPlayerData data) {
        return (lost(data, RIGHT_LEG) ? 1 : 0) + (lost(data, LEFT_LEG) ? 1 : 0);
    }

    public static int armFor(Player player, InteractionHand hand) {
        boolean right = (player.getMainArm() == HumanoidArm.RIGHT) == (hand == InteractionHand.MAIN_HAND);
        return right ? RIGHT_ARM : LEFT_ARM;
    }

    private static boolean refused(Player player, InteractionHand hand) {
        if (player.level().isClientSide() || player.getItemInHand(hand).is(ModItems.ZOMBIE_HORSE.get())) {
            return false;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean armless = lost(data, RIGHT_ARM) && lost(data, LEFT_ARM);
        return lost(data, armFor(player, hand)) && !(armless && mends(player.getItemInHand(hand)));
    }

    private static boolean mends(ItemStack stack) {
        if (stack.is(ModItems.HERBAL_TEA.get())) {
            return true;
        }
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            for (MobEffectInstance effect : potion.getAllEffects()) {
                if (mending(effect)) {
                    return true;
                }
            }
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            for (FoodProperties.PossibleEffect effect : food.effects()) {
                if (mending(effect.effect())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean mending(MobEffectInstance effect) {
        return effect.is(MobEffects.HEAL) || effect.is(MobEffects.REGENERATION);
    }

    public static void mended(LivingEntity entity, float health) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        int limb = nextRegrown(data);
        int needed = regrowTicks(data);
        int cue = needed - GROW_TICKS;
        if (limb == 0 || data.limbRegrow >= cue) {
            return;
        }
        data.limbRegrow = Math.min(cue, data.limbRegrow + Math.round(health * needed / MEND_HEALTH));
        PlayerDataAccess.set(player, data);
        player.displayClientMessage(Component.translatable("message.jojoha.limb.mending",
                Component.translatable(nameKey(limb)), data.limbRegrow * 100 / needed), true);
    }

    public static boolean refusesBlow(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker
                || StandCombatHandler.standBlow(source)) {
            return false;
        }
        return lost(PlayerDataAccess.get(attacker), armFor(attacker, InteractionHand.MAIN_HAND));
    }

    public static boolean severable(LivingEntity entity) {
        return entity instanceof ServerPlayer || entity instanceof Mob && entity instanceof LimbHolder
                && entity.getType().is(SEVERABLE);
    }

    public static int of(LivingEntity entity) {
        if (entity instanceof Player player) {
            return Body.state(player) & ALL;
        }
        return entity instanceof LimbHolder holder ? holder.jojoha$limbs() : 0;
    }

    public static boolean armless(LivingEntity entity) {
        return (of(entity) & (RIGHT_ARM | LEFT_ARM)) == (RIGHT_ARM | LEFT_ARM);
    }

    public static void struck(LivingEntity victim, DamageSource source, float dealt) {
        if (!on() || dealt <= 0F || !combatBlow(source)) {
            return;
        }
        float chance = dealt >= HEAVY_HIT ? HEAVY_CHANCE : 0F;
        if (dealt >= SLICE_HIT && slicing(source)) {
            chance += SLICE_CHANCE;
        }
        if (chance > 0F && victim.getRandom().nextFloat() < chance) {
            sever(victim, source);
        }
    }

    public static void bled(LivingEntity victim, int amplifier) {
        if (on() && amplifier >= BLEED_AMPLIFIER && victim.isAlive() && severable(victim)
                && victim.getRandom().nextFloat() < BLEED_CHANCE) {
            sever(victim, null);
        }
    }

    private static boolean combatBlow(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && !source.is(DamageTypeTags.IS_EXPLOSION)
                && !source.is(DamageTypeTags.IS_FALL) && !source.is(DamageTypeTags.WITCH_RESISTANT_TO);
    }

    private static boolean slicing(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
            return false;
        }
        if (attacker instanceof ServerPlayer player && StandCombatHandler.standBlow(source)) {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            return data.stand.isPresent() && StandTypes.SILVER_CHARIOT_ID.equals(data.stand.standId());
        }
        ItemStack held = attacker.getMainHandItem();
        return held.is(ItemTags.SWORDS) || held.is(ItemTags.AXES);
    }

    public static void sever(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayer player && graced(player)) {
            return;
        }
        int had = of(victim);
        List<Integer> intact = new ArrayList<>();
        for (int limb : EACH) {
            if ((had & limb) == 0) {
                intact.add(limb);
            }
        }
        if (intact.isEmpty()) {
            return;
        }
        int limb = intact.get(victim.getRandom().nextInt(intact.size()));
        Vec3 away = source == null || source.getEntity() == null ? Vec3.ZERO
                : victim.position().subtract(source.getEntity().position());
        SeveredLimb.fling(victim, limb, victim.position().add(0, victim.getBbHeight()
                * (limb == RIGHT_ARM || limb == LEFT_ARM ? 0.6 : 0.25), 0), away);
        NetworkHandler.broadcastFlair(victim, FlairCuePacket.SEVER, limb);
        victim.level().playSound(null, victim.blockPosition(), ModSounds.LIMB_SEVERED.get(),
                victim.getSoundSource(), 1.0F, 0.95F + victim.getRandom().nextFloat() * 0.1F);
        if (victim instanceof ServerPlayer player) {
            severPlayer(player, limb);
        } else if (victim instanceof Mob mob && mob instanceof LimbHolder holder) {
            severMob(mob, holder, had | limb, limb);
        }
    }

    private static void severMob(Mob mob, LimbHolder holder, int limbs, int limb) {
        holder.jojoha$setLimbs(limbs);
        InteractionHand hand = (mob.getMainArm() == HumanoidArm.RIGHT) == (limb == RIGHT_ARM)
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if ((limb == RIGHT_ARM || limb == LEFT_ARM) && !mob.getItemInHand(hand).isEmpty()) {
            mob.spawnAtLocation(mob.getItemInHand(hand).copy());
            mob.setItemInHand(hand, ItemStack.EMPTY);
        }
        int legs = ((limbs & RIGHT_LEG) != 0 ? 1 : 0) + ((limbs & LEFT_LEG) != 0 ? 1 : 0);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && legs > 0) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(MOB_LIMP, legs * Body.LIMP_SLOW,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static boolean graced(ServerPlayer player) {
        Long last = LAST_LOSS.get(player.getUUID());
        return last != null && player.level().getGameTime() - last < GRACE_TICKS;
    }

    private static void severPlayer(ServerPlayer player, int limb) {
        LAST_LOSS.put(player.getUUID(), player.level().getGameTime());
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.limbs |= limb;
        data.limbRegrow = 0;
        PlayerDataAccess.set(player, data);
        for (InteractionHand hand : InteractionHand.values()) {
            if (armFor(player, hand) == limb && !player.getItemInHand(hand).isEmpty()) {
                player.drop(player.getItemInHand(hand).copy(), true);
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }
        Speech.say(player, "speech.jojoha.limb.lost", ChatFormatting.RED, Component.translatable(nameKey(limb)));
        PlayerDataAccess.sync(player);
    }

    public static int regrowTicks(JojohaPlayerData data) {
        return Fate.ultimate(data) ? ULTIMATE_REGROW_TICKS : Fate.pillar(data) ? PILLAR_REGROW_TICKS : REGROW_TICKS;
    }

    private static int nextRegrown(JojohaPlayerData data) {
        for (int limb : EACH) {
            if (lost(data, limb)) {
                return limb;
            }
        }
        return 0;
    }

    public static String nameKey(int limb) {
        return "limb.jojoha." + switch (limb) {
            case RIGHT_ARM -> "right_arm";
            case LEFT_ARM -> "left_arm";
            case RIGHT_LEG -> "right_leg";
            default -> "left_leg";
        };
    }

    private static void tick(ServerPlayer player, JojohaPlayerData data) {
        if (data.limbs == 0) {
            return;
        }
        if (legsLost(data) > 0 && player.isSprinting()) {
            player.setSprinting(false);
        }
        int needed = regrowTicks(data);
        if (!Fate.undead(data) && data.limbRegrow < needed - GROW_TICKS) {
            return;
        }
        int limb = nextRegrown(data);
        if (data.limbRegrow == needed - GROW_TICKS) {
            NetworkHandler.broadcastFlair(player, FlairCuePacket.REGROW, limb);
        }
        if (++data.limbRegrow < needed) {
            PlayerDataAccess.set(player, data);
            return;
        }
        data.limbs &= ~limb;
        Speech.say(player, "speech.jojoha.limb.regrown", ChatFormatting.GREEN, Component.translatable(nameKey(limb)));
        data.limbRegrow = 0;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }
}
