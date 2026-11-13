package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.stand.StandManifestation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public final class StandRitualText {
    public static final float SECONDS_PER_CHAR = 0.075F;

    private static final int HOLD_TICKS = 75;
    private static final int FADE_OUT_TICKS = 30;

    public static final int NARRATION_COLOR = 0xFF6EC7;
    public static final int STAND_NAME_COLOR = 0xFFD93B;

    private static final int BOTTOM_OFFSET = 74;

    private static final int HEADLINE_COLOR = 0xFFAA00;
    private static final int NAME_DELAY_TICKS = StandManifestation.NAME_AFTER_FULL_TICKS;
    private static final int HINT_AFTER_NAME_TICKS = 14;
    private static final int HINT_COLOR = 0xAAAAAA;
    private static final int CARD_HOLD_TICKS = 90;
    private static final int LINE_GAP = 11;
    private static final float POP_TICKS = 4F;
    private static final float POP_SCALE = 0.4F;
    private static final float APPEAR_TICKS = 2F;

    private static final RandomSource RANDOM = RandomSource.create();

    private record Line(Component text, float startTick, int color) {
    }

    private record Card(Component headline, Component name, Component hint, float startTick) {
    }

    private record PendingLine(Supplier<Component> text, float showAtTick, int color) {
    }

    private static Line current;
    private static int soundedChars;
    private static Card card;
    private static int cardSounded;
    private static final Deque<PendingLine> PENDING = new ArrayDeque<>();

    private StandRitualText() {
    }

    public static void show(Component text, float clientTimeTicks, int color) {
        current = new Line(text, clientTimeTicks, color);
        soundedChars = 0;
    }

    public static void showAwakened(Component headline, Component name, Component hint, float startTick) {
        card = new Card(headline, name, hint, startTick);
        cardSounded = 0;
    }

    public static void showAfter(Component text, float clientTimeTicks, int delayTicks, int color) {
        showAfter(() -> text, clientTimeTicks, delayTicks, color);
    }

    public static void showAfter(Supplier<Component> text, float clientTimeTicks, int delayTicks, int color) {
        PENDING.add(new PendingLine(text, clientTimeTicks + delayTicks, color));
    }

    public static void clear() {
        current = null;
        soundedChars = 0;
        card = null;
        cardSounded = 0;
        PENDING.clear();
    }

    public static int typeTicks(String text) {
        return Math.round(text.length() * SECONDS_PER_CHAR * 20F);
    }

    public static void tick(float clientTimeTicks) {
        while (!PENDING.isEmpty() && clientTimeTicks >= PENDING.peek().showAtTick()) {
            PendingLine next = PENDING.poll();
            current = new Line(next.text().get(), clientTimeTicks, next.color());
            soundedChars = 0;
        }

        if (card != null && clientTimeTicks >= card.startTick()) {
            current = null;
            PENDING.clear();
            if (clientTimeTicks - card.startTick() > cardTotalTicks(card)) {
                card = null;
            }
        }

        if (current != null && clientTimeTicks - current.startTick() > totalTicks(current)) {
            current = null;
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        float clientTimeTicks = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        Card shownCard = card;
        if (shownCard != null && clientTimeTicks >= shownCard.startTick()) {
            renderCard(guiGraphics, minecraft, shownCard, clientTimeTicks - shownCard.startTick());
            return;
        }

        Line line = current;
        if (line == null) {
            return;
        }

        float elapsed = clientTimeTicks - line.startTick();
        if (elapsed < 0F) {
            return;
        }

        String full = line.text().getString();
        int revealed = revealed(elapsed, full.length());
        if (revealed == 0) {
            return;
        }

        soundedChars = playTypingSounds(minecraft, full, revealed, soundedChars);

        String shown = full.substring(0, revealed);
        float alpha = fadeAlpha(elapsed, typeTicks(full), HOLD_TICKS);
        if (alpha <= 0F) {
            return;
        }

        Font font = minecraft.font;
        int x = (guiGraphics.guiWidth() - font.width(shown)) / 2;
        int y = guiGraphics.guiHeight() - BOTTOM_OFFSET;
        guiGraphics.drawString(font, shown, x, y, withAlpha(line.color(), alpha), true);
    }

    private static void renderCard(GuiGraphics guiGraphics, Minecraft minecraft, Card card, float elapsed) {
        String hint = card.hint() == null ? "" : card.hint().getString();
        int hintShown = revealed(elapsed - NAME_DELAY_TICKS - HINT_AFTER_NAME_TICKS, hint.length());
        cardSounded = playTypingSounds(minecraft, hint, hintShown, cardSounded);

        float alpha = fadeAlpha(elapsed, cardTypedTicks(card), CARD_HOLD_TICKS);
        if (alpha <= 0F) {
            return;
        }

        Font font = minecraft.font;
        int baseY = guiGraphics.guiHeight() - BOTTOM_OFFSET;
        int top = baseY - (hint.isEmpty() ? 1 : 2) * LINE_GAP;
        float centreX = guiGraphics.guiWidth() / 2F;

        pop(guiGraphics, font, card.headline(), centreX, top, elapsed, HEADLINE_COLOR, alpha);
        if (elapsed >= NAME_DELAY_TICKS) {
            pop(guiGraphics, font, card.name(), centreX, top + LINE_GAP, elapsed - NAME_DELAY_TICKS,
                    STAND_NAME_COLOR, alpha);
        }

        if (hintShown > 0) {
            String hintText = hint.substring(0, hintShown);
            guiGraphics.drawString(font, hintText, (guiGraphics.guiWidth() - font.width(hintText)) / 2, baseY,
                    withAlpha(HINT_COLOR, alpha), true);
        }
    }

    private static void pop(GuiGraphics guiGraphics, Font font, Component text, float centreX, int y, float age,
                            int colour, float alpha) {
        float left = 1F - Mth.clamp(age / POP_TICKS, 0F, 1F);
        float scale = 1F + POP_SCALE * left * left;
        float shown = alpha * Mth.clamp(age / APPEAR_TICKS, 0F, 1F);
        int width = font.width(text);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centreX, y + font.lineHeight / 2F, 0F);
        guiGraphics.pose().scale(scale, scale, 1F);
        guiGraphics.drawString(font, text, -width / 2, -font.lineHeight / 2, withAlpha(colour, shown), true);
        guiGraphics.pose().popPose();
    }

    private static int revealed(float elapsed, int length) {
        return elapsed <= 0F ? 0 : Mth.clamp(Math.round(elapsed / (SECONDS_PER_CHAR * 20F)), 0, length);
    }

    private static int playTypingSounds(Minecraft minecraft, String full, int revealed, int sounded) {
        if (revealed <= sounded) {
            return sounded;
        }

        boolean audible = false;
        for (int i = sounded; i < revealed; i++) {
            if (!Character.isWhitespace(full.charAt(i))) {
                audible = true;
            }
        }

        if (audible) {
            float pitch = 1.8F + RANDOM.nextFloat() * 0.25F;
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F));
        }
        return revealed;
    }

    private static int totalTicks(Line line) {
        return typeTicks(line.text().getString()) + HOLD_TICKS + FADE_OUT_TICKS;
    }

    private static int cardTypedTicks(Card card) {
        if (card.hint() == null) {
            return NAME_DELAY_TICKS + Math.round(POP_TICKS);
        }
        return NAME_DELAY_TICKS + HINT_AFTER_NAME_TICKS + typeTicks(card.hint().getString());
    }

    private static int cardTotalTicks(Card card) {
        return cardTypedTicks(card) + CARD_HOLD_TICKS + FADE_OUT_TICKS;
    }

    private static float fadeAlpha(float elapsed, int typeTicks, int holdTicks) {
        float fadeStart = typeTicks + holdTicks;
        if (elapsed <= fadeStart) {
            return 1F;
        }
        return Mth.clamp(1F - (elapsed - fadeStart) / FADE_OUT_TICKS, 0F, 1F);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 4, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
