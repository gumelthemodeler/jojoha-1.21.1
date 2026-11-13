package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.network.packet.QuestBannerPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class QuestBanner {
    private static final int PAD = 10;
    private static final int HEIGHT = 26;
    private static final int MIN_WIDTH = 240;
    private static final int ICON = 16;
    private static final int ICON_GAP = 4;
    private static final int SECTION_GAP = 14;
    private static final long IN_MS = 250L;
    private static final long STAGGER_MS = 140L;
    private static final long POP_MS = 220L;
    private static final long HOLD_MS = 4500L;
    private static final long OUT_MS = 400L;
    private static final int HEAD_COLOUR = 0xFFE0B040;
    private static final int NEW_COLOUR = 0xFF9FD0FF;
    private static final int NAME_COLOUR = 0xFFFFFFFF;
    private static final int GAIN_COLOUR = 0xFF70E070;
    private static final int DIVIDER_GAP = 7;

    private record Shown(QuestBannerPacket packet, List<ItemStack> stacks, long at) {
    }

    @Nullable
    private static Shown shown;

    private QuestBanner() {
    }

    public static void show(QuestBannerPacket packet) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < packet.items().size() && i < packet.counts().size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(packet.items().get(i));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(id), packet.counts().get(i)));
            }
        }
        shown = new Shown(packet, stacks, Util.getMillis());
        if (!packet.complete()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F, 1F));
        }
    }

    private static long life(Shown current) {
        return HOLD_MS + current.stacks().size() * STAGGER_MS;
    }

    static long goneIn() {
        Shown current = shown;
        return current == null ? 0L : Math.max(0L, life(current) + OUT_MS - (Util.getMillis() - current.at()));
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Shown current = shown;
        if (current == null || mc.options.hideGui) {
            return;
        }
        long age = Util.getMillis() - current.at();
        long life = life(current);
        if (age > life + OUT_MS) {
            shown = null;
            return;
        }
        float fadeIn = Mth.clamp(age / (float) IN_MS, 0F, 1F);
        float fadeOut = 1F - Mth.clamp((age - life) / (float) OUT_MS, 0F, 1F);
        float alpha = Math.min(fadeIn, fadeOut);
        if (alpha <= 0.02F) {
            return;
        }
        QuestBannerPacket packet = current.packet();
        Font font = mc.font;
        Component head = Component.translatable(packet.complete() ? "quest.jojoha.banner.complete" : "quest.jojoha.banner.new");
        Component name = Component.translatable("quest.jojoha." + packet.quest() + ".name");
        List<Component> gains = new ArrayList<>();
        if (packet.complete() && packet.trust() > 0) {
            gains.add(Component.translatable("quest.jojoha.banner.trust", packet.trust()));
        }
        List<ItemStack> stacks = packet.complete() ? current.stacks() : List.of();

        int titleWidth = ICON + 6 + font.width(head) + DIVIDER_GAP * 2 + QuestIcons.DIVIDER_SIZE + font.width(name);
        int rewardWidth = 0;
        for (int i = 0; i < stacks.size(); i++) {
            rewardWidth += ICON + ICON_GAP;
        }
        for (Component gain : gains) {
            rewardWidth += font.width(gain) + ICON_GAP * 2;
        }
        int width = Math.max(MIN_WIDTH, PAD * 2 + titleWidth + (rewardWidth > 0 ? SECTION_GAP + rewardWidth : 0));
        int left = (graphics.guiWidth() - width) / 2;
        int top = Math.round(graphics.guiHeight() * 0.12F) - Math.round((1F - fadeIn) * 6F);
        int textAlpha = Math.max(4, Math.round(alpha * 255F)) << 24;
        int textY = top + (HEIGHT - 8) / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(0F, 0F, 300F);
        graphics.setColor(1F, 1F, 1F, alpha);
        DialogueFrame.draw(graphics, left, top, width, HEIGHT);
        graphics.setColor(1F, 1F, 1F, 1F);

        int contentWidth = titleWidth + (rewardWidth > 0 ? SECTION_GAP + rewardWidth : 0);
        int x = left + (width - contentWidth) / 2;
        int headColour = (packet.complete() ? HEAD_COLOUR : NEW_COLOUR) & 0xFFFFFF | textAlpha;
        graphics.setColor(1F, 1F, 1F, alpha);
        QuestIcons.icon(graphics, QuestIcons.objective(packet.quest()), x, top + (HEIGHT - ICON) / 2);
        graphics.setColor(1F, 1F, 1F, 1F);
        x += ICON + 6;
        graphics.drawString(font, head, x, textY, headColour, true);
        x += font.width(head) + DIVIDER_GAP;
        graphics.setColor(1F, 1F, 1F, alpha);
        QuestIcons.divider(graphics, x, top + (HEIGHT - QuestIcons.DIVIDER_SIZE) / 2);
        graphics.setColor(1F, 1F, 1F, 1F);
        x += QuestIcons.DIVIDER_SIZE + DIVIDER_GAP;
        graphics.drawString(font, name, x, textY, NAME_COLOUR & 0xFFFFFF | textAlpha, true);
        x += font.width(name) + SECTION_GAP;
        if (!stacks.isEmpty() || !gains.isEmpty()) {
            graphics.setColor(1F, 1F, 1F, alpha);
            QuestIcons.divider(graphics, x - SECTION_GAP / 2 - QuestIcons.DIVIDER_SIZE / 2, top + (HEIGHT - QuestIcons.DIVIDER_SIZE) / 2);
            graphics.setColor(1F, 1F, 1F, 1F);
        }

        int iconY = top + (HEIGHT - ICON) / 2;
        for (int i = 0; i < stacks.size(); i++) {
            long since = age - IN_MS - i * STAGGER_MS;
            if (since > 0 && fadeOut > 0.3F) {
                float t = Mth.clamp(since / (float) POP_MS, 0F, 1F);
                float pop = (t < 1F ? 1F + Mth.sin(t * Mth.PI) * 0.35F : 1F) * t;
                ItemStack stack = stacks.get(i);
                graphics.pose().pushPose();
                graphics.pose().translate(x + ICON / 2F, iconY + ICON / 2F, 0F);
                graphics.pose().scale(pop, pop, 1F);
                graphics.pose().translate(-ICON / 2F, -ICON / 2F, 0F);
                graphics.renderItem(stack, 0, 0);
                graphics.renderItemDecorations(font, stack, 0, 0);
                graphics.pose().popPose();
            }
            x += ICON + ICON_GAP;
        }
        long gainsSince = age - IN_MS - stacks.size() * STAGGER_MS;
        if (gainsSince > 0) {
            for (Component gain : gains) {
                x += ICON_GAP;
                graphics.drawString(font, gain, x, textY, GAIN_COLOUR & 0xFFFFFF | textAlpha, true);
                x += font.width(gain) + ICON_GAP;
            }
        }
        graphics.pose().popPose();
    }
}
