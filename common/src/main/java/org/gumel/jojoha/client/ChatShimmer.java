package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.gumel.jojoha.speech.Speech;

public final class ChatShimmer {
    private static final float SPEED = 4F;
    private static final float SPACING = 0.35F;
    private static final float GLINT = 0.65F;
    private static final int PLAIN = 0xFFFFFF;

    private ChatShimmer() {
    }

    public static FormattedCharSequence wrap(FormattedCharSequence line) {
        return sink -> line.accept((index, style, codePoint) ->
                sink.accept(index, Speech.SHIMMER.equals(style.getFont()) ? glint(style, index) : style, codePoint));
    }

    private static Style glint(Style style, int index) {
        TextColor colour = style.getColor();
        int base = colour == null ? PLAIN : colour.getValue();
        float seconds = (Util.getMillis() % 100000L) / 1000F;
        float wave = Mth.sin(seconds * SPEED - index * SPACING);
        float lift = wave <= 0F ? 0F : wave * wave * GLINT;
        int r = Math.round(Mth.lerp(lift, (base >> 16) & 0xFF, 255F));
        int g = Math.round(Mth.lerp(lift, (base >> 8) & 0xFF, 255F));
        int b = Math.round(Mth.lerp(lift, base & 0xFF, 255F));
        return style.withColor(r << 16 | g << 8 | b);
    }
}
