package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.mixin.client.ClientTextTooltipAccessor;
import org.gumel.jojoha.skill.Indicator;

import java.util.ArrayList;
import java.util.List;

public final class JojohaTooltip {
    public static final ResourceLocation TOOLTIP = menu("tooltip/tooltip_texture.png");

    public static final ResourceLocation[] CORNERS = {menu("tooltip/tooltip_corner.png"), menu("tooltip/tooltip_corner2.png"),
            menu("tooltip/tooltip_corner3.png"), menu("tooltip/tooltip_corner4.png")};
    public static final int CORNER = 18;

    public static final int[][] CORNER_GOLD = {{5, 3}, {12, 3}, {14, 12}, {3, 12}};
    public static final int FRAME_GOLD = 1;

    public static final int FRAME = 4;

    public static final int TEX = 64;
    public static final int BORDER = 4;

    public static final int TEXT_PAD = 1;

    public static final int MAX_TEXT_W = 160;

    private static final int COMPACT_MIN_W = 140;
    private static final int COMPACT_MAX_W = 220;
    private static final double COMPACT_ASPECT = 1.4;

    private static boolean armed;

    private JojohaTooltip() {
    }

    private static ResourceLocation menu(String file) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/" + file);
    }

    public static boolean isOurs(ItemStack stack) {
        return !stack.isEmpty()
                && Jojoha.MOD_ID.equals(
                        BuiltInRegistries.ITEM
                                .getKey(stack.getItem()).getNamespace());
    }

    public static void armNext() {
        armed = true;
    }

    public static void arm(ItemStack stack) {
        armed = isOurs(stack);
    }

    public static boolean isArmed() {
        return armed;
    }

    public static List<ClientTooltipComponent> compact(
            List<ClientTooltipComponent> parts) {
        Font font = Minecraft.getInstance().font;
        long area = 0;
        int widest = 0;
        for (ClientTooltipComponent part : parts) {
            if (part instanceof ClientTextTooltip) {
                int w = part.getWidth(font);
                area += (long) w * font.lineHeight;
                widest = Math.max(widest, w);
            }
        }
        int limit = (int) Math.round(Math.sqrt(area) * COMPACT_ASPECT);
        if (!parts.isEmpty()) {
            limit = Math.max(limit, parts.get(0).getWidth(font));
        }
        limit = Math.max(COMPACT_MIN_W, Math.min(COMPACT_MAX_W, limit));
        if (widest <= limit) {
            return parts;
        }

        List<ClientTooltipComponent> out =
                new ArrayList<>(parts.size() + 4);
        for (ClientTooltipComponent part : parts) {
            if (part instanceof ClientTextTooltip text
                    && part.getWidth(font) > limit) {
                FormattedCharSequence line = ((ClientTextTooltipAccessor) text).jojoha$text();
                for (FormattedCharSequence piece : wrap(font, styled(line, 0, Integer.MAX_VALUE), limit)) {
                    out.add(ClientTooltipComponent.create(piece));
                }
            } else {
                out.add(part);
            }
        }
        return out;
    }

    public static List<FormattedCharSequence> wrap(Font font, FormattedText text,
                                                             int width) {
        FormattedCharSequence whole = Language.getInstance().getVisualOrder(text);
        int[] lead = new int[2];
        int[] seen = {0};
        whole.accept((index, style, codePoint) -> {
            lead[seen[0]] = codePoint;
            seen[0]++;
            return seen[0] < 2;
        });
        if (seen[0] < 2 || !Indicator.isGlyph(lead[0]) || lead[1] != ' ') {
            return font.split(text, width);
        }
        FormattedText head = styled(whole, 0, 2);
        int indent = font.width(head);
        List<FormattedCharSequence> pieces =
                font.split(styled(whole, 2, Integer.MAX_VALUE), Math.max(1, width - indent));
        List<FormattedCharSequence> out = new ArrayList<>(Math.max(1, pieces.size()));
        FormattedCharSequence headLine = Language.getInstance().getVisualOrder(head);
        if (pieces.isEmpty()) {
            out.add(headLine);
            return out;
        }
        FormattedCharSequence gap = Indicator.indent(indent).getVisualOrderText();
        for (int i = 0; i < pieces.size(); i++) {
            out.add(FormattedCharSequence.composite(i == 0 ? headLine : gap, pieces.get(i)));
        }
        return out;
    }

    private static FormattedText styled(FormattedCharSequence line, int from, int to) {
        MutableComponent whole = Component.empty();
        StringBuilder run = new StringBuilder();
        Style[] runStyle = {null};
        int[] at = {0};
        line.accept((index, style, codePoint) -> {
            int here = at[0]++;
            if (here < from || here >= to) {
                return true;
            }
            if (runStyle[0] != null && !style.equals(runStyle[0])) {
                whole.append(Component.literal(run.toString()).setStyle(runStyle[0]));
                run.setLength(0);
            }
            runStyle[0] = style;
            run.appendCodePoint(codePoint);
            return true;
        });
        if (runStyle[0] != null) {
            whole.append(Component.literal(run.toString()).setStyle(runStyle[0]));
        }
        return whole;
    }

    public static boolean consumeArmed() {
        boolean was = armed;
        armed = false;
        return was;
    }

    public static void frame(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        guiGraphics.flush();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT,
                Minecraft.ON_OSX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        solid(guiGraphics, x + 1, y + 1, w - 2, h - 2, BACKGROUND);
        solid(guiGraphics, x + 1, y, w - 2, 1, BACKGROUND);
        solid(guiGraphics, x + 1, y + h - 1, w - 2, 1, BACKGROUND);
        solid(guiGraphics, x, y + 1, 1, h - 2, BACKGROUND);
        solid(guiGraphics, x + w - 1, y + 1, 1, h - 2, BACKGROUND);
        solid(guiGraphics, x + 1, y + 1, w - 2, 1, BORDER_TOP);
        solid(guiGraphics, x + 1, y + h - 2, w - 2, 1, BORDER_BOTTOM);
        int rows = Math.max(1, h - 4);
        for (int row = 0; row < rows; row++) {
            int colour = mixArgb(BORDER_TOP, BORDER_BOTTOM, rows == 1 ? 0F : row / (float) (rows - 1));
            solid(guiGraphics, x + 1, y + 2 + row, 1, 1, colour);
            solid(guiGraphics, x + w - 2, y + 2 + row, 1, 1, colour);
        }
        guiGraphics.setColor(1F, 1F, 1F, 1F);
    }

    private static final ResourceLocation SOLID = menu("tooltip/solid.png");
    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER_TOP = 0x505000FF;
    private static final int BORDER_BOTTOM = 0x5028007F;

    private static void solid(GuiGraphics guiGraphics, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        guiGraphics.setColor(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F,
                (argb >>> 24) / 255F);
        guiGraphics.blit(SOLID, x, y, w, h, 0F, 0F, 1, 1, 4, 4);
    }

    private static int mixArgb(int from, int to, float share) {
        int a = Math.round((from >>> 24) * (1F - share) + (to >>> 24) * share);
        int r = Math.round(((from >> 16) & 0xFF) * (1F - share) + ((to >> 16) & 0xFF) * share);
        int g = Math.round(((from >> 8) & 0xFF) * (1F - share) + ((to >> 8) & 0xFF) * share);
        int b = Math.round((from & 0xFF) * (1F - share) + (to & 0xFF) * share);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void blackBox(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        frame(guiGraphics, x, y, w, h);
        solid(guiGraphics, x + 2, y + 2, w - 4, h - 4, BLACK);
        guiGraphics.setColor(1F, 1F, 1F, 1F);
    }

    private static final int BLACK = 0xFF000000;

    public interface Pane {
        int width();

        int height();

        void draw(GuiGraphics guiGraphics, int x, int y, int w, int h);
    }

    private static final int PANE_GAP = 4;
    private static final int PANE_FILL = 0xFF000000;

    public static void drawCompact(GuiGraphics guiGraphics, Font font, List<Component> lines,
                                   int mouseX, int mouseY, int screenW, int screenH, int pixel, float text,
                                   Pane pane) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(wrap(font, line, MAX_TEXT_W));
        }
        if (wrapped.isEmpty()) {
            return;
        }

        int textW = 0;
        for (FormattedCharSequence line : wrapped) {
            textW = Math.max(textW, font.width(line));
        }
        int inset = FRAME;
        int blockW = (int) Math.ceil(textW * text);
        int blockH = (int) Math.ceil((wrapped.size() * font.lineHeight - 1) * text);
        int paneW = pane == null ? 0 : pane.width() + PANE_GAP;
        int paneH = pane == null ? 0 : pane.height();
        int boxW = blockW + paneW + inset * 2;
        int boxH = Math.max(blockH, paneH) + inset * 2;

        int shownW = boxW * pixel;
        int shownH = boxH * pixel;

        int edge = 1;
        int x = Math.max(edge, Math.min(mouseX + 10, screenW - shownW - edge));
        int y = Math.max(edge, Math.min(mouseY - 10, screenH - shownH - edge));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 400F);
        guiGraphics.pose().scale(pixel, pixel, 1F);

        frame(guiGraphics, 0, 0, boxW, boxH);
        if (pane != null) {
            solid(guiGraphics, inset, inset, pane.width(), paneH, PANE_FILL);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(inset + paneW, inset, 0F);
        guiGraphics.pose().scale(text, text, 1F);
        int lineY = 0;
        for (FormattedCharSequence line : wrapped) {
            guiGraphics.drawString(font, line, 0, lineY, 0xFFFFFF, true);
            lineY += font.lineHeight;
        }
        guiGraphics.pose().popPose();

        guiGraphics.pose().popPose();

        if (pane != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0F, 0F, 400F);
            pane.draw(guiGraphics, x + inset * pixel, y + inset * pixel, pane.width() * pixel, paneH * pixel);
            guiGraphics.pose().popPose();
        }
    }

    public static void drawCompact(GuiGraphics guiGraphics, Font font, List<Component> lines,
                                   int mouseX, int mouseY, int screenW, int screenH, int pixel, float text) {
        drawCompact(guiGraphics, font, lines, mouseX, mouseY, screenW, screenH, pixel, text, null);
    }

    public static void corners(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        for (int i = 0; i < CORNERS.length; i++) {
            boolean left = i == 0 || i == 3;
            boolean top = i < 2;
            int goldX = left ? x + FRAME_GOLD : x + w - 1 - FRAME_GOLD;
            int goldY = top ? y + FRAME_GOLD : y + h - 1 - FRAME_GOLD;
            guiGraphics.blit(CORNERS[i], goldX - CORNER_GOLD[i][0], goldY - CORNER_GOLD[i][1], 0F, 0F,
                    CORNER, CORNER, CORNER, CORNER);
        }
    }

    public static void nineSlice(GuiGraphics guiGraphics, ResourceLocation texture,
                                 int x, int y, int w, int h, int border) {
        int b = border;
        int t = TEX;
        int inner = t - b * 2;
        int iw = Math.max(0, w - b * 2);
        int ih = Math.max(0, h - b * 2);
        int far = t - b;

        guiGraphics.blit(texture, x, y, b, b, 0F, 0F, b, b, t, t);
        guiGraphics.blit(texture, x + w - b, y, b, b, far, 0F, b, b, t, t);
        guiGraphics.blit(texture, x, y + h - b, b, b, 0F, far, b, b, t, t);
        guiGraphics.blit(texture, x + w - b, y + h - b, b, b, far, far, b, b, t, t);

        guiGraphics.blit(texture, x + b, y, iw, b, b, 0F, inner, b, t, t);
        guiGraphics.blit(texture, x + b, y + h - b, iw, b, b, far, inner, b, t, t);
        guiGraphics.blit(texture, x, y + b, b, ih, 0F, b, b, inner, t, t);
        guiGraphics.blit(texture, x + w - b, y + b, b, ih, far, b, b, inner, t, t);

        guiGraphics.blit(texture, x + b, y + b, iw, ih, b, b, inner, inner, t, t);
    }
}
