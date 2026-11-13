package org.gumel.jojoha.data;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;

public final class StatEffects {
    public static final int BASE_STAT = 5;

    private static final float CLIMB = StatPoints.MAX_STAT - BASE_STAT;

    private static final double MAX_ATTACK_DAMAGE = 2.0;
    private static final double MAX_HEALTH_BONUS = 10.0;
    private static final double MAX_MOVEMENT = 0.15;
    private static final double MAX_ARMOUR = 4.0;
    private static final double MAX_KNOCKBACK = 0.10;
    private static final double MAX_LUCK = 3.0;

    private static final float BODY_DAMAGE_AT_BASE = 1.15F;
    private static final float BODY_DAMAGE_AT_MAX = 1.4F;

    private static final float MAX_REGEN_BONUS = 0.3F;

    private static final float POWER_AT_BASE = 1.2F;
    private static final float POWER_AT_MAX = 1.6F;

    private static final float COOLDOWN_CUT_AT_BASE = 0.10F;
    private static final float COOLDOWN_CUT_AT_MAX = 0.30F;

    private static final float COST_CUT_AT_BASE = 0.075F;
    private static final float COST_CUT_AT_MAX = 0.30F;

    private static final int GUARD_HITS_AT_BASE = 8;
    private static final int GUARD_HITS_AT_MAX = 11;

    private static final float MAX_ENERGY_BONUS = 25F;

    private static final ResourceLocation STRENGTH_ID = id("stat_strength");
    private static final ResourceLocation VITALITY_ID = id("stat_vitality");
    private static final ResourceLocation AGILITY_ID = id("stat_agility");
    private static final ResourceLocation ENDURANCE_ARMOUR_ID = id("stat_endurance_armour");
    private static final ResourceLocation ENDURANCE_KNOCKBACK_ID = id("stat_endurance_knockback");
    private static final ResourceLocation WORTHINESS_ID = id("stat_worthiness");

    private StatEffects() {
    }

    public static float climb(int stat) {
        return Math.min(1F, Math.max(0F, (stat - BASE_STAT) / CLIMB));
    }

    public static void apply(ServerPlayer player, JojohaPlayerData data) {
        set(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID,
                MAX_ATTACK_DAMAGE * climb(data.strength), AttributeModifier.Operation.ADD_VALUE);

        set(player, Attributes.MAX_HEALTH, VITALITY_ID,
                MAX_HEALTH_BONUS * climb(data.vitality), AttributeModifier.Operation.ADD_VALUE);

        set(player, Attributes.MOVEMENT_SPEED, AGILITY_ID,
                MAX_MOVEMENT * climb(data.agility),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        set(player, Attributes.ARMOR, ENDURANCE_ARMOUR_ID,
                MAX_ARMOUR * climb(data.endurance), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.KNOCKBACK_RESISTANCE, ENDURANCE_KNOCKBACK_ID,
                MAX_KNOCKBACK * climb(data.endurance), AttributeModifier.Operation.ADD_VALUE);

        set(player, Attributes.LUCK, WORTHINESS_ID,
                MAX_LUCK * climb(data.worthiness), AttributeModifier.Operation.ADD_VALUE);

        set(player, Attributes.SAFE_FALL_DISTANCE, LIGHT_FEET_ID,
                StatPerk.LIGHT_FEET.has(data) ? LIGHT_FEET_BLOCKS : 0.0, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.STEP_HEIGHT, WIND_STEP_ID,
                StatPerk.WIND_STEP.has(data) ? WIND_STEP_HEIGHT : 0.0, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.OXYGEN_BONUS, DEEP_LUNGS_ID,
                StatPerk.DEEP_LUNGS.has(data) ? DEEP_LUNGS_OXYGEN : 0.0, AttributeModifier.Operation.ADD_VALUE);
    }

    private static final double LIGHT_FEET_BLOCKS = 2.0;
    private static final double WIND_STEP_HEIGHT = 0.4;
    private static final double DEEP_LUNGS_OXYGEN = 1.0;

    private static final ResourceLocation LIGHT_FEET_ID = id("perk_light_feet");
    private static final ResourceLocation WIND_STEP_ID = id("perk_wind_step");
    private static final ResourceLocation DEEP_LUNGS_ID = id("perk_deep_lungs");

    public static float bodyDamageScale(int strength) {
        return BODY_DAMAGE_AT_BASE + (BODY_DAMAGE_AT_MAX - BODY_DAMAGE_AT_BASE) * climb(strength);
    }

    public static float regenScale(JojohaPlayerData data) {
        return 1F + MAX_REGEN_BONUS * climb(data.worthiness);
    }

    public static float powerScale(int power) {
        float scale = POWER_AT_BASE + (POWER_AT_MAX - POWER_AT_BASE) * climb(power);
        return StatPerk.HEAVY_STRIKES.reached(power) ? scale * HEAVY_STRIKES_SCALE : scale;
    }

    public static float standCooldownScale(int speed) {
        float scale = 1F - (COOLDOWN_CUT_AT_BASE + (COOLDOWN_CUT_AT_MAX - COOLDOWN_CUT_AT_BASE) * climb(speed));
        if (StatPerk.QUICK_HANDS.reached(speed)) {
            scale *= 1F - QUICK_HANDS_CUT;
        }
        if (StatPerk.RELENTLESS.reached(speed)) {
            scale *= 1F - RELENTLESS_CUT;
        }
        return scale;
    }

    public static float standCostScale(int endurance) {
        float scale = 1F - (COST_CUT_AT_BASE + (COST_CUT_AT_MAX - COST_CUT_AT_BASE) * climb(endurance));
        return StatPerk.ENDURING.reached(endurance) ? scale * (1F - ENDURING_CUT) : scale;
    }

    public static int guardHits(int protection) {
        int hits = Math.round(GUARD_HITS_AT_BASE
                + (GUARD_HITS_AT_MAX - GUARD_HITS_AT_BASE) * climb(protection));
        if (StatPerk.FIRM_GUARD.reached(protection)) {
            hits += FIRM_GUARD_HITS;
        }
        return StatPerk.IMPENETRABLE.reached(protection) ? hits + IMPENETRABLE_HITS : hits;
    }

    public static float energyBonus(int potential) {
        float bonus = MAX_ENERGY_BONUS * climb(potential);
        return StatPerk.DEEP_WELL.reached(potential) ? bonus + DEEP_WELL_ENERGY : bonus;
    }

    private static final float HEAVY_STRIKES_SCALE = 1.05F;
    private static final float QUICK_HANDS_CUT = 0.03F;
    private static final float RELENTLESS_CUT = 0.05F;
    private static final float ENDURING_CUT = 0.03F;
    private static final int FIRM_GUARD_HITS = 1;
    private static final int IMPENETRABLE_HITS = 2;
    private static final float DEEP_WELL_ENERGY = 10F;

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                            double amount, AttributeModifier.Operation operation) {
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

    public static void clear(ServerPlayer player) {
        remove(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID);
        remove(player, Attributes.MAX_HEALTH, VITALITY_ID);
        remove(player, Attributes.MOVEMENT_SPEED, AGILITY_ID);
        remove(player, Attributes.ARMOR, ENDURANCE_ARMOUR_ID);
        remove(player, Attributes.KNOCKBACK_RESISTANCE, ENDURANCE_KNOCKBACK_ID);
        remove(player, Attributes.LUCK, WORTHINESS_ID);
        remove(player, Attributes.SAFE_FALL_DISTANCE, LIGHT_FEET_ID);
        remove(player, Attributes.STEP_HEIGHT, WIND_STEP_ID);
        remove(player, Attributes.OXYGEN_BONUS, DEEP_LUNGS_ID);
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
