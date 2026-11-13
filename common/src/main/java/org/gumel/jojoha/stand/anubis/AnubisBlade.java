package org.gumel.jojoha.stand.anubis;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.gumel.jojoha.Jojoha;

public final class AnubisBlade {
    public static final double DAMAGE = 7.0;

    public static final double SPEED = 1.8;

    public static final double REACH = 3.0;

    private static final ResourceLocation MODIFIER = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_blade");

    private AnubisBlade() {
    }

    public static void tick(ServerPlayer player, boolean out) {
        ItemStack held = player.getMainHandItem();
        set(player, Attributes.ATTACK_DAMAGE, out ? DAMAGE : Double.NaN, held);
        set(player, Attributes.ATTACK_SPEED, out ? SPEED : Double.NaN, held);
        set(player, Attributes.ENTITY_INTERACTION_RANGE, out ? REACH : Double.NaN, held);
    }

    public static void clear(ServerPlayer player) {
        tick(player, false);
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, double wanted, ItemStack held) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (Double.isNaN(wanted)) {
            if (instance.getModifier(MODIFIER) != null) {
                instance.removeModifier(MODIFIER);
            }
            return;
        }
        double offset = wanted - instance.getBaseValue() - fromItem(held, attribute);
        AttributeModifier current = instance.getModifier(MODIFIER);
        if (current != null && Math.abs(current.amount() - offset) < 1.0E-6) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER, offset,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static double fromItem(ItemStack held, Holder<Attribute> attribute) {
        double sum = 0.0;
        for (ItemAttributeModifiers.Entry entry : held.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.attribute().equals(attribute) && entry.slot().test(EquipmentSlot.MAINHAND)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum += entry.modifier().amount();
            }
        }
        return sum;
    }
}
