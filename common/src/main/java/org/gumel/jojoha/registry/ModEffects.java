package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill;

import java.util.function.BiConsumer;

import static org.gumel.jojoha.registry.ModRegistries.MOB_EFFECTS;

public final class ModEffects {
    private static final ResourceLocation STUN_SLOW =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stun_slow");
    private static final ResourceLocation STUN_DISARM =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stun_disarm");
    private static final ResourceLocation TREMBLING_SLOW =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "trembling_slow");

    private static final ResourceLocation PLUCK_DAMAGE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pluck_damage");
    private static final ResourceLocation PLUCK_RESISTANCE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pluck_resistance");

    private static final ResourceLocation GRAVE_MINING =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grave_mining");

    private static final int STUN_COLOUR = 0xD8D8C8;
    private static final int GRAVE_FATIGUE_COLOUR = 0x4A4250;
    private static final double GRAVE_MINING_FACTOR = 0.3;
    private static final int TANGLED_COLOUR = 0x8A4FBF;
    private static final int TREMBLING_COLOUR = 0x6B2D8C;

    private static final double TREMBLING_SLOWNESS = -0.35;

    private static final int PLUCK_COLOUR = 0xD93A34;

    private static final int BLEED_COLOUR = 0x8C1111;

    private static final int GUARD_BREAK_COLOUR = 0xC8792A;

    private static final int BURNING_COLOUR = 0xE8711C;

    private static final int BLEED_DRIP_EVERY = 10;

    private static final double PLUCK_DAMAGE_PER_LEVEL = 1.5;
    private static final double PLUCK_RESISTANCE_PER_LEVEL = 0.1;

    public static final RegistrySupplier<MobEffect> STUN = MOB_EFFECTS.register("stun",
            () -> new StunEffect()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, STUN_SLOW, -0.5,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, STUN_DISARM, -0.5,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final RegistrySupplier<MobEffect> TREMBLING = MOB_EFFECTS.register("trembling",
            () -> new StunEffect(TREMBLING_COLOUR)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, TREMBLING_SLOW, TREMBLING_SLOWNESS,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final RegistrySupplier<MobEffect> BLEED =
            MOB_EFFECTS.register("bleed", BleedEffect::new);

    public static final RegistrySupplier<MobEffect> GUARD_BREAK =
            MOB_EFFECTS.register("guard_break", GuardBreakEffect::new);

    public static final RegistrySupplier<MobEffect> BURNING =
            MOB_EFFECTS.register("burning", BurningEffect::new);

    public static final RegistrySupplier<MobEffect> GRAVE_FATIGUE =
            MOB_EFFECTS.register("grave_fatigue", GraveFatigueEffect::new);

    public static Holder<MobEffect> graveFatigue() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(GRAVE_FATIGUE.get());
    }

    public static Holder<MobEffect> burning() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BURNING.get());
    }

    public static Holder<MobEffect> bleed() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BLEED.get());
    }

    public static Holder<MobEffect> guardBreak() {
        return BuiltInRegistries.MOB_EFFECT
                .wrapAsHolder(GUARD_BREAK.get());
    }

    public static Holder<MobEffect> trembling() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TREMBLING.get());
    }

    public static Holder<MobEffect> stun() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(STUN.get());
    }

    private static final int KEEN_GUARD_COLOUR = 0xE9E2C8;
    private static final int STAND_VIGOR_COLOUR = 0x8A5A3A;
    private static final int STEADY_BREATH_COLOUR = 0x9A2740;

    public static final RegistrySupplier<MobEffect> KEEN_GUARD =
            MOB_EFFECTS.register("keen_guard", () -> new CurioEffect(KEEN_GUARD_COLOUR));

    public static final RegistrySupplier<MobEffect> STAND_VIGOR =
            MOB_EFFECTS.register("stand_vigor", () -> new CurioEffect(STAND_VIGOR_COLOUR));

    public static final RegistrySupplier<MobEffect> STEADY_BREATH =
            MOB_EFFECTS.register("steady_breath", () -> new CurioEffect(STEADY_BREATH_COLOUR));

    public static Holder<MobEffect> keenGuard() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(KEEN_GUARD.get());
    }

    public static Holder<MobEffect> standVigor() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(STAND_VIGOR.get());
    }

    public static Holder<MobEffect> steadyBreath() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(STEADY_BREATH.get());
    }

    private static final class CurioEffect extends MobEffect {
        private CurioEffect(int colour) {
            super(MobEffectCategory.BENEFICIAL, colour);
        }
    }

    private ModEffects() {
    }

    public static final RegistrySupplier<MobEffect> TANGLED =
            MOB_EFFECTS.register("tangled", TangledEffect::new);

    public static final RegistrySupplier<MobEffect> PLUCK = MOB_EFFECTS.register("pluck",
            () -> new PluckEffect()
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, PLUCK_DAMAGE,
                            PLUCK_DAMAGE_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, PLUCK_RESISTANCE,
                            PLUCK_RESISTANCE_PER_LEVEL, AttributeModifier.Operation.ADD_VALUE));

    public static Holder<MobEffect> pluck() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(PLUCK.get());
    }

    private static final class PluckEffect extends MobEffect {
        private PluckEffect() {
            super(MobEffectCategory.BENEFICIAL, PLUCK_COLOUR);
        }
    }

    public static Holder<MobEffect> tangled() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TANGLED.get());
    }

    public static void bootstrap() {
    }

    private static final class GraveFatigueEffect extends MobEffect {
        private GraveFatigueEffect() {
            super(MobEffectCategory.HARMFUL, GRAVE_FATIGUE_COLOUR);
            addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, GRAVE_MINING, GRAVE_MINING_FACTOR - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        private static AttributeModifier slowed(int amplifier) {
            return new AttributeModifier(GRAVE_MINING, Math.pow(GRAVE_MINING_FACTOR, amplifier + 1) - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override
        public void createModifiers(int amplifier, BiConsumer<Holder<
                Attribute>, AttributeModifier> output) {
            output.accept(Attributes.BLOCK_BREAK_SPEED, slowed(amplifier));
        }

        @Override
        public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
            AttributeInstance speed =
                    attributes.getInstance(Attributes.BLOCK_BREAK_SPEED);
            if (speed != null) {
                speed.removeModifier(GRAVE_MINING);
                speed.addPermanentModifier(slowed(amplifier));
            }
        }
    }

    private static final class TangledEffect extends MobEffect {
        private TangledEffect() {
            super(MobEffectCategory.HARMFUL, TANGLED_COLOUR);
        }

        @Override
        public void onEffectStarted(LivingEntity entity, int amplifier) {
            MobEffectInstance tangled = entity.getEffect(tangled());
            if (tangled != null) {
                NetworkHandler.broadcastTangled(entity, tangled.getDuration());
            }
        }
    }

    private static final class BleedEffect extends MobEffect {
        private BleedEffect() {
            super(MobEffectCategory.HARMFUL, BLEED_COLOUR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration <= 1 || duration % BLEED_DRIP_EVERY == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity.level() instanceof ServerLevel level)) {
                return true;
            }

            MobEffectInstance instance = entity.getEffect(bleed());
            if (instance != null && instance.getDuration() <= 1) {
                Bleed.comeDue(entity, amplifier);
                return true;
            }

            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    1 + amplifier, 0.2, 0.3, 0.2, 0.0);
            NetworkHandler.broadcastFlair(entity,
                    FlairCuePacket.DRIP, 1 + amplifier);
            return true;
        }
    }

    private static final class BurningEffect extends MobEffect {
        private BurningEffect() {
            super(MobEffectCategory.HARMFUL, BURNING_COLOUR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % Burning.EVERY == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            return Burning.burn(entity, amplifier);
        }

        @Override
        public ParticleOptions createParticleOptions(
                MobEffectInstance instance) {
            return ParticleTypes.FLAME;
        }
    }

    private static final class GuardBreakEffect extends MobEffect {
        private GuardBreakEffect() {
            super(MobEffectCategory.HARMFUL, GUARD_BREAK_COLOUR);
        }
    }

    private static final class StunEffect extends MobEffect {
        private static final double RING_HEIGHT = 0.42;
        private static final double RING_RADIUS = 0.34;

        private static final int PLACE_EVERY = 3;
        private static final double STEP_DEGREES = 71.0;

        private StunEffect() {
            this(STUN_COLOUR);
        }

        private StunEffect(int colour) {
            super(MobEffectCategory.HARMFUL, colour);
        }

        @Override
        public ParticleOptions createParticleOptions(
                MobEffectInstance instance) {
            return ModRegistries.STUN.get();
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % PLACE_EVERY == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity.level() instanceof ServerLevel level)) {
                return true;
            }

            if (SkullCrusherSkill.isGripped(entity)) {
                return true;
            }

            NetworkHandler.broadcastStunned(entity);

            double angle = Math.toRadians(entity.tickCount * STEP_DEGREES);
            double x = entity.getX() + Math.cos(angle) * RING_RADIUS;
            double z = entity.getZ() + Math.sin(angle) * RING_RADIUS;
            double y = entity.getY() + entity.getBbHeight() + RING_HEIGHT;

            level.sendParticles(ModRegistries.STUN.get(), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            return true;
        }
    }
}
