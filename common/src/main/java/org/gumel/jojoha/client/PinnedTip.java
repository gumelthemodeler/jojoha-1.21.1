package org.gumel.jojoha.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.gumel.jojoha.skill.Inflicts;

import java.util.ArrayList;
import java.util.List;

final class PinnedTip {
    private static final int COLOR_DEFINITION = 0xFFA0A0A0;
    private static final int COLOR_HINT = 0xFF707070;
    private static final int COLOR_OVER = 0xFF7FE3FF;
    private static final int BODY = -1;
    private static final int HINT = -2;

    private final List<Component> lines;
    private final List<Inflicts.Status> keywords;
    private final boolean[] open;
    private final int anchorX;
    private final int anchorY;

    private final List<FormattedCharSequence> shown = new ArrayList<>();
    private final List<Integer> owners = new ArrayList<>();
    private int x;
    private int y;
    private int pixel = 1;
    private float text = 1F;
    private int lineHeight = 9;
    private int boxW;
    private int boxH;

    PinnedTip(List<Component> lines, List<Inflicts.Status> keywords, int anchorX, int anchorY) {
        this.lines = List.copyOf(lines);
        this.keywords = List.copyOf(keywords);
        this.open = new boolean[keywords.size()];
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    void draw(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, int screenW, int screenH,
              int pixel, float text) {
        this.pixel = pixel;
        this.text = text;
        this.lineHeight = font.lineHeight;
        layout(font, keywordAt(mouseX, mouseY));

        int textW = 0;
        for (FormattedCharSequence line : shown) {
            textW = Math.max(textW, font.width(line));
        }
        int inset = JojohaTooltip.FRAME;
        boxW = (int) Math.ceil(textW * text) + inset * 2;
        boxH = (int) Math.ceil((shown.size() * lineHeight - 1) * text) + inset * 2;

        int edge = 1;
        x = Math.max(edge, Math.min(anchorX + 10, screenW - boxW * pixel - edge));
        y = Math.max(edge, Math.min(anchorY - 10, screenH - boxH * pixel - edge));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 400F);
        guiGraphics.pose().scale(pixel, pixel, 1F);
        JojohaTooltip.frame(guiGraphics, 0, 0, boxW, boxH);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(inset, inset, 0F);
        guiGraphics.pose().scale(text, text, 1F);
        for (int i = 0; i < shown.size(); i++) {
            guiGraphics.drawString(font, shown.get(i), 0, i * lineHeight, 0xFFFFFFFF, true);
        }
        guiGraphics.pose().popPose();
        guiGraphics.pose().popPose();
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + boxW * pixel && mouseY >= y && mouseY < y + boxH * pixel;
    }

    boolean click(double mouseX, double mouseY) {
        int keyword = keywordAt(mouseX, mouseY);
        if (keyword < 0) {
            return false;
        }
        open[keyword] = !open[keyword];
        return true;
    }

    private int keywordAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int inset = JojohaTooltip.FRAME;
        double local = (mouseY - y) / pixel - inset;
        int row = (int) Math.floor(local / (lineHeight * text));
        if (row < 0 || row >= owners.size()) {
            return -1;
        }
        return owners.get(row);
    }

    private void layout(Font font, int over) {
        shown.clear();
        owners.clear();
        for (Component line : lines) {
            add(font, line, BODY);
        }
        for (int i = 0; i < keywords.size(); i++) {
            Inflicts.Status status = keywords.get(i);
            int colour = i == over ? COLOR_OVER : status.colour();
            add(font, Component.literal(open[i] ? "- " : "+ ").withStyle(s -> s.withColor(colour))
                    .append(status.tagged(colour)), i);
            if (open[i]) {
                add(font, Component.literal("  ").append(status.definition())
                        .withStyle(s -> s.withColor(COLOR_DEFINITION)), i);
            }
        }
        add(font, Component.literal(keywords.isEmpty() ? "Click away to close"
                : "Click a keyword to expand it").withStyle(s -> s.withColor(COLOR_HINT)), HINT);
    }

    private void add(Font font, Component line, int owner) {
        for (FormattedCharSequence part : JojohaTooltip.wrap(font, line, JojohaTooltip.MAX_TEXT_W)) {
            shown.add(part);
            owners.add(owner);
        }
    }
}
