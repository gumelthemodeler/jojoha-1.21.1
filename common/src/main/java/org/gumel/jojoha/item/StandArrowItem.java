package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.StandLuck;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.fortune.Fortunes;
import org.gumel.jojoha.fortune.Gambler;

import java.util.List;

public final class StandArrowItem extends ArrowItem {
    private static final int REQUIRED_WORTHINESS = 15;

    private static final float ARROW_CHANCE_AT_THRESHOLD = 0.25F;
    private static final float ARROW_CHANCE_AT_FULL_ODDS = 0.50F;
    private static final float REJECTION_DAMAGE = 4.0F;

    private static final int FULL_ODDS_WORTHINESS = StatPoints.MAX_STAT;

    private static final float UNWORTHY_DAMAGE_PER_POINT = 1.5F;

    public static final float FATED_BONUS = 0.05F;

    public static final ResourceLocation FAVOUR_SPENT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "charge/heavens_favour");

    private final int requiredWorthiness;
    private final float chanceAtThreshold;
    private final float chanceAtFullOdds;
    private final boolean shard;

    public StandArrowItem(Properties properties) {
        this(properties, REQUIRED_WORTHINESS,
                ARROW_CHANCE_AT_THRESHOLD, ARROW_CHANCE_AT_FULL_ODDS, false);
    }

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

    public int requiredWorthiness() {
        return requiredWorthiness;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo,
                                                                            LivingEntity shooter,
                                                                            ItemStack weapon) {
        return new StandArrowProjectile(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position at,
                                                                        ItemStack stack,
                                                                        Direction direction) {
        StandArrowProjectile arrow = new StandArrowProjectile(level, at.x(), at.y(), at.z(), stack.copyWithCount(1));
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    public float successChance(int worthiness) {
        float span = FULL_ODDS_WORTHINESS - requiredWorthiness;
        float through = span <= 0F ? 1F
                : Mth.clamp((worthiness - requiredWorthiness) / span, 0F, 1F);
        return Mth.clamp(JojohaServerConfig.awakening(Mth.lerp(through, chanceAtThreshold, chanceAtFullOdds), false), 0F, 1F);
    }

    public float unworthyDamage(int worthiness) {
        return REJECTION_DAMAGE + Math.max(0, requiredWorthiness - worthiness) * UNWORTHY_DAMAGE_PER_POINT;
    }

    public float oddsFor(Player player, JojohaPlayerData data) {
        float chance = successChance(data.worthiness)
                + StandLuck.bonusFor(player);
        if (StatPerk.FATED.has(data)) {
            chance += FATED_BONUS;
        }
        chance *= Fortunes.arrowOdds(player, data);
        return Mth.clamp(chance, 0F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines,
                                TooltipFlag flag) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        Player viewer = ArrowTooltips.viewer();
        if (data.stand.isPresent()) {
            lines.add(Component.translatable("item.jojoha.stand_arrow.has_stand")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (data.worthiness < requiredWorthiness) {
            lines.add(Component.translatable("item.jojoha.stand_arrow.unworthy",
                    requiredWorthiness, data.worthiness).withStyle(ChatFormatting.RED));
            return;
        }
        int percent = Math.round((viewer != null ? oddsFor(viewer, data)
                : Mth.clamp(successChance(data.worthiness), 0F, 1F)) * 100F);
        lines.add(Component.translatable("item.jojoha.stand_arrow.odds",
                Component.literal(percent + "%")
                        .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND || player.getOffhandItem().getItem() instanceof AncientBowItem) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);
        if (data.stand.isPresent() || data.standArrowRitualTicks > 0) {
            return InteractionResultHolder.fail(stack);
        }
        if (Fate.barredFromStands(data)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.jojoha.ultimate.no_stand"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (data.worthiness < requiredWorthiness) {
            stack.shrink(1);
            player.hurt(serverLevel.damageSources().magic(), unworthyDamage(data.worthiness));
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.jojoha.stand_arrow.unworthy", requiredWorthiness), true);
            return InteractionResultHolder.success(stack);
        }

        if (Gambler.calls(serverPlayer)) {
            if (!Gambler.flip(serverPlayer)) {
                stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }
            Gambler.doubleRoll(serverPlayer);
        }

        data.standArrowRitualShard = shard;
        StandArrowRitual.begin(serverPlayer, data);
        return InteractionResultHolder.success(stack);
    }
}
