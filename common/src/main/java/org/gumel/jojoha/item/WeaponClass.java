package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.gumel.jojoha.skill.Indicator;

import java.util.List;
import java.util.Locale;

public enum WeaponClass {
    DAGGER(2.0F, 0F, 0F),

    RAPIER(1.0F, 0.10F, 0.75),

    SWORD(1.0F, 0F, 0F),

    SABRE(1.0F, 0F, 0F),

    SCYTHE(1.0F, 0F, 0F);

    private final float openerMultiplier;
    private final float armourPierce;
    private final double bonusReach;

    WeaponClass(float openerMultiplier, float armourPierce, double bonusReach) {
        this.openerMultiplier = openerMultiplier;
        this.armourPierce = armourPierce;
        this.bonusReach = bonusReach;
    }

    public float openerMultiplier() {
        return openerMultiplier;
    }

    public float armourPierce() {
        return armourPierce;
    }

    public double bonusReach() {
        return bonusReach;
    }

    public boolean thrusts() {
        return this == RAPIER;
    }

    public boolean chainsPerfects() {
        return this == SABRE;
    }

    public boolean reaps() {
        return this == SCYTHE;
    }

    public void appendLines(List<Component> lines) {
        String base = "tooltip.jojoha.weapon_class." + name().toLowerCase(Locale.ROOT) + ".";

        StringBuilder text = new StringBuilder();
        for (int line = 1; Language.getInstance().has(base + line); line++) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(Component.translatable(base + line).getString().trim());
        }
        for (String sentence : text.toString().split("(?<=[.!?])\\s+")) {
            if (!sentence.isBlank()) {
                lines.add(Indicator.ABILITY.line(
                        Component.literal(sentence)
                                .withStyle(ChatFormatting.DARK_AQUA)));
            }
        }
    }

    public boolean sweeps() {
        return this == SWORD;
    }
}
