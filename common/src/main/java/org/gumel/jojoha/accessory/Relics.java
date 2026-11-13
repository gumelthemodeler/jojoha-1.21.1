package org.gumel.jojoha.accessory;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.skill.moves.BalestraSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopUpgradeSkill;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Relics {
    public static final float FINGERLESS_BONUS = 2F;
    public static final int FINGERLESS_TICKS = 20;

    public static final double COAT_CHAIN_KNOCKBACK = 0.25;

    public static final double BOWLER_SPEED = 0.15;
    public static final float BOWLER_HEALTH = 0.3F;

    public static final float WEDDING_RING_SCALE = 1.15F;
    public static final int WEDDING_RING_TICKS = 3600;
    public static final int WEDDING_RING_POISON_TICKS = 100;

    public static final int CAP_BLOWS = 3;
    public static final int CAP_WINDOW_TICKS = 100;

    public static final int HEADBAND_TICKS_PER_KILL = 60;
    public static final float HEADBAND_MOST_SHARE = 0.5F;

    public static final float CONTROL_MASK_BREATH = 1.25F;

    public static final int HEADBAND_BUBBLES = 1;
    public static final double HEADBAND_BUBBLE_SPEED = 1.3;

    public static final float AJA_SCALE = 0.2F;

    public static final int CHERRY_EMERALDS = 2;

    public static final int POLNAREFF_AFTERIMAGES = 2;

    private static final ResourceLocation COAT_CHAIN_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "jotaros_chain");
    private static final ResourceLocation BOWLER_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "speedwagons_bowler_hat");

    private static final Map<UUID, Long> PERFECT_AT = new HashMap<>();
    private static final Map<UUID, Long> RING_FED_AT = new HashMap<>();
    private static final Map<UUID, Deque<Long>> GUARD_BLOWS = new HashMap<>();
    private static final Set<UUID> CAP_PRIMED = new HashSet<>();

    private Relics() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer served) {
                tick(served);
            }
        });
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            UseAnim use = held.getUseAnimation();
            if ((use == UseAnim.EAT || use == UseAnim.DRINK)
                    && Accessories.wearing(player, ModItems.HAMON_CONTROL_MASK.get())) {
                player.displayClientMessage(Component.translatable("message.jojoha.control_mask"), true);
                return CompoundEventResult.interruptFalse(held);
            }
            return CompoundEventResult.pass();
        });
        PlayerEvent.PLAYER_QUIT.register(Relics::forget);
    }

    public static void forget(Player player) {
        UUID id = player.getUUID();
        PERFECT_AT.remove(id);
        RING_FED_AT.remove(id);
        GUARD_BLOWS.remove(id);
        CAP_PRIMED.remove(id);
    }

    private static void tick(ServerPlayer player) {
        long now = player.level().getGameTime();

        modifier(player, Attributes.KNOCKBACK_RESISTANCE, COAT_CHAIN_ID, COAT_CHAIN_KNOCKBACK,
                AttributeModifier.Operation.ADD_VALUE,
                Accessories.wearing(player, ModItems.JOTAROS_CHAIN.get()));
        modifier(player, Attributes.MOVEMENT_SPEED, BOWLER_ID, BOWLER_SPEED,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                player.isSprinting() && player.getHealth() <= player.getMaxHealth() * BOWLER_HEALTH
                        && Accessories.wearing(player, ModItems.SPEEDWAGONS_BOWLER_HAT.get()));

        if (!Accessories.wearing(player, ModItems.WEDDING_RING_OF_DEATH.get()) || player.isCreative()
                || player.isSpectator()) {
            RING_FED_AT.put(player.getUUID(), now);
        } else if (now - RING_FED_AT.getOrDefault(player.getUUID(), now) >= WEDDING_RING_TICKS) {
            RING_FED_AT.put(player.getUUID(), now);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, WEDDING_RING_POISON_TICKS, 1));
            player.displayClientMessage(Component.translatable("message.jojoha.wedding_ring"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK,
                    SoundSource.PLAYERS, 0.8F, 0.6F);
        }
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                                 double amount, AttributeModifier.Operation operation, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || instance.hasModifier(id) == on) {
            return;
        }
        if (on) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else {
            instance.removeModifier(id);
        }
    }

    public static void perfectParry(ServerPlayer player) {
        PERFECT_AT.put(player.getUUID(), player.level().getGameTime());
    }

    public static float outgoing(LivingEntity victim, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || victim == attacker) {
            return amount;
        }
        float dealt = amount;
        if (source.getDirectEntity() == attacker && Accessories.wearing(attacker,
                ModItems.JOSEPHS_FINGERLESS_GLOVES.get())) {
            Long at = PERFECT_AT.remove(attacker.getUUID());
            if (at != null && attacker.level().getGameTime() - at <= FINGERLESS_TICKS) {
                dealt += FINGERLESS_BONUS;
            }
        }
        if (Accessories.wearing(attacker, ModItems.WEDDING_RING_OF_DEATH.get())) {
            if (victim instanceof Enemy) {
                RING_FED_AT.put(attacker.getUUID(), attacker.level().getGameTime());
            }
            dealt = capped(dealt, dealt * WEDDING_RING_SCALE);
        }
        return dealt;
    }

    private static float capped(float before, float after) {
        float cap = JojohaServerConfig.MAX_HIT.value();
        return cap > 0F ? Math.min(after, Math.max(before, cap)) : after;
    }

    public static float hamonScale(Entity attacker) {
        if (!(attacker instanceof Player player) || !Accessories.wearing(player, ModItems.RING_OF_AJA.get())) {
            return 1F;
        }
        return 1F + (sunlit(player) ? AJA_SCALE : -AJA_SCALE);
    }

    private static boolean sunlit(Player player) {
        return player.level().isDay() && player.level().getBrightness(LightLayer.SKY, player.blockPosition()) >= 12;
    }

    public static float breathScale(Player player, JojohaPlayerData data) {
        return Fate.breathesHamon(data) && Accessories.wearing(player, ModItems.HAMON_CONTROL_MASK.get())
                ? CONTROL_MASK_BREATH : 1F;
    }

    public static int extraBubbles(Player player) {
        return Accessories.wearing(player, ModItems.CAESARS_HEADBAND.get()) ? HEADBAND_BUBBLES : 0;
    }

    public static double bubbleSpeed(Player player) {
        return Accessories.wearing(player, ModItems.CAESARS_HEADBAND.get()) ? HEADBAND_BUBBLE_SPEED : 1.0;
    }

    public static int extraEmeralds(Player player) {
        return Accessories.wearing(player, ModItems.KAKYOIN_EARRING.get()) ? CHERRY_EMERALDS : 0;
    }

    public static int extraAfterimages(Player player) {
        return Accessories.wearing(player, ModItems.POLNAREFF_EARRING.get()) ? POLNAREFF_AFTERIMAGES : 0;
    }

    public static void guardBlow(ServerPlayer player) {
        if (!Accessories.wearing(player, ModItems.JOTAROS_CAP.get())) {
            return;
        }
        long now = player.level().getGameTime();
        Deque<Long> blows = GUARD_BLOWS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        blows.addLast(now);
        while (!blows.isEmpty() && now - blows.peekFirst() > CAP_WINDOW_TICKS) {
            blows.removeFirst();
        }
        if (blows.size() >= CAP_BLOWS) {
            blows.clear();
            CAP_PRIMED.add(player.getUUID());
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                    SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    public static void standPunchLanded(ServerPlayer player, LivingEntity target) {
        if (CAP_PRIMED.remove(player.getUUID()) && Accessories.wearing(player, ModItems.JOTAROS_CAP.get())) {
            Stun.stagger(target);
        }
    }

    public static void riposteLanded(ServerPlayer player) {
        if (!Accessories.wearing(player, ModItems.SHERRYS_LOCKET.get())) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.setMoveCooldown(BalestraSkill.ID, player.level().getGameTime(), 0);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.syncSoon(player);
    }

    public static void timeStopKills(ServerPlayer player, int kills) {
        if (kills <= 0 || !Accessories.wearing(player, ModItems.DIOS_HEADBAND.get())) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        long now = player.level().getGameTime();
        int cut = kills * HEADBAND_TICKS_PER_KILL;
        for (ResourceLocation id : new ResourceLocation[] {
                TimeStopSkill.ID,
                TimeStopUpgradeSkill.EXTENDED_ID,
                TimeStopUpgradeSkill.PERFECT_ID}) {
            long left = data.moveCooldownLeft(id, now);
            if (left > 0) {
                data.setMoveCooldown(id, now, (int) Math.max(left * (1F - HEADBAND_MOST_SHARE), left - cut));
            }
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    public static ProjectileDeflection windShell(ServerPlayer player, Projectile projectile) {
        if (!player.isSprinting() || !Accessories.wearing(player, ModItems.WAMUUS_HEADDRESS.get())) {
            return null;
        }
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 from = velocity.lengthSqr() < 1.0E-6 ? projectile.position()
                : projectile.position().subtract(velocity.normalize().scale(3.0));
        if (Defence.facing(player, from) != Defence.Facing.BACK) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = projectile.position();
        level.sendParticles(ParticleTypes.GUST, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_DEFLECT, SoundSource.PLAYERS, 0.8F, 1.1F);
        return ProjectileDeflection.REVERSE;
    }
}
