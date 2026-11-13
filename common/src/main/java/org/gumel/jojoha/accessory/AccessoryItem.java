package org.gumel.jojoha.accessory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.List;

public class AccessoryItem extends Item {
    private final AccessorySlot slot;
    private final AccessoryRarity rarity;
    private boolean burdened;
    private ResourceLocation stand;
    private boolean pillarMen;

    public AccessoryItem(AccessorySlot slot, AccessoryRarity rarity, Properties properties) {
        super(properties);
        this.slot = slot;
        this.rarity = rarity;
    }

    public AccessoryItem burdened() {
        this.burdened = true;
        return this;
    }

    public AccessoryItem forStand(ResourceLocation standId) {
        this.stand = standId;
        return this;
    }

    public AccessoryItem forPillarMen() {
        this.pillarMen = true;
        return this;
    }

    public AccessorySlot slot() {
        return slot;
    }

    public AccessoryRarity rarity() {
        return rarity;
    }

    public boolean hasBurden() {
        return burdened;
    }

    public ResourceLocation stand() {
        return stand;
    }

    public boolean worksFor(Player player) {
        if (stand == null && !pillarMen) {
            return true;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data == null) {
            return false;
        }
        if (pillarMen) {
            Fate fate = Fate.of(data);
            return fate == Fate.PILLAR_MAN || fate == Fate.ULTIMATE_LIFEFORM;
        }
        return data.stand.isPresent() && stand.equals(data.stand.standId());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(rarity.colour());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SimpleContainer worn = Accessories.of(player);
        ItemStack current = worn.getItem(slot.ordinal());
        if (ItemStack.matches(held, current)) {
            return InteractionResultHolder.fail(held);
        }

        ItemStack back = current.isEmpty() ? held : current.copy();
        ItemStack on = player.isCreative() ? held.copy() : held.copyAndClear();
        worn.setItem(slot.ordinal(), on);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.sidedSuccess(back, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        lines.add(Component.translatable(rarity.translationKey()).withStyle(rarity.colour()));
        if (stand != null) {
            lines.add(Component.translatable("accessory.jojoha.for_stand",
                    Component.translatable("stand." + stand.getNamespace() + "." + stand.getPath()))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (pillarMen) {
            lines.add(Component.translatable("accessory.jojoha.for_pillar_men").withStyle(ChatFormatting.DARK_PURPLE));
        }
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (burdened) {
            lines.add(Component.translatable("accessory.jojoha.burden",
                    Component.translatable(getDescriptionId() + ".burden")).withStyle(ChatFormatting.RED));
        }
        lines.add(Component.translatable("accessory.jojoha.worn_in",
                Component.translatable(slot.translationKey())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
