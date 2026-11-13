package org.gumel.jojoha.combat;

import org.gumel.jojoha.town.Residents;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.Blocks;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.trait.TraitEffects;
import org.gumel.jojoha.vampire.Headless;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VampireBlood {
    private static final TagKey<EntityType<?>> BLOODLESS =
            TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            Jojoha.MOD_ID, "bloodless"));

    private static final int MAX_BLOOD = 20;

    private static final int KILL_BLOOD_BASE = 1;
    private static final double KILL_BLOOD_PER_HEALTH = 0.15;
    private static final int KILL_BLOOD_CAP = 5;

    private static final int BITE_BLOOD = 2;
    private static final float BITE_DAMAGE = 2.0F;
    private static final float BITE_HEAL = 1.0F;
    private static final int BITE_COOLDOWN_TICKS = 40;
    private static final double BITE_REACH = 3.0;

    private static final Map<UUID, Long> LAST_BITE = new HashMap<>();

    private VampireBlood() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(VampireBlood::onKill);
        InteractionEvent.INTERACT_ENTITY.register(VampireBlood::onInteract);
        PlayerEvent.PLAYER_QUIT
                .register(player -> LAST_BITE.remove(player.getUUID()));
    }

    public static boolean isVampire(Player player) {
        return player instanceof ServerPlayer served
                && PlayerDataAccess.get(served).vampireStage != VampireStage.NONE;
    }

    public static boolean breathless(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (!player.level().isClientSide()) {
            return isVampire(player);
        }
        return player.isLocalPlayer()
                && ClientPlayerDataCache.data.vampireStage != VampireStage.NONE;
    }

    public static boolean hasBlood(LivingEntity entity) {
        return !entity.getType().is(BLOODLESS);
    }

    public static int blood(Player player) {
        return player.getFoodData().getFoodLevel();
    }

    public static boolean spend(ServerPlayer player, int amount) {
        FoodData food = player.getFoodData();
        if (amount <= 0 || food.getFoodLevel() < amount) {
            return false;
        }
        food.setFoodLevel(food.getFoodLevel() - amount);
        return true;
    }

    public static int feed(ServerPlayer player, int amount) {
        JojohaAdvancements.award(player, "part1/first_feeding");
        FoodData food = player.getFoodData();
        int room = MAX_BLOOD - food.getFoodLevel();
        if (room <= 0 || amount <= 0) {
            return 0;
        }

        int given = Math.min(room, TraitEffects.bloodDrawn(player, amount));
        food.eat(given, given * 0.1F);
        return given;
    }

    private static EventResult onKill(LivingEntity died, DamageSource source) {
        if (died.level().isClientSide() || !(source.getEntity() instanceof ServerPlayer killer)) {
            return EventResult.pass();
        }
        if (!isVampire(killer) || !hasBlood(died)) {
            return EventResult.pass();
        }

        int worth = KILL_BLOOD_BASE
                + (int) Math.min(KILL_BLOOD_CAP, died.getMaxHealth() * KILL_BLOOD_PER_HEALTH);
        if (feed(killer, worth) > 0) {
            drinkSound(killer);
        }
        return EventResult.pass();
    }

    private static EventResult onInteract(Player player, Entity target, InteractionHand hand) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return EventResult.pass();
        }
        if (!(player.isShiftKeyDown() || Headless.is(player))
                || !(player instanceof ServerPlayer vampire)) {
            return EventResult.pass();
        }
        if (!isVampire(vampire) || !(target instanceof LivingEntity prey)) {
            return EventResult.pass();
        }
        if (!hasBlood(prey) || !prey.isAlive() || vampire.distanceTo(prey) > BITE_REACH) {
            return EventResult.pass();
        }
        long now = vampire.level().getGameTime();
        Long last = LAST_BITE.get(vampire.getUUID());
        if (last != null && now - last < BITE_COOLDOWN_TICKS) {
            return EventResult.pass();
        }

        if (feed(vampire, BITE_BLOOD) <= 0) {
            return EventResult.pass();
        }

        prey.hurt(vampire.damageSources().playerAttack(vampire), DamageScale.vampire(vampire, prey, BITE_DAMAGE));
        vampire.heal(BITE_HEAL);
        LAST_BITE.put(vampire.getUUID(), now);
        vampire.swing(hand, true);
        drinkSound(vampire);
        Residents.fed(vampire, prey);

        if (vampire.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    prey.getX(), prey.getY() + prey.getBbHeight() * 0.7, prey.getZ(),
                    8, 0.15, 0.15, 0.15, 0.0);
        }

        return EventResult.interruptTrue();
    }

    private static void drinkSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS, 0.7F, 0.7F);
    }
}
