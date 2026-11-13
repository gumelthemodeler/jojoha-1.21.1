package org.gumel.jojoha.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.item.BladeItem;
import org.gumel.jojoha.item.ClackersItem;
import org.gumel.jojoha.item.DaggerItem;
import org.gumel.jojoha.item.ThrownDagger;
import org.gumel.jojoha.item.WeaponClass;
import org.gumel.jojoha.stand.anubis.Anubis;

public final class WeaponClasses {
    private static final double BEHIND_DOT = -0.4;

    private WeaponClasses() {
    }

    public static WeaponClass of(ItemStack stack) {
        if (stack.getItem() instanceof BladeItem blade) {
            return blade.weaponClass();
        }
        if (stack.getItem() instanceof DaggerItem) {
            return WeaponClass.DAGGER;
        }
        return null;
    }

    public static boolean reaps(ItemStack stack) {
        WeaponClass weapon = of(stack);
        return weapon != null && weapon.reaps();
    }

    public static boolean swingsItself(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (of(stack) != null || stack.getItem() instanceof ClackersItem
                || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES) || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE)) {
            return true;
        }
        for (ItemAttributeModifiers.Entry entry : stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)
                    && entry.modifier().amount() > 0.0) {
                return true;
            }
        }
        return false;
    }

    public static WeaponClass held(LivingEntity attacker) {
        if (Anubis.wields(attacker)) {
            return null;
        }
        return of(attacker.getMainHandItem());
    }

    public static boolean reaps(LivingEntity holder) {
        WeaponClass weapon = held(holder);
        return weapon != null && weapon.reaps();
    }

    private static WeaponClass responsible(DamageSource source, LivingEntity attacker) {
        if (source.getDirectEntity() instanceof ThrownDagger thrown) {
            return of(thrown.getWeaponItem());
        }
        if (source.getDirectEntity() != attacker) {
            return null;
        }
        return held(attacker);
    }

    public static float strikeMultiplier(DamageSource source, LivingEntity victim) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return 1F;
        }

        WeaponClass weapon = responsible(source, attacker);
        if (weapon == null) {
            return 1F;
        }

        if (weapon.openerMultiplier() > 1F && isOpener(attacker, victim)) {
            return weapon.openerMultiplier();
        }

        return 1F;
    }

    public static float armourPierce(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return 0F;
        }

        if (RapierThrust.isThrusting(attacker)) {
            return RapierThrust.PIERCE;
        }

        WeaponClass weapon = responsible(source, attacker);
        return weapon == null ? 0F : weapon.armourPierce();
    }

    private static boolean isOpener(LivingEntity attacker, LivingEntity victim) {
        if (victim instanceof Mob mob) {
            return mob.getTarget() != attacker;
        }

        if (victim instanceof Player) {
            Vec3 facing = victim.getLookAngle();
            Vec3 toAttacker = attacker.position().subtract(victim.position()).normalize();
            return facing.x * toAttacker.x + facing.z * toAttacker.z <= BEHIND_DOT;
        }

        return false;
    }
}
