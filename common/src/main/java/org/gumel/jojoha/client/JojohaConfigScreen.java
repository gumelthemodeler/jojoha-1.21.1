package org.gumel.jojoha.client;

import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.gumel.jojoha.config.ChoiceSetting;
import org.gumel.jojoha.config.ConfigFile;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.config.NumberSetting;
import org.gumel.jojoha.config.Setting;
import org.gumel.jojoha.config.ToggleSetting;

import java.util.List;
import java.util.Locale;

public final class JojohaConfigScreen extends Screen {
    private static final int ROW_W = 300;
    private static final int ROW_H = 20;
    private static final int ROW_PITCH = 24;
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 36;
    private static final int BUTTON_W = 148;
    private static final int BUTTON_GAP = 4;
    private static final int HEADING_COLOUR = 0xFFE0B040;
    private static final int NOTE_COLOUR = 0xFFA0A0A0;

    private final Screen parent;
    private SettingsList list;
    private boolean clientChanged;
    private boolean serverChanged;

    public static void registerCommand() {
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) ->
                dispatcher.register(ClientCommandRegistrationEvent
                        .literal("jojohaconfig")
                        .executes(ctx -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            minecraft.tell(() -> minecraft.setScreen(new JojohaConfigScreen(null)));
                            return 1;
                        })));
    }

    public JojohaConfigScreen(Screen parent) {
        super(Component.translatable("config.jojoha.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        list = new SettingsList(this.minecraft, this.width, this.height - HEADER_H - FOOTER_H, HEADER_H);

        fill(JojohaClientConfig.FILE, true);

        boolean hosting = serverEditable();
        list.add(new HeadingRow(Component.translatable("config.jojoha.section.world")
                .withStyle(ChatFormatting.BOLD), HEADING_COLOUR));
        list.add(new NoteRow(Component.translatable(hosting
                ? "config.jojoha.section.world.note"
                : "config.jojoha.section.world.remote")));
        fill(JojohaServerConfig.FILE, hosting);

        this.addRenderableWidget(list);

        int left = this.width / 2 - BUTTON_W - BUTTON_GAP / 2;
        int y = this.height - FOOTER_H + (FOOTER_H - ROW_H) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("config.jojoha.reset"), button -> confirmReset())
                .bounds(left, y, BUTTON_W, ROW_H).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + BUTTON_W + BUTTON_GAP, y, BUTTON_W, ROW_H).build());
    }

    private void fill(ConfigFile file, boolean editable) {
        String group = null;
        for (Setting<?> setting : file.settings()) {
            if (!setting.group().equals(group)) {
                group = setting.group();
                list.add(new HeadingRow(Component.translatable("config.jojoha.group." + group), 0xFFFFFFFF));
            }
            list.add(new WidgetRow(widgetFor(setting), setting, editable));
        }
    }

    private AbstractWidget widgetFor(Setting<?> setting) {
        if (setting instanceof ToggleSetting toggle) {
            return CycleButton.onOffBuilder(toggle.on())
                    .withTooltip(value -> Tooltip.create(tooltip(setting)))
                    .create(0, 0, ROW_W, ROW_H, label(setting), (button, value) -> {
                        toggle.set(value);
                        changed(setting);
                    });
        }
        if (setting instanceof NumberSetting number) {
            SettingSlider slider = new SettingSlider(number, label(setting));
            slider.setTooltip(Tooltip.create(tooltip(setting)));
            return slider;
        }
        if (setting instanceof ChoiceSetting<?> choice) {
            return choiceButton(choice);
        }
        throw new IllegalStateException("No widget for " + setting.key());
    }

    private <E extends Enum<E>> AbstractWidget choiceButton(ChoiceSetting<E> choice) {
        return CycleButton.<E>builder(value -> Component.translatable(
                        "config.jojoha." + choice.langKey() + "." + ChoiceSetting.name(value)))
                .withValues(choice.options())
                .withInitialValue(choice.get())
                .withTooltip(value -> Tooltip.create(tooltip(choice)))
                .create(0, 0, ROW_W, ROW_H, label(choice), (button, value) -> {
                    choice.set(value);
                    changed(choice);
                });
    }

    private static Component label(Setting<?> setting) {
        String key = "config.jojoha." + setting.langKey();
        return setting.labelArgument() == null
                ? Component.translatable(key)
                : Component.translatable(key, Component.translatable(setting.labelArgument()));
    }

    private static Component tooltip(Setting<?> setting) {
        String key = "config.jojoha." + setting.langKey() + ".tooltip";
        return setting.labelArgument() == null
                ? Component.translatable(key)
                : Component.translatable(key, Component.translatable(setting.labelArgument()));
    }

    static Component valueText(NumberSetting setting, float value) {
        return switch (setting.unit()) {
            case TIMES -> Component.literal("×" + trimmed(value, 2));
            case PERCENT -> Component.literal(trimmed(value, setting.step() < 1F ? 1 : 0) + "%");
            case ONE_IN -> Math.round(value) <= 0
                    ? Component.translatable("config.jojoha.value.never")
                    : Component.translatable("config.jojoha.value.one_in", Math.round(value));
            case WHOLE -> Component.literal(Integer.toString(Math.round(value)));
            case HEALTH -> value <= 0F
                    ? Component.translatable("config.jojoha.value.no_cap")
                    : Component.translatable("config.jojoha.value.health", trimmed(value, 1), trimmed(value / 2F, 2));
            case HARDNESS -> Component.literal(trimmed(value, 1));
            case PIXELS -> Component.translatable("config.jojoha.value.pixels",
                    (Math.round(value) > 0 ? "+" : "") + Math.round(value));
            case BLOCKS -> Component.translatable("config.jojoha.value.blocks", trimmed(value, 1));
        };
    }

    private static String trimmed(float value, int decimals) {
        String text = String.format(Locale.ROOT, "%." + decimals + "f", value);
        if (decimals > 0 && text.contains(".")) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text;
    }

    private void changed(Setting<?> setting) {
        if (setting.file() == JojohaServerConfig.FILE) {
            serverChanged = true;
        } else {
            clientChanged = true;
        }
    }

    private boolean serverEditable() {
        return this.minecraft == null || this.minecraft.level == null || this.minecraft.hasSingleplayerServer();
    }

    private void confirmReset() {
        boolean hosting = serverEditable();
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                JojohaClientConfig.FILE.resetAll();
                JojohaClientConfig.FILE.save();
                clientChanged = false;
                if (hosting) {
                    JojohaServerConfig.FILE.resetAll();
                    JojohaServerConfig.FILE.save();
                    serverChanged = false;
                }
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("config.jojoha.reset.title"),
                Component.translatable(hosting ? "config.jojoha.reset.message" : "config.jojoha.reset.message.remote")));
    }

    private void saveChanges() {
        if (clientChanged) {
            JojohaClientConfig.FILE.save();
            clientChanged = false;
        }
        if (serverChanged && serverEditable()) {
            JojohaServerConfig.FILE.save();
            serverChanged = false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, (HEADER_H - this.font.lineHeight) / 2, 0xFFFFFF);
    }

    @Override
    public void removed() {
        saveChanges();
    }

    @Override
    public void onClose() {
        saveChanges();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private final class SettingSlider extends AbstractSliderButton {
        private final NumberSetting setting;
        private final Component label;

        private SettingSlider(NumberSetting setting, Component label) {
            super(0, 0, ROW_W, ROW_H, Component.empty(),
                    (setting.value() - setting.min()) / (double) (setting.max() - setting.min()));
            this.setting = setting;
            this.label = label;
            updateMessage();
        }

        private float picked() {
            return setting.snap((float) (setting.min() + this.value * (setting.max() - setting.min())));
        }

        @Override
        protected void updateMessage() {
            if (setting == null) {
                return;
            }
            setMessage(Component.translatable("options.generic_value", label, valueText(setting, picked())));
        }

        @Override
        protected void applyValue() {
            setting.set(picked());
            changed(setting);
        }
    }

    private static final class SettingsList extends ContainerObjectSelectionList<Row> {
        private SettingsList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_PITCH);
        }

        private void add(Row row) {
            this.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return ROW_W;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + ROW_W / 2 + 8;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    private final class HeadingRow extends Row {
        private final Component text;
        private final int colour;

        private HeadingRow(Component text, int colour) {
            this.text = text;
            this.colour = colour;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            graphics.drawCenteredString(font, text, left + width / 2, top + (height - font.lineHeight) / 2, colour);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private final class NoteRow extends Row {
        private final List<FormattedCharSequence> lines;

        private NoteRow(Component text) {
            this.lines = font.split(text, ROW_W);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            int y = top + (height - lines.size() * font.lineHeight) / 2;
            for (FormattedCharSequence line : lines) {
                graphics.drawCenteredString(font, line, left + width / 2, y, NOTE_COLOUR);
                y += font.lineHeight;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private static final class WidgetRow extends Row {
        private final AbstractWidget widget;
        private final Setting<?> setting;
        private final boolean editable;

        private WidgetRow(AbstractWidget widget, Setting<?> setting, boolean editable) {
            this.widget = widget;
            this.setting = setting;
            this.editable = editable;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            widget.active = editable && setting.available();
            widget.setX(left + (width - ROW_W) / 2);
            widget.setY(top + (height - ROW_H) / 2);
            widget.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(widget);
        }
    }
}
