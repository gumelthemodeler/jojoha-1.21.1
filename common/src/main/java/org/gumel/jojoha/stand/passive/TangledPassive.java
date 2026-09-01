package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModEffects;

/**
 * Now and then a punch leaves the vines behind, wrapped round whatever it hit.
 *
 * <p>Hermit Purple's damage is poor and always will be, so what it contributes to a fight is not
 * hurting people - it is taking things away from them. Tangled is short and it is not a lock: it
 * costs the victim three of whatever they were relying on for a couple of seconds, which is long
 * enough to matter and far too short to be a stun-lock.
 *
 * <h2>Which three, and where that is stored</h2>
 *
 * <p>Nowhere - it is carried in the effect's own amplifier, which is the neat part.
 *
 * <p>Blocking three of eight slots means choosing one of fifty-six combinations, and an amplifier is
 * an integer that already syncs to the client, already saves with the entity, and already expires
 * with the effect. Numbering the combinations and storing the number means the choice needs no new
 * field, no packet and no cleanup - and a victim who logs out mid-effect comes back with the same
 * three moves missing rather than a fresh roll.
 *
 * <p>The alternative was a set of ids on the player's data, which is a codec change, a sync, and a
 * thing to remember to clear.
 */
public final class TangledPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tangled");

    public static final TangledPassive INSTANCE = new TangledPassive();

    /** How often a hit catches, and for how long once it does. */
    private static final float CHANCE = 0.18F;
    private static final int TANGLE_TICKS = 50;

    /** The stagger that comes with being wrapped up, which is much shorter than the tangle. */
    private static final int STUN_TICKS = 10;

    /** How many of the bar's slots go, and how many there are to choose from. */
    public static final int BLOCKED = 3;
    public static final int SLOTS = 8;

    /** Every way of choosing three slots from eight, worked out once. */
    private static final int[][] COMBINATIONS = combinations();

    private TangledPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.tangled";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        if (!(player.level() instanceof ServerLevel level) || target.level().isClientSide()) {
            return amount;
        }

        // Only if it is not already caught. Re-rolling on every hit of a barrage would turn a small
        // chance into a certainty and a short effect into a permanent one.
        if (target.hasEffect(ModEffects.tangled()) || level.random.nextFloat() >= CHANCE) {
            return amount;
        }

        int which = level.random.nextInt(COMBINATIONS.length);
        target.addEffect(new MobEffectInstance(ModEffects.tangled(), TANGLE_TICKS, which,
                false, true, true));

        // A moment of being wrapped up before they get moving again.
        target.addEffect(new MobEffectInstance(ModEffects.stun(), STUN_TICKS, 0,
                false, false, true));

        // The damage is untouched. This passive does not hit harder, it hits stickier.
        return amount;
    }

    /** Whether a tangled entity has lost the use of this slot. */
    public static boolean blocks(int amplifier, int slot) {
        if (amplifier < 0 || amplifier >= COMBINATIONS.length) {
            return false;
        }

        for (int blocked : COMBINATIONS[amplifier]) {
            if (blocked == slot) {
                return true;
            }
        }
        return false;
    }

    /** The fifty-six ways of choosing three from eight, in a fixed order both sides agree on. */
    private static int[][] combinations() {
        int count = 0;
        for (int a = 0; a < SLOTS - 2; a++) {
            for (int b = a + 1; b < SLOTS - 1; b++) {
                count += SLOTS - 1 - b;
            }
        }

        int[][] all = new int[count][BLOCKED];
        int at = 0;
        for (int a = 0; a < SLOTS - 2; a++) {
            for (int b = a + 1; b < SLOTS - 1; b++) {
                for (int c = b + 1; c < SLOTS; c++) {
                    all[at][0] = a;
                    all[at][1] = b;
                    all[at][2] = c;
                    at++;
                }
            }
        }
        return all;
    }
}
