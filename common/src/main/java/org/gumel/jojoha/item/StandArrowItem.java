package org.gumel.jojoha.item;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandTypes;

/**
 * Right-click to gain a Stand - per the design doc's Stand Obtainment section: "Arrows draw
 * from the general pool... If a player lacks the required WOR, the arrow simply damages them
 * instead of granting a stand." Placeholder numbers (required WOR, rejection damage, granted
 * stand stats) - only one Stand exists in the pool right now (Star Platinum), so that's what
 * a successful draw always grants.
 */
public final class StandArrowItem extends Item {
    private static final int REQUIRED_WORTHINESS = 15;
    private static final float REJECTION_DAMAGE = 4.0F;

    /**
     * The worthiness at which a piece is as likely to work as it will ever be.
     *
     * <p>The odds run from the threshold that lets you use the thing at all up to here. Beyond it
     * nothing improves - a shard is a broken piece of an arrow, and no amount of being worthy makes
     * a broken thing whole.
     */
    private static final int FULL_ODDS_WORTHINESS = 100;

    private final int requiredWorthiness;
    private final float chanceAtThreshold;
    private final float chanceAtFullOdds;
    private final boolean shard;

    public StandArrowItem(Properties properties) {
        this(properties, REQUIRED_WORTHINESS, 1F, 1F, false);
    }

    /**
     * @param chanceAtThreshold odds of awakening at exactly {@code requiredWorthiness}
     * @param chanceAtFullOdds  odds at {@link #FULL_ODDS_WORTHINESS} and above
     * @param shard             whether this is a fragment, which the ritual records to spend the
     *                          right item and to roll the right odds
     */
    public StandArrowItem(Properties properties, int requiredWorthiness,
                          float chanceAtThreshold, float chanceAtFullOdds, boolean shard) {
        super(properties);
        this.requiredWorthiness = requiredWorthiness;
        this.chanceAtThreshold = chanceAtThreshold;
        this.chanceAtFullOdds = chanceAtFullOdds;
        this.shard = shard;
    }

    public boolean isShard() {
        return shard;
    }

    /**
     * How likely this is to actually wake a Stand, for a player of the given worthiness.
     *
     * <p>Linear between the two ends, because the player is meant to be able to feel the number
     * moving as their worthiness climbs. A whole arrow is simply 1 at both ends and so never rolls;
     * a shard is worse everywhere and stays worse at the top, which is the point of it.
     */
    public float successChance(int worthiness) {
        float span = FULL_ODDS_WORTHINESS - requiredWorthiness;
        float through = span <= 0F ? 1F
                : Mth.clamp((worthiness - requiredWorthiness) / span, 0F, 1F);
        return Mth.clamp(Mth.lerp(through, chanceAtThreshold, chanceAtFullOdds), 0F, 1F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Main hand only. There is one stab animation and it swings the right arm, so an off-hand
        // use would play a stab that doesn't match the hand the arrow is actually in. Passing
        // rather than failing leaves the off-hand click free to fall through to whatever is behind
        // it, exactly as if the slot were empty.
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);
        if (data.stand.isPresent() || data.standArrowRitualTicks > 0) {
            return InteractionResultHolder.fail(stack);
        }

        if (data.worthiness < requiredWorthiness) {
            stack.shrink(1);
            player.hurt(serverLevel.damageSources().magic(), REJECTION_DAMAGE);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            return InteractionResultHolder.success(stack);
        }

        // The arrow is deliberately NOT consumed here - it stays in hand for the whole stab and is
        // only spent once the Stand actually awakens (StandArrowRitual), so the item vanishing
        // lines up with the payoff instead of with the click.
        data.standArrowRitualShard = shard;
        StandArrowRitual.begin(serverPlayer, data);
        return InteractionResultHolder.success(stack);
    }
}
