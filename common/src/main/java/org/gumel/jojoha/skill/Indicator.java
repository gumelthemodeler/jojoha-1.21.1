package org.gumel.jojoha.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Indicator {
    ABILITY('\uE701'),
    BLEED('\uE702'),
    HAMON('\uE703'),
    STUN('\uE704'),
    STUN_II('\uE705'),
    BURNING('\uE706'),
    GUARD_BREAK('\uE707');

    public static final char INDENT = '\uE70F';

    private final char glyph;

    Indicator(char glyph) {
        this.glyph = glyph;
    }

    public char glyph() {
        return glyph;
    }

    public MutableComponent icon() {
        return Component.literal(String.valueOf(glyph))
                .withStyle(style -> style.withFont(Inflicts.TAG_FONT).withColor(0xFFFFFF));
    }

    public MutableComponent line(Component text) {
        return Component.empty().append(icon()).append(Component.literal(" ").append(text));
    }

    public static boolean isGlyph(int codePoint) {
        if (codePoint == '\uE700') {
            return true;
        }
        for (Indicator indicator : values()) {
            if (indicator.glyph == codePoint) {
                return true;
            }
        }
        return false;
    }

    public static MutableComponent indent(int width) {
        return Component.literal(String.valueOf(INDENT).repeat(Math.max(0, width)))
                .withStyle(style -> style.withFont(Inflicts.TAG_FONT));
    }
}
