package org.gumel.jojoha.combat;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModEffects;

public final class JojohaEnchantments {

    public static final ResourceKey<Enchantment> CONDUCTIVITY = key("conductivity");
    public static final ResourceKey<Enchantment> SIPHON = key("siphon");
    public static final ResourceKey<Enchantment> LUCK_AND_PLUCK = key("luck_and_pluck");

    private static final float CONDUCTIVITY_PER_LEVEL = 3.0F;

    private static final float SIPHON_HEALTH_PER_LEVEL = 0.6F;

    private static final float SIPHON_BLOOD_PER_LEVEL = 0.4F;

    private static final float PLUCK_HEALTH_FRACTION = 0.3F;
    private static final int PLUCK_DURATION_TICKS = 160;

    private JojohaEnchantments() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(JojohaEnchantments::onDeath);
        EntityEvent.LIVING_HURT.register(JojohaEnchantments::onHurt);
    }

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name));
    }

    public static int levelOn(LivingEntity holder, ItemStack stack, ResourceKey<Enchantment> which) {
        if (stack.isEmpty() || !stack.isEnchanted()) {
            return 0;
        }
        return holder.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(which)
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack))
                .orElse(0);
    }

    public static int levelWorn(LivingEntity wearer, ResourceKey<Enchantment> which) {
        int best = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            best = Math.max(best, levelOn(wearer, wearer.getItemBySlot(slot), which));
        }
        return best;
    }

    public static float conductivityBonus(DamageSource source, LivingEntity victim) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return 0F;
        }
        if (!Fate.breathesHamon(PlayerDataAccess.get(attacker))) {
            return 0F;
        }
        if (!isVampiric(victim)) {
            return 0F;
        }

        int level = levelOn(attacker, attacker.getMainHandItem(), CONDUCTIVITY);
        return level <= 0 ? 0F : level * CONDUCTIVITY_PER_LEVEL;
    }

    public static int conductivityLevel(ServerPlayer player) {
        return levelOn(player, player.getMainHandItem(), CONDUCTIVITY);
    }

    private static boolean isVampiric(LivingEntity entity) {
        return JojohaUndead.is(entity);
    }

    private static EventResult onDeath(LivingEntity died, DamageSource source) {
        if (died.level().isClientSide() || !(source.getEntity() instanceof ServerPlayer killer)) {
            return EventResult.pass();
        }

        int level = levelOn(killer, killer.getMainHandItem(), SIPHON);
        if (level <= 0) {
            return EventResult.pass();
        }

        killer.heal(level * SIPHON_HEALTH_PER_LEVEL);

        if (VampireBlood.isVampire(killer) && VampireBlood.hasBlood(died)) {
            VampireBlood.feed(killer, (int) (level * SIPHON_BLOOD_PER_LEVEL));
        }

        return EventResult.pass();
    }

    private static EventResult onHurt(LivingEntity hurt, DamageSource source, float amount) {
        if (hurt.level().isClientSide() || !(hurt instanceof ServerPlayer player)) {
            return EventResult.pass();
        }

        if (player.getHealth() > player.getMaxHealth() * PLUCK_HEALTH_FRACTION) {
            return EventResult.pass();
        }

        int level = levelWorn(player, LUCK_AND_PLUCK);
        if (level <= 0) {
            return EventResult.pass();
        }

        player.addEffect(new MobEffectInstance(ModEffects.pluck(), PLUCK_DURATION_TICKS, level - 1));
        return EventResult.pass();
    }
}
