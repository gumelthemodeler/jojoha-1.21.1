package org.gumel.jojoha.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.combat.RapierThrust;
import org.gumel.jojoha.combat.ScytheSweep;
import org.gumel.jojoha.hamon.Conductivity;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.List;

public class BladeItem extends TieredItem {
    private static final Tool SWORD_TOOL = new Tool(
            List.of(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
                    Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)),
            1.0F, 2);

    private final WeaponClass weaponClass;

    public BladeItem(Tier tier, WeaponClass weaponClass, Properties properties) {
        super(tier, properties.component(DataComponents.TOOL, SWORD_TOOL));
        this.weaponClass = weaponClass;
    }

    public WeaponClass weaponClass() {
        return weaponClass;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        weaponClass.appendLines(lines);
        Conductivity.appendLine(stack, lines);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity holder) {
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(weaponClass.thrusts() || weaponClass.reaps()) || player.getCooldowns().isOnCooldown(this)
                || Anubis.wields(player)) {
            return InteractionResultHolder.pass(held);
        }

        if (held.getDamageValue() >= held.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(held);
        }

        if (player instanceof ServerPlayer server) {
            if (weaponClass.reaps()) {
                ScytheSweep.perform(server, held);
                player.getCooldowns().addCooldown(this, ScytheSweep.cooldownTicks());
            } else {
                RapierThrust.perform(server, held);
                player.getCooldowns().addCooldown(this, RapierThrust.COOLDOWN_TICKS);
            }
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }
}
