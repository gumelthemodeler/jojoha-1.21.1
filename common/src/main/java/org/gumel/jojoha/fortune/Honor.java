package org.gumel.jojoha.fortune;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Honor {
    public static final float START = 50F;

    public static final float MOST = 100F;

    public static final float HIGH = 75F;

    public static final float LOW = 25F;

    public static final double HIGH_DAMAGE = 1.0;

    public static final double LOW_DAMAGE = -1.0;

    public static final float FACING_GAIN = 1F;

    public static final float PATIENT_GAIN = 3F;

    public static final float TEAM_GAIN = 1F;

    public static final float BRAVE_GAIN = 1F;

    public static final int GAIN_GAP_TICKS = 20;

    public static final int PATIENT_TICKS = 60;

    public static final float BRAVE_BELOW = 0.3F;

    public static final double TEAM_RANGE = 8.0;

    public static final float BACKSHOT_LOSS = 10F;

    public static final float HEAL_LOSS = 5F;

    public static final float ITEM_LOSS = 3F;

    public static final float STALL_LOSS = 5F;

    public static final int STALL_TICKS = 400;

    public static final int USE_GAP_TICKS = 40;

    public static final int SETTLE_EVERY = 1200;

    public static final float COMEBACK_BELOW = 0.2F;

    public static final float COMEBACK_ABOVE = 0.7F;

    public static final int COMEBACK_TICKS = 100;

    public static final int COMEBACK_AGAIN_TICKS = 6000;

    public static final int OPPONENT_TICKS = 600;

    public static final float GIFT_GAIN = 3F;

    public static final int GIFT_FOOD = 2;

    public static final float GIFT_SATURATION = 0.5F;

    public static final int GIFT_GAP_TICKS = 20;

    public static final double SACRIFICE_RANGE = 8.0;

    public static final int SACRIFICE_TICKS = 600;

    private static final ResourceLocation DAMAGE_ID = Fortunes.id("fortune_honor");

    private static final Map<UUID, Long> LAST_GAIN = new HashMap<>();

    private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();

    private static final Map<UUID, Long> LAST_STALL = new HashMap<>();

    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    private static final Map<UUID, Long> LAST_GIFT = new HashMap<>();

    private static final Map<UUID, Map<UUID, Long>> OPPONENTS = new HashMap<>();

    private static final Map<String, Long> COMEBACKS = new HashMap<>();

    private Honor() {
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean honorbound) {
        double bonus = (!honorbound ? 0.0 : data.honor >= HIGH ? HIGH_DAMAGE : data.honor <= LOW ? LOW_DAMAGE : 0.0)
                * Charms.fortune(player);
        Fortunes.modifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE);
        if (!honorbound) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (data.combatTicks > 0) {
            Long last = LAST_ATTACK.putIfAbsent(player.getUUID(), now);
            Long stalled = LAST_STALL.get(player.getUUID());
            if (last != null && now - last > STALL_TICKS && (stalled == null || now - stalled > STALL_TICKS)) {
                LAST_STALL.put(player.getUUID(), now);
                change(player, data, -STALL_LOSS, "message.jojoha.fortune.honor.stalling");
            }
        } else {
            LAST_ATTACK.remove(player.getUUID());
            if (player.tickCount % SETTLE_EVERY == 0 && data.honor != START) {
                data.honor += Math.signum(START - data.honor);
                PlayerDataAccess.set(player, data);
            }
        }
    }

    static void struck(ServerPlayer attacker, JojohaPlayerData data, LivingEntity victim, DamageSource source,
                       float amount) {
        if (!(victim instanceof Player || victim instanceof Enemy)) {
            return;
        }
        if (victim instanceof ServerPlayer target) {
            opposed(attacker, target);
        }
        long now = attacker.serverLevel().getGameTime();
        Defence.Facing facing = Defence.facing(victim, attacker);
        if (source.getDirectEntity() instanceof Projectile && facing == Defence.Facing.BACK) {
            change(attacker, data, -BACKSHOT_LOSS, "message.jojoha.fortune.honor.backshot");
        } else {
            Long gained = LAST_GAIN.get(attacker.getUUID());
            if (gained == null || now - gained >= GAIN_GAP_TICKS) {
                LAST_GAIN.put(attacker.getUUID(), now);
                Long last = LAST_ATTACK.get(attacker.getUUID());
                float gain = facing == Defence.Facing.FRONT ? FACING_GAIN : 0F;
                if (last != null && now - last >= PATIENT_TICKS) {
                    gain += PATIENT_GAIN;
                }
                if (teamed(attacker)) {
                    gain += TEAM_GAIN;
                }
                if (attacker.getHealth() < attacker.getMaxHealth() * BRAVE_BELOW) {
                    gain += BRAVE_GAIN;
                }
                change(attacker, data, gain, null);
            }
        }
        LAST_ATTACK.put(attacker.getUUID(), now);
        if (victim instanceof ServerPlayer target) {
            comeback(attacker, target, amount, now);
        }
    }

    private static boolean teamed(ServerPlayer player) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && other.distanceToSqr(player) < TEAM_RANGE * TEAM_RANGE
                    && PlayerDataAccess.get(other).combatTicks > 0) {
                return true;
            }
        }
        return false;
    }

    private static void comeback(ServerPlayer attacker, ServerPlayer target, float amount, long now) {
        float left = target.getHealth() - amount;
        if (attacker.getHealth() < attacker.getMaxHealth() * COMEBACK_ABOVE
                || left <= 0F || left > target.getMaxHealth() * COMEBACK_BELOW) {
            return;
        }
        String pair = attacker.getUUID() + ">" + target.getUUID();
        Long last = COMEBACKS.get(pair);
        if (last != null && now - last < COMEBACK_AGAIN_TICKS) {
            return;
        }
        COMEBACKS.put(pair, now);
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COMEBACK_TICKS, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, COMEBACK_TICKS, 0));
        Fortunes.whisper(target, Component.translatable("message.jojoha.fortune.comeback", attacker.getDisplayName()));
    }

    static void opposed(ServerPlayer one, ServerPlayer other) {
        long now = one.serverLevel().getGameTime();
        OPPONENTS.computeIfAbsent(one.getUUID(), id -> new HashMap<>()).put(other.getUUID(), now);
        OPPONENTS.computeIfAbsent(other.getUUID(), id -> new HashMap<>()).put(one.getUUID(), now);
    }

    static boolean opponents(ServerPlayer one, Entity other) {
        Map<UUID, Long> fought = OPPONENTS.get(one.getUUID());
        Long at = fought == null ? null : fought.get(other.getUUID());
        return at != null && one.serverLevel().getGameTime() - at <= OPPONENT_TICKS;
    }

    static void used(ServerPlayer player, ItemStack stack) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.combatTicks <= 0) {
            return;
        }
        boolean healing = stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || (stack.getItem() instanceof PotionItem && !(stack.getItem() instanceof ThrowablePotionItem)
                && heals(stack));
        boolean fleeing = stack.is(Items.ENDER_PEARL) || stack.is(Items.CHORUS_FRUIT);
        if (!healing && !fleeing) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        Long last = LAST_USE.get(player.getUUID());
        if (last != null && now - last < USE_GAP_TICKS) {
            return;
        }
        LAST_USE.put(player.getUUID(), now);
        change(player, data, healing ? -HEAL_LOSS : -ITEM_LOSS, "message.jojoha.fortune.honor.cowardice");
    }

    private static boolean heals(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().equals(MobEffects.HEAL) || effect.getEffect().equals(MobEffects.REGENERATION)) {
                return true;
            }
        }
        return false;
    }

    public static void mirrored(LivingEntity target, MobEffectInstance effect, Entity source) {
        if (!Fortunes.on() || !(source instanceof ServerPlayer giver) || giver == target
                || !(target instanceof ServerPlayer) || !effect.getEffect().value().isBeneficial()) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(giver);
        if (!Fortune.has(data, Fortune.HONORBOUND) || !opponents(giver, target)) {
            return;
        }
        giver.addEffect(new MobEffectInstance(effect));
        gift(giver, data);
    }

    static void picked(ServerPlayer picker, ItemEntity item, ItemStack stack) {
        if (!(item.getOwner() instanceof ServerPlayer giver) || giver == picker) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(giver);
        if (!Fortune.has(data, Fortune.HONORBOUND) || !opponents(giver, picker)) {
            return;
        }
        if (stack.has(DataComponents.FOOD) || stack.getItem() instanceof PotionItem) {
            gift(giver, data);
        }
    }

    private static void gift(ServerPlayer giver, JojohaPlayerData data) {
        long now = giver.serverLevel().getGameTime();
        Long last = LAST_GIFT.get(giver.getUUID());
        if (last != null && now - last < GIFT_GAP_TICKS) {
            return;
        }
        LAST_GIFT.put(giver.getUUID(), now);
        giver.getFoodData().eat(GIFT_FOOD, GIFT_SATURATION);
        change(giver, data, GIFT_GAIN, null);
    }

    static void fell(ServerPlayer player, DamageSource source) {
        if (PlayerDataAccess.get(player).combatTicks <= 0) {
            return;
        }
        for (ServerPlayer ally : player.serverLevel().players()) {
            if (ally == player || ally == source.getEntity() || ally.isSpectator()
                    || ally.distanceToSqr(player) > SACRIFICE_RANGE * SACRIFICE_RANGE || opponents(player, ally)) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, SACRIFICE_TICKS, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SACRIFICE_TICKS, 0));
            Fortunes.whisper(ally, Component.translatable("message.jojoha.fortune.sacrifice", player.getDisplayName()));
        }
    }

    private static void change(ServerPlayer player, JojohaPlayerData data, float by, String why) {
        if (by == 0F) {
            return;
        }
        if (by < 0F) {
            by *= Charms.fortune(player);
        }
        float before = data.honor;
        data.honor = Math.max(0F, Math.min(MOST, data.honor + by));
        PlayerDataAccess.set(player, data);
        if (why != null) {
            player.displayClientMessage(Component.translatable(why), true);
        }
        if ((before < HIGH) != (data.honor < HIGH) || (before > LOW) != (data.honor > LOW)) {
            PlayerDataAccess.sync(player);
        }
    }

    static void forget(Player player) {
        UUID id = player.getUUID();
        LAST_GAIN.remove(id);
        LAST_ATTACK.remove(id);
        LAST_STALL.remove(id);
        LAST_USE.remove(id);
        LAST_GIFT.remove(id);
        OPPONENTS.remove(id);
        COMEBACKS.keySet().removeIf(pair -> pair.contains(id.toString()));
    }
}
