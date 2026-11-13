package org.gumel.jojoha.client;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.gumel.jojoha.supporter.SupporterGate;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public final class SupporterLockScreen extends Screen {
    private static final int TEXT_WIDTH = 300;
    private static final int BUTTON_WIDTH = 200;
    private static final int LINE = 10;
    private static SupporterGate.Verdict verdict;
    private static boolean checking;

    private Button retry;

    private SupporterLockScreen() {
        super(Component.translatable("screen.jojoha.supporter_build.title"));
    }

    public static void register() {
        if (!SupporterGate.ACTIVE) {
            return;
        }
        ClientGuiEvent.SET_SCREEN.register(screen -> {
            if (verdict == SupporterGate.Verdict.ALLOWED || screen == null || screen instanceof SupporterLockScreen) {
                return CompoundEventResult.pass();
            }
            return CompoundEventResult.interruptTrue(new SupporterLockScreen());
        });
    }

    private static void check() {
        if (checking) {
            return;
        }
        checking = true;
        Minecraft minecraft = Minecraft.getInstance();
        UUID uuid = minecraft.getUser().getProfileId();
        SupporterGate.check(uuid).thenAccept(result -> minecraft.execute(() -> {
            checking = false;
            verdict = result;
            if (minecraft.screen instanceof SupporterLockScreen lock) {
                lock.refresh();
            }
        }));
    }

    @Override
    protected void init() {
        int x = width / 2 - BUTTON_WIDTH / 2;
        int y = height / 2 + 30;
        retry = addRenderableWidget(Button.builder(Component.translatable("screen.jojoha.supporter_build.retry"),
                button -> {
                    check();
                    refresh();
                }).bounds(x, y, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.jojoha.supporter_build.discord"),
                button -> Util.getPlatform().openUri(URI.create(SupporterGate.DISCORD)))
                .bounds(x, y + 24, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.quit"),
                button -> minecraft.stop()).bounds(x, y + 48, BUTTON_WIDTH, 20).build());
        if (verdict == null) {
            check();
        }
        refresh();
    }

    private void refresh() {
        if (verdict == SupporterGate.Verdict.ALLOWED) {
            minecraft.setScreen(new TitleScreen());
            return;
        }
        if (retry != null) {
            retry.active = !checking;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int y = height / 2 - 60;
        graphics.drawCenteredString(font, title, width / 2, y, 0xFFFFFF);
        y += LINE * 2;
        for (FormattedCharSequence line : lines()) {
            graphics.drawCenteredString(font, line, width / 2, y, 0xFFFFFF);
            y += LINE;
        }
    }

    private List<FormattedCharSequence> lines() {
        String name = minecraft.getUser().getName();
        Component message = checking || verdict == null
                ? Component.translatable("screen.jojoha.supporter_build.checking").withStyle(ChatFormatting.GRAY)
                : verdict == SupporterGate.Verdict.DENIED
                ? Component.translatable("screen.jojoha.supporter_build.denied", name)
                : Component.translatable("screen.jojoha.supporter_build.offline");
        return font.split(message, TEXT_WIDTH);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
