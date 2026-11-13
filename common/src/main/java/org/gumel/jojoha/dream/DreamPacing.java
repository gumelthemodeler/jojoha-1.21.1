package org.gumel.jojoha.dream;

import net.minecraft.network.chat.Component;

import java.util.List;

public final class DreamPacing {
    public static final float LETTERS_PER_TICK = 1.25F;

    public static final int STOP_TICKS = 9;
    public static final int BREATH_TICKS = 4;

    public static final int HOLD_TICKS = 26;
    public static final float HOLD_PER_LETTER = 0.3F;

    private DreamPacing() {
    }

    private static float cost(String text, int index) {
        float ticks = 1F / LETTERS_PER_TICK;
        char c = text.charAt(index);
        boolean last = index == text.length() - 1;
        boolean spaced = last || Character.isWhitespace(text.charAt(index + 1));
        if (!last && spaced) {
            if (c == '.' || c == '?' || c == '!') {
                ticks += STOP_TICKS;
            } else if (c == ',' || c == ';' || c == ':' || c == '\u2014') {
                ticks += BREATH_TICKS;
            }
        }
        return ticks;
    }

    public static int shown(String text, float elapsed) {
        float spent = 0F;
        for (int i = 0; i < text.length(); i++) {
            spent += cost(text, i);
            if (spent > elapsed) {
                return i;
            }
        }
        return text.length();
    }

    public static boolean pausing(String text, float elapsed) {
        float spent = 0F;
        for (int i = 0; i < text.length(); i++) {
            float letter = 1F / LETTERS_PER_TICK;
            float next = spent + cost(text, i);
            if (elapsed < next) {
                return elapsed >= spent + letter;
            }
            spent = next;
        }
        return true;
    }

    public static float typing(String text) {
        float spent = 0F;
        for (int i = 0; i < text.length(); i++) {
            spent += cost(text, i);
        }
        return spent;
    }

    public static float hold(String text) {
        return HOLD_TICKS + text.length() * HOLD_PER_LETTER;
    }

    public static int ticks(List<Component> lines) {
        float total = 0F;
        for (Component line : lines) {
            String text = line.getString();
            total += typing(text) + hold(text);
        }
        return (int) Math.ceil(total);
    }
}
