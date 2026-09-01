package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;

import static org.gumel.jojoha.registry.ModRegistries.MOB_EFFECTS;

/**
 * Status effects the mod applies.
 *
 * <h2>Stun</h2>
 *
 * <p>A real effect rather than a bag of tricks played on the victim. Stunning something needs to
 * work on a mob and on a player, and those are two very different things to hold still: a mob is
 * stopped by taking its speed away, a player by taking their input away. An effect is the one handle
 * both sides can read - the server takes the speed, and the client's input mixin sees the same
 * effect on the local player and stops sending movement.
 *
 * <p>Speed is removed by multiplication rather than subtraction so it holds whatever the victim's
 * base speed happens to be - a subtraction big enough to stop a horse would be nonsense on a bat.
 */
public final class ModEffects {
    private static final ResourceLocation STUN_SLOW =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stun_slow");
    private static final ResourceLocation STUN_DISARM =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stun_disarm");

    /** Bone white, so the icon reads as the skull the move shows. */
    private static final int STUN_COLOUR = 0xD8D8C8;
    private static final int TANGLED_COLOUR = 0x8A4FBF;

    public static final RegistrySupplier<MobEffect> STUN = MOB_EFFECTS.register("stun",
            () -> new StunEffect()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, STUN_SLOW, -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, STUN_DISARM, -1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /**
     * The effect as a holder the game will accept.
     *
     * <p>Not the {@code RegistrySupplier} itself, even though it implements {@link net.minecraft.core.Holder}.
     * That one is Architectury's own wrapper, and it is not the holder the vanilla registry hands
     * out - so saving an entity carrying it fails with "Unregistered holder in
     * ResourceKey[minecraft:mob_effect]" and takes the world save down with it. The registry's own
     * holder is the one that serialises.
     *
     * <p>Looked up each time rather than cached in a field: the registry is not populated when this
     * class initialises, so a field would be resolved too early and be wrong forever.
     */
    public static net.minecraft.core.Holder<MobEffect> stun() {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(STUN.get());
    }

    private ModEffects() {
    }

    /**
     * Wrapped in thorns: three of your moves are gone until it wears off.
     *
     * <p>Carries no attribute modifiers of its own. Everything it does is done by the code that
     * reads it - the skill gate refuses the blocked slots - and giving it a modifier as well would
     * be a second, invisible effect riding along with the visible one.
     *
     * <p>Which three is stored in the amplifier. See TangledPassive.
     */
    public static final RegistrySupplier<MobEffect> TANGLED =
            MOB_EFFECTS.register("tangled", TangledEffect::new);


    /** The registry's own holder, for the same reason stun() exists. */
    public static net.minecraft.core.Holder<MobEffect> tangled() {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TANGLED.get());
    }

    /** Touching the class is what runs its static initialisers, and so its registrations. */
    public static void bootstrap() {
    }

    /**
     * Thorns catching on somebody, for as long as they are caught.
     *
     * <p>Was a set of vine segments driven into the model, which was the literal reading and looked
     * like a mistake - solid geometry sticking out of a mob reads as clipping rather than as an
     * effect, and at seven of them it read as a lot of clipping. Short particles say the same thing
     * without asking the eye to accept a shape that is not part of the model.
     *
     * <p>Brief on purpose, and few. This has to be legible across a fight without becoming the thing
     * you are looking at - a haze thick enough to obscure the mob would cost more than the effect
     * is worth, and the effect only lasts two and a half seconds.
     */
    private static final class TangledEffect extends MobEffect {
        /** How high up the body they catch, and how far out from it. */
        private static final double FROM_HEIGHT = 0.15;
        private static final double TO_HEIGHT = 0.9;
        private static final double SPREAD = 0.42;

        /** How often a few appear, and how many. */
        private static final int EVERY = 3;
        private static final int PER_BURST = 2;

        private TangledEffect() {
            super(MobEffectCategory.HARMFUL, TANGLED_COLOUR);
        }

        /**
         * Every third tick rather than every one.
         *
         * <p>Vanilla runs this on every tick by default, and at twenty bursts a second the mob is
         * inside a cloud rather than wearing a few thorns. Three is about the rate at which one
         * fades as the next appears.
         */
        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % EVERY == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity.level() instanceof ServerLevel level)) {
                return true;
            }

            double height = entity.getBbHeight();
            double width = Math.max(0.3, entity.getBbWidth());

            for (int i = 0; i < PER_BURST; i++) {
                // Round the body rather than in a ring at one height, so they read as catching all
                // over rather than as a belt.
                double around = level.random.nextDouble() * Math.PI * 2;
                double out = SPREAD * width * (0.6 + level.random.nextDouble() * 0.5);
                double up = Mth.lerp(level.random.nextDouble(), FROM_HEIGHT, TO_HEIGHT) * height;

                double x = entity.getX() + Math.cos(around) * out;
                double y = entity.getY() + up;
                double z = entity.getZ() + Math.sin(around) * out;

                // No spread and no speed: the position is the whole point, and letting the server
                // scatter them would undo the placement it just worked out.
                level.sendParticles(ModRegistries.TANGLED_THORN.get(), x, y, z, 1,
                        0.0, 0.0, 0.0, 0.0);
            }
            return true;
        }

        /**
         * The mark, never a coloured mote.
         *
         * <p>Same reasoning as the stun's: vanilla would otherwise spray this effect's colour over
         * the whole entity whenever an instance is applied with its visible flag set, and a second
         * particle nobody chose fighting the one above is worse than either alone.
         */
        @Override
        public net.minecraft.core.particles.ParticleOptions createParticleOptions(
                MobEffectInstance instance) {
            return ModRegistries.TANGLED_THORN.get();
        }
    }

    /** Named rather than anonymous so the effect has a class to hang behaviour on later. */
    private static final class StunEffect extends MobEffect {
        /** How high over the head the ring sits, and how wide it is, in blocks. */
        private static final double RING_HEIGHT = 0.42;
        private static final double RING_RADIUS = 0.34;

        /** How many ticks apart the marks are placed, and how far round the ring each step goes. */
        private static final int PLACE_EVERY = 3;
        private static final double STEP_DEGREES = 71.0;

        private StunEffect() {
            super(MobEffectCategory.HARMFUL, STUN_COLOUR);
        }

        /**
         * The mark, never a coloured mote.
         *
         * <p>Vanilla builds a particle out of the effect colour and sprays it over the whole entity
         * whenever an instance is applied with its visible flag set. This colour is a near-white, so
         * a stunned mob came out looking whitewashed - pale specks from head to foot, which is not a
         * status indicator, it is fog.
         *
         * <p>Every place the mod applies this effect passes visible false, so in practice none of
         * that runs. This is the guarantee rather than the fix: overridden so that the coloured mote
         * is not merely unused but unreachable, and anything that ever applies stun visibly - a
         * command, another mod, a later change here - gets the ring mark instead of a white haze.
         *
         * <p>Returns a particle rather than null on purpose. Null is the tempting answer and the
         * callers are not documented to accept one.
         */
        @Override
        public net.minecraft.core.particles.ParticleOptions createParticleOptions(
                MobEffectInstance instance) {
            return ModRegistries.STUN.get();
        }

        /**
         * Every third tick, not every tick.
         *
         * <p>The default is to run this on every one, and at twenty marks a second the ring stops
         * being a ring and becomes a solid disc of overlapping sprites. Three is roughly the rate at
         * which one mark has faded by the time its opposite number appears.
         */
        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % PLACE_EVERY == 0;
        }

        /**
         * Walks one step round the ring and drops a mark there.
         *
         * <p>Driven off the remaining duration rather than a stored angle, because a MobEffect is a
         * singleton - there is one instance for every stunned entity in the world, so it cannot hold
         * a position for any of them. The duration is per-entity, always moving, and already to
         * hand, which makes it the natural clock.
         *
         * <p>The step is 71 degrees: co-prime with a full turn, so consecutive marks land nowhere
         * near each other and the ring fills in evenly instead of crawling round it.
         */
        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (!(entity.level() instanceof ServerLevel level)) {
                return true;
            }

            // Nothing while they are still in the Stand's hand. The marks say "this one is not
            // getting up yet", which is a statement about the aftermath - during the grab they are
            // held by the head with a skull coming apart inside it, and a ring of stars over the top
            // of that is clutter sitting on the one thing the move wants you looking at.
            if (org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill.isGripped(entity)) {
                return true;
            }

            double angle = Math.toRadians(entity.tickCount * STEP_DEGREES);
            double x = entity.getX() + Math.cos(angle) * RING_RADIUS;
            double z = entity.getZ() + Math.sin(angle) * RING_RADIUS;
            double y = entity.getY() + entity.getBbHeight() + RING_HEIGHT;

            // Count of one and no spread: the position is the whole point, and letting the server
            // scatter them would undo the ring it just worked out.
            level.sendParticles(ModRegistries.STUN.get(), x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            return true;
        }
    }
}
