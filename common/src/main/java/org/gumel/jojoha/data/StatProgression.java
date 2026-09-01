package org.gumel.jojoha.data;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Where stat points come from: killing things that were trying to kill you.
 *
 * <p>Points existed before this and had no source outside a command, which made every stat in the
 * game a number an operator typed. This is the loop that earns them.
 *
 * <h2>Why a running total rather than a point per kill</h2>
 *
 * <p>A point per kill makes a zombie and a warden worth the same, and makes the first stat cheap
 * enough that nothing after it feels earned. A running total priced by how hard the thing was to
 * kill fixes both: a warden is worth twenty zombies because it is, and the total can be tuned in one
 * place without touching what any individual mob is worth.
 *
 * <p>The total carries over rather than resetting, so a run of small kills is never wasted - the
 * eighth zombie finishes what the first seven started.
 */
public final class StatProgression {
    /**
     * How much killing something is worth, per point of its maximum health.
     *
     * <p>Health rather than a table of mob types, because it already encodes what the table would
     * have said and it covers modded mobs nobody here has heard of. A zombie is worth five, an
     * enderman ten, a wither seventy-five.
     */
    private static final float WORTH_PER_HEALTH = 0.25F;

    /**
     * How much worth buys one point.
     *
     * <p>Thirty, so an ordinary hostile mob is about a sixth of a point and six of them is one.
     * Maxing a single stat is thirty-five points, which is a few hundred kills - a long arc rather
     * than an afternoon, and the only number to change if the whole curve feels wrong.
     */
    private static final int WORTH_PER_POINT = 30;

    private StatProgression() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(StatProgression::onDeath);
    }

    private static EventResult onDeath(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        // Never interferes with the death itself; it only watches. Returning pass rather than a
        // result is what keeps this out of the way of anything else listening.
        if (!(source.getEntity() instanceof ServerPlayer killer)) {
            return EventResult.pass();
        }

        int worth = worthOf(victim);
        if (worth <= 0) {
            return EventResult.pass();
        }

        award(killer, worth);
        return EventResult.pass();
    }

    /**
     * What this kill is worth, or nothing if it should not count.
     *
     * <p>Hostiles and players only. A stat system fed by anything that dies is a stat system fed by
     * a cow pen, and the fastest route to maximum strength should not be a farm.
     */
    private static int worthOf(LivingEntity victim) {
        if (!(victim instanceof Enemy) && !(victim instanceof Player)) {
            return 0;
        }

        return Math.max(1, Math.round(victim.getMaxHealth() * WORTH_PER_HEALTH));
    }

    /**
     * Progress earned by using a Stand rather than by killing with one.
     *
     * <p>The same pot and the same payout - this is not a second currency, it is a second way of
     * filling the first. A Stand that earns this way is not earning twice, because the Stands that
     * do are the ones whose kills are worth nothing to them. See UnorthodoxMethodPassive.
     */
    public static void awardUse(ServerPlayer player, int worth) {
        award(player, worth);
    }

    /** Adds to the running total and pays out whatever whole points it now covers. */
    private static void award(ServerPlayer player, int worth) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        int total = data.killProgress + worth;
        int earned = total / WORTH_PER_POINT;

        // The remainder stays. Anything else would quietly throw away most of a point every time
        // one was paid out.
        data.killProgress = total % WORTH_PER_POINT;

        if (earned > 0) {
            data.availableStatPoints += earned;
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        if (earned <= 0) {
            return;
        }

        player.displayClientMessage(Component.literal(
                "+" + earned + " stat point" + (earned == 1 ? "" : "s"))
                .withStyle(ChatFormatting.GOLD), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.35F, 1.6F);
    }

    /** How far along the next point is, 0 to 1 - for anything that wants to draw it. */
    public static float towardsNextPoint(JojohaPlayerData data) {
        return Math.min(1F, data.killProgress / (float) WORTH_PER_POINT);
    }
}
