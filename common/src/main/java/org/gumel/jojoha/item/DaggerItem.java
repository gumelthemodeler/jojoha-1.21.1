package org.gumel.jojoha.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A short blade that can be thrown, after the trident.
 *
 * <p>Held to wind up and released to throw, using vanilla's own spear animation so the pose reads as
 * something a player already knows how to do. Below {@link #THROW_THRESHOLD_TICKS} nothing leaves
 * the hand - a throw has to be a decision, and without a floor every mis-click would launch your
 * weapon across the room.
 *
 * <p>Built on {@link SwordItem} rather than beside it, which is what gets the tier to mean anything:
 * durability, enchantability, the repair material, mining behaviour against webs and the sweep are
 * all a sword's, and a dagger differs from one in its numbers rather than its nature.
 *
 * <p>Those numbers are deliberately not a sword's. A dagger swings faster and hits softer - see
 * {@code ModItems} for the figures - and the point of the trade is that it is the only blade in the
 * game you can also throw. Once thrown you are unarmed until you fetch it, which is the cost that
 * keeps the throw from simply being free damage; nothing here brings it back on its own, and that
 * omission is the balance.
 */
public class DaggerItem extends SwordItem {
    /**
     * How long the wind-up must be held before anything is thrown, in ticks.
     *
     * <p>Vanilla's trident threshold. Matching it rather than picking a number means a player who
     * has thrown a trident already knows the timing of this.
     */
    public static final int THROW_THRESHOLD_TICKS = 10;

    /** How hard it leaves the hand. The trident's own figure, for the same reason as above. */
    private static final float SHOOT_POWER = 2.5F;
    private static final float SHOOT_INACCURACY = 1.0F;

    private final float throwDamage;

    public DaggerItem(Tier tier, float throwDamage, Properties properties) {
        super(tier, properties);
        this.throwDamage = throwDamage;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    /**
     * Effectively forever.
     *
     * <p>The wind-up is ended by letting go, not by running out - so this is a ceiling nobody
     * reaches rather than a duration. Vanilla's own weapons do the same.
     */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity holder) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // A dagger about to break is not thrown. It would land as a broken weapon, or shatter on
        // the way and leave nothing to pick up - either way the player loses the item to a throw
        // rather than to a swing, and cannot see it coming.
        if (held.getDamageValue() >= held.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(held);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(held);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity holder, int timeLeft) {
        if (!(holder instanceof Player player)) {
            return;
        }

        int held = this.getUseDuration(stack, holder) - timeLeft;
        if (held < THROW_THRESHOLD_TICKS) {
            return;
        }

        if (!level.isClientSide) {
            // Durability is spent on the throw itself, so a dagger cannot be thrown forever for
            // free the way a snowball can. The stack handed to the projectile is taken after this,
            // and is therefore the worn one - what lands is the dagger in the state you threw it.
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            ThrownDagger thrown = new ThrownDagger(level, player, stack, throwDamage);
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    SHOOT_POWER, SHOOT_INACCURACY);

            // Creative keeps the dagger and the throw both; everyone else has to go and get it.
            if (player.getAbilities().instabuild) {
                thrown.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            } else {
                player.getInventory().removeItem(stack);
            }

            level.addFreshEntity(thrown);
            level.playSound(null, thrown, SoundEvents.TRIDENT_THROW.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
