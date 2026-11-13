package org.gumel.jojoha.fortune;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Lucky {
    public static final double LUCK = 1.0;

    public static final double BROKEN_LUCK = -1.0;

    public static final float ARROW_ODDS = 1.1F;

    public static final int FOUR = 4;

    public static final double CROWD_RANGE = 8.0;

    public static final int COMBO_GAP_TICKS = 30;

    public static final int BROKEN_TICKS = 600;

    public static final int SPOOK_WEAKNESS_TICKS = 60;

    public static final int SPOOK_AGAIN_TICKS = 1200;

    public static final double LIGHTNING_RANGE = 16.0;

    public static final int PULL_EVERY = 12000;

    public static final float PULL_CHANCE = 0.5F;

    private static final int CHECK_EVERY = 20;

    private static final ResourceLocation LUCK_ID = Fortunes.id("fortune_lucky");

    private static final Map<UUID, Long> BROKEN_UNTIL = new HashMap<>();

    private static final Map<UUID, Boolean> FOURS = new HashMap<>();

    private static final Map<UUID, long[]> COMBO = new HashMap<>();

    private static final Map<UUID, Long> SPOOKED = new HashMap<>();

    private Lucky() {
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean lucky) {
        if (!lucky) {
            Fortunes.modifier(player, Attributes.LUCK, LUCK_ID, 0.0, AttributeModifier.Operation.ADD_VALUE);
            return;
        }
        if (player.tickCount % CHECK_EVERY == 0) {
            FOURS.put(player.getUUID(), data.phobia == Fortune.TETRAPHOBIA && fours(player));
        }
        Fortunes.modifier(player, Attributes.LUCK, LUCK_ID,
                (intact(player, data) ? LUCK : BROKEN_LUCK) * Charms.fortune(player),
                AttributeModifier.Operation.ADD_VALUE);
        if (player.tickCount % PULL_EVERY == 0 && player.getRandom().nextFloat() < PULL_CHANCE) {
            Pulls.structure(player, "message.jojoha.fortune.lucky_pull");
        }
    }

    public static boolean intact(Player player, JojohaPlayerData data) {
        if (!(player instanceof ServerPlayer)) {
            return true;
        }
        Long until = BROKEN_UNTIL.get(player.getUUID());
        boolean broken = until != null && player.level().getGameTime() < until;
        return !broken && !FOURS.getOrDefault(player.getUUID(), false);
    }

    static boolean fours(ServerPlayer player) {
        for (List<ItemStack> part : List.of(player.getInventory().items, player.getInventory().armor,
                player.getInventory().offhand)) {
            for (ItemStack stack : part) {
                if (stack.getCount() == FOUR) {
                    return true;
                }
            }
        }
        Map<EntityType<?>, Integer> crowd = new HashMap<>();
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(CROWD_RANGE))) {
            if (crowd.merge(mob.getType(), 1, Integer::sum) >= FOUR) {
                return true;
            }
        }
        return false;
    }

    static void struck(ServerPlayer attacker, JojohaPlayerData data) {
        if (data.phobia != Fortune.TETRAPHOBIA) {
            return;
        }
        long now = attacker.serverLevel().getGameTime();
        long[] combo = COMBO.computeIfAbsent(attacker.getUUID(), id -> new long[2]);
        if (now - combo[1] > COMBO_GAP_TICKS) {
            combo[0] = 0;
        }
        combo[0]++;
        combo[1] = now;
        if (combo[0] % FOUR == 0) {
            breakLuck(attacker, now);
        }
    }

    static void wounded(ServerPlayer player, JojohaPlayerData data, DamageSource source) {
        if (data.phobia != Fortune.SUPERSTITION) {
            return;
        }
        boolean stand = source.getDirectEntity() instanceof StandEntity
                || StandCombatHandler.standBlow(source)
                || (source.getEntity() instanceof Mob mob && MobStands.out(mob));
        boolean undead = source.getEntity() instanceof LivingEntity living
                && HamonCombat.isUndead(living);
        if (stand || undead) {
            spook(player);
        }
    }

    static void lightning(Level level, Vec3 at) {
        if (level.isClientSide()) {
            return;
        }
        for (Player near : level.players()) {
            if (near instanceof ServerPlayer player && player.position().distanceToSqr(at) < LIGHTNING_RANGE * LIGHTNING_RANGE) {
                JojohaPlayerData data = PlayerDataAccess.get(player);
                if (Fortune.has(data, Fortune.LUCKY) && data.phobia == Fortune.SUPERSTITION) {
                    spook(player);
                }
            }
        }
    }

    private static void spook(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Long last = SPOOKED.get(player.getUUID());
        SPOOKED.put(player.getUUID(), now);
        if (last != null && now - last < SPOOK_AGAIN_TICKS) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                Math.round(SPOOK_WEAKNESS_TICKS * Charms.fortune(player)), 0));
        player.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.7F, 1.0F);
        breakLuck(player, now);
    }

    private static void breakLuck(ServerPlayer player, long now) {
        Long until = BROKEN_UNTIL.put(player.getUUID(), now + Math.round(BROKEN_TICKS * Charms.fortune(player)));
        if (until == null || now >= until) {
            Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.luck_broken"));
        }
    }

    public static boolean luckyCrystal(Player player) {
        return player instanceof ServerPlayer server
                && Fortune.has(PlayerDataAccess.get(server), Fortune.LUCKY) && intact(player, PlayerDataAccess.get(server));
    }

    static void forget(Player player) {
        BROKEN_UNTIL.remove(player.getUUID());
        FOURS.remove(player.getUUID());
        COMBO.remove(player.getUUID());
        SPOOKED.remove(player.getUUID());
    }
}
