package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * The Stand Arrow ritual's on-screen narration, typed out a character at a time.
 *
 * <p>Rendered as a HUD overlay rather than pushed through the vanilla action bar: the action bar
 * takes a finished string and fades it on its own schedule, so a typewriter through it would mean
 * a packet per character and no control over the timing.
 */
public final class StandRitualText {
    /**
     * Real seconds per character.
     *
     * <p>Two thirds slower than it was. The old speed finished a line in well under a second, which
     * is faster than the line can be read - so the typewriter was doing its work on text nobody had
     * caught up with, and by the time you had, it was already fading. A line should finish arriving
     * a moment before you finish reading it, not several before you start.
     */
    public static final float SECONDS_PER_CHAR = 0.075F;

    /**
     * How long the finished line sits before fading, and how long the fade takes.
     *
     * <p>Both lengthened with the typing. The hold is the part that is actually read - the typing is
     * only how it arrives - so a line that types slowly and then leaves at the old speed has moved
     * the problem rather than fixed it.
     */
    private static final int HOLD_TICKS = 75;
    private static final int FADE_OUT_TICKS = 30;

    /** The ritual's narration colour, and the gold the Stand's own name is called out in. */
    public static final int NARRATION_COLOR = 0xFF6EC7;
    public static final int STAND_NAME_COLOR = 0xFFD93B;

    /** Pixels above the hotbar. Clear of the action bar's own slot so the two never collide. */
    private static final int BOTTOM_OFFSET = 74;

    private static final RandomSource RANDOM = RandomSource.create();

    private record Line(Component text, float startTick, int color) {
    }

    /**
     * Holds a supplier rather than a finished Component: queued lines are scheduled well before
     * they appear, and the Stand's name in particular isn't known yet at scheduling time - the
     * grant that decides it happens on the very tick this line is due.
     */
    private record PendingLine(Supplier<Component> text, float showAtTick, int color) {
    }

    private static Line current;
    private static int soundedChars;
    private static final Deque<PendingLine> PENDING = new ArrayDeque<>();

    private StandRitualText() {
    }

    /** Types a line out starting now. */
    public static void show(Component text, float clientTimeTicks, int color) {
        current = new Line(text, clientTimeTicks, color);
        soundedChars = 0;
    }

    /** Queues a line to start typing once {@code delayTicks} have passed. */
    public static void showAfter(Component text, float clientTimeTicks, int delayTicks, int color) {
        showAfter(() -> text, clientTimeTicks, delayTicks, color);
    }

    /** Queues a line whose contents are resolved at the moment it appears, not when it's queued. */
    public static void showAfter(Supplier<Component> text, float clientTimeTicks, int delayTicks, int color) {
        PENDING.add(new PendingLine(text, clientTimeTicks + delayTicks, color));
    }

    public static void clear() {
        current = null;
        soundedChars = 0;
        PENDING.clear();
    }

    /** How long a given line takes to finish typing, in ticks. */
    public static int typeTicks(String text) {
        return Math.round(text.length() * SECONDS_PER_CHAR * 20F);
    }

    /** Call once per client tick - promotes queued lines and retires finished ones. */
    public static void tick(float clientTimeTicks) {
        while (!PENDING.isEmpty() && clientTimeTicks >= PENDING.peek().showAtTick()) {
            PendingLine next = PENDING.poll();
            current = new Line(next.text().get(), clientTimeTicks, next.color());
            soundedChars = 0;
        }

        if (current != null && clientTimeTicks - current.startTick() > totalTicks(current)) {
            current = null;
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        Line line = current;
        if (line == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // Includes the partial tick so characters appear on a smooth clock rather than in
        // 20-per-second steps, which at this typing speed would read as stuttering.
        float clientTimeTicks = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float elapsed = clientTimeTicks - line.startTick();
        if (elapsed < 0F) {
            return;
        }

        String full = line.text().getString();
        int revealed = Mth.clamp(Math.round(elapsed / (SECONDS_PER_CHAR * 20F)), 0, full.length());
        if (revealed == 0) {
            return;
        }

        playTypingSounds(minecraft, full, revealed);

        String shown = full.substring(0, revealed);
        float alpha = fadeAlpha(elapsed, typeTicks(full));
        if (alpha <= 0F) {
            return;
        }

        Font font = minecraft.font;
        int x = (guiGraphics.guiWidth() - font.width(shown)) / 2;
        int y = guiGraphics.guiHeight() - BOTTOM_OFFSET;
        guiGraphics.drawString(font, shown, x, y, withAlpha(line.color(), alpha), true);
    }

    /**
     * One tick per character as it lands. Spaces are skipped - a typewriter clicks on letters, and
     * sounding the gaps turns the rhythm into a flat rattle. At most one per frame, so a lag spike
     * that reveals several characters at once doesn't fire a burst of overlapping clicks.
     */
    private static void playTypingSounds(Minecraft minecraft, String full, int revealed) {
        if (revealed <= soundedChars) {
            return;
        }

        boolean audible = false;
        for (int i = soundedChars; i < revealed; i++) {
            if (!Character.isWhitespace(full.charAt(i))) {
                audible = true;
            }
        }
        soundedChars = revealed;

        if (audible) {
            float pitch = 1.8F + RANDOM.nextFloat() * 0.25F;
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F));
        }
    }

    private static int totalTicks(Line line) {
        return typeTicks(line.text().getString()) + HOLD_TICKS + FADE_OUT_TICKS;
    }

    private static float fadeAlpha(float elapsed, int typeTicks) {
        float fadeStart = typeTicks + HOLD_TICKS;
        if (elapsed <= fadeStart) {
            return 1F;
        }
        return Mth.clamp(1F - (elapsed - fadeStart) / FADE_OUT_TICKS, 0F, 1F);
    }

    /** drawString takes ARGB, and an alpha of 0 is treated as fully opaque - so it's floored. */
    private static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 4, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
