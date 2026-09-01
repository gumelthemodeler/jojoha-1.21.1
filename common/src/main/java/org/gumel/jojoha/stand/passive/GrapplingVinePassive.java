package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hermit Purple keeps its user off the ground and unbothered by it.
 *
 * <p>Three small things that all say the same one: this Stand is a way of getting about. It cannot
 * fight worth anything, so what it gives instead is the confidence to be somewhere awkward - up a
 * wall, mid-swing, dropping off a roof - without the game punishing you for it.
 *
 * <h2>Knockback only while airborne</h2>
 *
 * <p>On the ground, being hit should still move you; that is a fight working properly. In the air it
 * is something else entirely, because there is a rope involved and being knocked off it costs the
 * height you spent the last ten seconds earning. So the resistance goes on when your feet leave the
 * floor and comes off when they touch it.
 *
 * <h2>Fall damage by the increment, not by the total</h2>
 *
 * <p>The obvious way to soften a fall is to scale the accumulated distance every tick, and it does
 * not work: scaling the same running total repeatedly converges it to nothing, so a factor meant to
 * take off a third takes off everything and the passive is quietly full immunity.
 *
 * <p>What is scaled here is the <em>growth</em> - how much further you fell this tick - which is the
 * thing a percentage was always meant to describe. Fall twice as far and you still take twice as
 * much; you simply take less of it than somebody without a Stand made of rope.
 */
public final class GrapplingVinePassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine");

    public static final GrapplingVinePassive INSTANCE = new GrapplingVinePassive();

    private static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine_speed");
    private static final ResourceLocation AIR_KNOCKBACK_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine_air");

    /** How much quicker on foot, and how firmly planted in the air. */
    private static final double SPEED_BONUS = 0.12;
    private static final double AIR_KNOCKBACK = 1.0;

    /** How much of each tick's new fall distance actually counts. */
    private static final float FALL_KEPT = 0.55F;

    /** What the fall distance was last tick, so the growth can be told from the total. */
    private static final Map<UUID, Float> LAST_FALL = new HashMap<>();

    private GrapplingVinePassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.grappling_vine";
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);

        set(player, Attributes.KNOCKBACK_RESISTANCE, AIR_KNOCKBACK_ID, AIR_KNOCKBACK,
                AttributeModifier.Operation.ADD_VALUE, !player.onGround());

        soften(player);
    }

    /**
     * Takes a share off however much further they fell this tick.
     *
     * <p>Reset on landing rather than left to grow, so the next fall starts from nothing and the
     * map cannot fill up with people who once jumped.
     */
    private static void soften(ServerPlayer player) {
        UUID id = player.getUUID();

        if (player.onGround() || player.fallDistance <= 0F) {
            LAST_FALL.remove(id);
            return;
        }

        float previous = LAST_FALL.getOrDefault(id, 0F);
        float growth = player.fallDistance - previous;

        if (growth > 0F) {
            player.fallDistance = previous + growth * FALL_KEPT;
        }
        LAST_FALL.put(id, player.fallDistance);
    }

    /** Adds, removes or refreshes one modifier, without churning it every tick. */
    private static void set(ServerPlayer player,
                           net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                           ResourceLocation id, double amount,
                           AttributeModifier.Operation operation, boolean wanted) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (wanted == (existing != null)) {
            return;
        }

        if (wanted) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else {
            instance.removeModifier(id);
        }
    }
}
