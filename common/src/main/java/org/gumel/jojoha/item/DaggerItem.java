package org.gumel.jojoha.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.stand.anubis.Anubis;

public class DaggerItem extends BladeItem {
    public static final int THROW_THRESHOLD_TICKS = 10;

    private static final float SHOOT_POWER = 2.5F;
    private static final float SHOOT_INACCURACY = 1.0F;

    private final float throwDamage;

    public DaggerItem(Tier tier, float throwDamage, Properties properties) {
        super(tier, WeaponClass.DAGGER, properties);
        this.throwDamage = throwDamage;
    }

    public float throwDamage() {
        return throwDamage;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity holder) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && Anubis.wields(player)) {
            return InteractionResultHolder.pass(held);
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
            ThrownDagger thrown = new ThrownDagger(level, player, stack.copyWithCount(1), throwDamage);
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    SHOOT_POWER, SHOOT_INACCURACY);

            if (player.getAbilities().instabuild) {
                thrown.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            } else {
                stack.shrink(1);
            }

            level.addFreshEntity(thrown);
            level.playSound(null, thrown, SoundEvents.TRIDENT_THROW.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
