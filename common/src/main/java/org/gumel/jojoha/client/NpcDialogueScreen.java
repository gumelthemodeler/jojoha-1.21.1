package org.gumel.jojoha.client;

import org.joml.Vector3f;
import org.joml.Quaternionf;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.networking.NetworkManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.entity.npc.JojoNpc;
import org.gumel.jojoha.network.packet.NpcReplyPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class NpcDialogueScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int GUTTER = 16;
    private static final int PAD = 7;
    private static final int LINE = 10;
    private static final int HEADER = 14;
    private static final int PORTRAIT = 58;
    private static final int PORTRAIT_SCALE = 46;
    private static final float PORTRAIT_LIFT = 0.72F;
    private static final float PORTRAIT_TILT = 4F;
    private static final int PORTRAIT_FILL = 0xFF000000;
    private static final int PORTRAIT_EDGE = 0x50FFFFFF;
    private static final int OPTION_ROW = 12;
    private static final int OPTION_SPLIT = 7;
    private static final int OPTION_INDENT = 4;
    private static final int OPTION_HOVER_FILL = 0x30FFDD55;
    private static final int SPLIT_COLOUR = 0x40FFFFFF;
    private static final float CHARS_PER_TICK = 1.6F;
    private static final int STOP_PAUSE = 6;
    private static final int COMMA_PAUSE = 3;
    private static final int BLIP_EVERY = 2;
    private static final int REPLY_WAIT = 12;
    private static final int TEXT_COLOUR = 0xFFFFFFFF;
    private static final int OPTION_COLOUR = 0xFFD8D8D8;
    private static final int HOVER_COLOUR = 0xFFFFDD55;
    private static final int NUMBER_COLOUR = 0xFF8A8A8A;
    private static final int HINT_COLOUR = 0xFF8A8A8A;
    private static final int MORE_COLOUR = 0xFFB8B8B8;
    private static final long SHAKE_MS = 220L;
    private static final float SHAKE_PX = 2.5F;
    private static final long DELTA_MS = 1400L;
    private static final int TRUST_GOOD = 0xFFE0B040;
    private static final int TRUST_BAD = 0xFFD04848;
    private static final int TIER_COLOUR = 0xFFE0C8E0;
    private static final int OFF_COLOUR = 0xFF6A5A6A;
    private static final int ICON = 16;
    private static final int ICON_SCALE_GAP = 2;
    private static final int TRAIT_COLOUR = 0xFFA8C8E8;
    private static final int SIGNATURE_COLOUR = 0xFFE8D8A0;

    private enum Page {
        TALK, MORE, ABOUT
    }

    private record Option(Component label, int colour, boolean enabled, boolean gift, Runnable run) {
    }

    private final int entityId;
    private final float basePitch;
    private final boolean feminine;
    @Nullable
    private final SoundEvent character;
    private final RandomSource random = RandomSource.create();
    private String text;
    private List<String> replies;
    private List<String> questions = List.of();
    private Component speaker = Component.empty();
    private final List<Glyphs> lines = new ArrayList<>();
    private Page page = Page.TALK;
    private int total;
    private float shown;
    private int pause;
    private int lastBlip;
    private int ticks;
    private int waiting = -1;
    private boolean farewell;
    private int trust;
    private boolean canGift;
    @Nullable
    private JojoNpc.Trait trait;
    private int shaking = -1;
    private long shakeAt;
    private int trustDelta;
    private long deltaAt;
    private int panelLeft;
    private int panelWidth;
    private int boxTop;
    private int boxHeight;
    private int textLeft;
    private int textWidth;

    private record Glyphs(int[] codepoints, Style[] styles) {
        FormattedCharSequence prefix(int count) {
            int end = Math.min(count, codepoints.length);
            return sink -> {
                for (int i = 0; i < end; i++) {
                    if (!sink.accept(i, styles[i], codepoints[i])) {
                        return false;
                    }
                }
                return true;
            };
        }
    }

    private NpcDialogueScreen(int entityId, String text, List<String> replies, int trust, boolean canGift, String trait) {
        super(Component.translatable(text));
        this.trait = JojoNpc.Trait.byId(trait);
        this.entityId = entityId;
        this.text = text;
        split(replies);
        this.trust = trust;
        this.canGift = canGift;
        Entity entity = entity();
        if (entity != null) {
            this.speaker = entity.getDisplayName();
            Boolean voiced = entity instanceof JojoNpc npc ? npc.feminineVoice() : null;
            this.feminine = voiced != null ? voiced : DefaultPlayerSkin.get(entity.getUUID()).model() == PlayerSkin.Model.SLIM;
            this.character = entity instanceof JojoNpc npc ? npc.voiceBlip() : null;
            RandomSource voice = RandomSource.create(entity.getUUID().getLeastSignificantBits());
            this.basePitch = character != null ? 1F : 0.85F + voice.nextFloat() * 0.4F;
        } else {
            this.feminine = false;
            this.character = null;
            this.basePitch = 1F;
        }
    }

    public static void open(int entityId, String text, List<String> replies, int trust, boolean canGift, String trait) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof NpcDialogueScreen screen && screen.entityId == entityId) {
            screen.trait = JojoNpc.Trait.byId(trait);
            screen.show(text, replies, trust, canGift);
            return;
        }
        mc.setScreen(new NpcDialogueScreen(entityId, text, replies, trust, canGift, trait));
    }

    private void show(String text, List<String> replies, int trust, boolean canGift) {
        this.text = text;
        split(replies);
        if (trust != this.trust) {
            this.trustDelta = trust - this.trust;
            this.deltaAt = Util.getMillis();
        }
        this.trust = trust;
        this.canGift = canGift;
        this.page = Page.TALK;
        this.shown = 0;
        this.pause = 0;
        this.lastBlip = 0;
        this.waiting = -1;
        layout();
    }

    private void split(List<String> all) {
        int marker = all.indexOf(JojoNpc.MORE);
        replies = marker < 0 ? all : all.subList(0, marker);
        questions = marker < 0 ? List.of() : all.subList(marker + 1, all.size());
    }

    @Nullable
    private Entity entity() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : mc.level.getEntity(entityId);
    }

    @Override
    protected void init() {
        layout();
    }

    private List<Component> body() {
        if (page != Page.ABOUT || trait == null) {
            return List.of(Component.translatable(text));
        }
        return aboutPage();
    }

    private List<Component> aboutPage() {
        List<Component> about = new ArrayList<>();
        about.add(Component.translatable(trait.key()).withColor(TRAIT_COLOUR)
                .append(Component.literal(" - ").withColor(HINT_COLOUR))
                .append(Component.translatable(trait.key() + ".about").withColor(TEXT_COLOUR)));
        if (JojoNpc.Tier.of(trust).ordinal() >= JojoNpc.Tier.FRIEND.ordinal()) {
            about.add(Component.translatable("npc.jojoha.trait.likes", Component.translatable(trait.likes().topic())).withColor(TRUST_GOOD));
            about.add(Component.translatable("npc.jojoha.trait.dislikes", Component.translatable(trait.dislikes().topic())).withColor(TRUST_BAD));
        } else {
            about.add(Component.translatable("npc.jojoha.trait.hint").withColor(HINT_COLOUR));
        }
        return about;
    }

    private void layout() {
        panelWidth = Math.min(PANEL_WIDTH, width - GUTTER * 2);
        panelLeft = (width - panelWidth) / 2;
        textLeft = panelLeft + PAD + PORTRAIT + PAD + 1;
        textWidth = panelLeft + panelWidth - PAD - textLeft;
        lines.clear();
        total = 0;
        for (Component part : body()) {
            for (FormattedCharSequence line : font.split(part, textWidth)) {
                List<Integer> codepoints = new ArrayList<>();
                List<Style> styles = new ArrayList<>();
                line.accept((index, style, codepoint) -> {
                    codepoints.add(codepoint);
                    styles.add(style);
                    return true;
                });
                lines.add(new Glyphs(codepoints.stream().mapToInt(Integer::intValue).toArray(), styles.toArray(Style[]::new)));
                total += codepoints.size();
            }
        }
        if (page != Page.TALK) {
            shown = total;
        }
        boxHeight = Math.max(PORTRAIT + PAD * 2,
                PAD * 2 + HEADER + lines.size() * LINE + OPTION_SPLIT + options().size() * OPTION_ROW);
        boxTop = height - GUTTER - boxHeight;
    }

    private void turn(Page next) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
        page = next;
        shaking = -1;
        layout();
    }

    private List<Option> options() {
        List<Option> options = new ArrayList<>();
        switch (page) {
            case TALK -> {
                if (replies.isEmpty()) {
                    options.add(new Option(Component.translatable("npc.jojoha.leave"), OPTION_COLOUR, true, false, this::onClose));
                    if (questions.isEmpty()) {
                        break;
                    }
                }
                for (int i = 0; i < replies.size(); i++) {
                    int index = i;
                    options.add(new Option(Component.translatable(replies.get(i)),
                            signature(i) ? SIGNATURE_COLOUR : OPTION_COLOUR, true, false, () -> reply(index)));
                }
                if (replies.contains(JojoNpc.TALK_BACK)) {
                    options.add(new Option(Component.translatable("npc.jojoha.more"), MORE_COLOUR, true, false,
                            () -> turn(Page.MORE)));
                }
            }
            case MORE -> {
                for (String question : questions) {
                    options.add(new Option(Component.translatable(question), OPTION_COLOUR, true, false, () -> ask(question)));
                }
                options.add(giftOption());
                boolean known = trait != null && JojoNpc.Tier.of(trust).ordinal() >= JojoNpc.Tier.ACQUAINTED.ordinal();
                options.add(new Option(Component.translatable(known ? "npc.jojoha.trait.option" : "npc.jojoha.trait.locked"),
                        TRAIT_COLOUR, known, false, () -> turn(Page.ABOUT)));
                options.add(new Option(Component.translatable("npc.jojoha.back"), MORE_COLOUR, true, false,
                        () -> turn(Page.TALK)));
            }
            case ABOUT -> options.add(new Option(Component.translatable("npc.jojoha.back"), MORE_COLOUR, true, false,
                    () -> turn(Page.MORE)));
        }
        return options;
    }

    private Option giftOption() {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (!canGift) {
            return new Option(Component.translatable("npc.jojoha.gift.tip_later"), OPTION_COLOUR, false, true, () -> {
            });
        }
        if (held.isEmpty()) {
            return new Option(Component.translatable("npc.jojoha.gift.hold"), OPTION_COLOUR, false, true, () -> {
            });
        }
        return new Option(Component.translatable("npc.jojoha.gift.tip", held.getHoverName()), OPTION_COLOUR, true, true,
                this::gift);
    }

    private boolean typing() {
        return shown < total;
    }

    private int textTop() {
        return boxTop + PAD + HEADER;
    }

    private int optionsTop() {
        return textTop() + lines.size() * LINE + OPTION_SPLIT;
    }

    private int optionTop(int index) {
        return optionsTop() + index * OPTION_ROW;
    }

    private int hovered(double mouseX, double mouseY) {
        if (typing() || waiting >= 0) {
            return -1;
        }
        int count = options().size();
        for (int i = 0; i < count; i++) {
            int top = optionTop(i);
            if (mouseX >= textLeft - 3 && mouseX < textLeft + textWidth && mouseY >= top - 2 && mouseY < top + OPTION_ROW - 2) {
                return i;
            }
        }
        return -1;
    }

    private void gift() {
        NetworkManager.sendToServer(new NpcReplyPacket(entityId, text, JojoNpc.GIFT));
        skip();
        waiting = REPLY_WAIT;
    }

    private void shake(int slot) {
        shaking = slot;
        shakeAt = Util.getMillis();
    }

    private int shakeOffset(int slot) {
        if (shaking != slot) {
            return 0;
        }
        float t = (Util.getMillis() - shakeAt) / (float) SHAKE_MS;
        if (t >= 1F) {
            return 0;
        }
        return Math.round(Mth.sin(t * Mth.PI * 6F) * SHAKE_PX * (1F - t));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && scrollY != 0) {
            mc.player.getInventory().swapPaint(scrollY);
            if (page == Page.MORE) {
                layout();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void reply(int index) {
        if (index < 0 || index >= replies.size()) {
            return;
        }
        NetworkManager.sendToServer(new NpcReplyPacket(entityId, text, replies.get(index)));
        waiting = REPLY_WAIT;
    }

    private void ask(String question) {
        NetworkManager.sendToServer(new NpcReplyPacket(entityId, text, question));
        waiting = REPLY_WAIT;
    }

    private void choose(int index) {
        List<Option> options = options();
        if (index < 0 || index >= options.size() || !options.get(index).enabled()) {
            return;
        }
        Option option = options.get(index);
        if (page == Page.TALK || option.gift() || page == Page.MORE && index < questions.size()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            shake(index);
        }
        option.run().run();
    }

    @Override
    public void tick() {
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        Entity entity = entity();
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || mc.player == null
                || entity.distanceToSqr(mc.player) > JojoNpc.TALK_RANGE * JojoNpc.TALK_RANGE) {
            onClose();
            return;
        }
        speaker = entity.getDisplayName();
        if (waiting >= 0 && waiting-- == 0) {
            onClose();
            return;
        }
        if (!typing()) {
            return;
        }
        if (pause > 0) {
            pause--;
            return;
        }
        int before = (int) shown;
        shown = Math.min(total, shown + CHARS_PER_TICK);
        int after = (int) shown;
        for (int i = before; i < after; i++) {
            int codepoint = glyph(i);
            if (codepoint == '.' || codepoint == '!' || codepoint == '?') {
                shown = i + 1;
                pause = STOP_PAUSE;
                break;
            }
            if (codepoint == ',' || codepoint == ';' || codepoint == ':') {
                shown = i + 1;
                pause = COMMA_PAUSE;
                break;
            }
        }
        if ((int) shown - lastBlip >= BLIP_EVERY && !Character.isWhitespace(glyph((int) shown - 1))) {
            lastBlip = (int) shown;
            blip();
        }
    }

    private int glyph(int index) {
        for (Glyphs line : lines) {
            if (index < line.codepoints().length) {
                return line.codepoints()[index];
            }
            index -= line.codepoints().length;
        }
        return ' ';
    }

    private void blip() {
        SoundEvent voice = character != null ? character
                : (feminine ? ModSounds.NPC_VOICE_FEMININE : ModSounds.NPC_VOICE_MASCULINE).get();
        float pitch = Mth.clamp(basePitch + (random.nextFloat() - 0.5F) * 0.16F, 0.5F, 2F);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(voice, pitch, 0.55F));
    }

    private void skip() {
        shown = total;
        pause = 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (typing()) {
            skip();
            return true;
        }
        int index = hovered(mouseX, mouseY);
        if (index >= 0) {
            choose(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing() && (keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_RETURN)) {
            skip();
            return true;
        }
        if (keyCode == InputConstants.KEY_ESCAPE && page != Page.TALK && waiting < 0) {
            turn(page == Page.ABOUT ? Page.MORE : Page.TALK);
            return true;
        }
        if (!typing() && waiting < 0 && keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9) {
            int index = keyCode - InputConstants.KEY_1;
            if (index < options().size()) {
                choose(index);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DialogueFrame.draw(graphics, panelLeft, boxTop, panelWidth, boxHeight);
        int portraitLeft = panelLeft + PAD;
        int portraitTop = boxTop + (boxHeight - PORTRAIT) / 2;
        graphics.fill(portraitLeft - 1, portraitTop - 1, portraitLeft + PORTRAIT + 1, portraitTop + PORTRAIT + 1, PORTRAIT_EDGE);
        graphics.fill(portraitLeft, portraitTop, portraitLeft + PORTRAIT, portraitTop + PORTRAIT, PORTRAIT_FILL);
        if (entity() instanceof LivingEntity npc) {
            portrait(graphics, npc, portraitLeft, portraitTop, mouseX, mouseY, partialTick);
        }

        renderHeader(graphics);

        int remaining = (int) shown;
        int y = textTop();
        for (Glyphs line : lines) {
            if (remaining <= 0) {
                break;
            }
            graphics.drawString(font, line.prefix(remaining), textLeft, y, TEXT_COLOUR, false);
            remaining -= line.codepoints().length;
            y += LINE;
        }
        if (typing()) {
            if ((ticks / 8) % 2 == 0) {
                Component hint = Component.translatable("npc.jojoha.skip");
                graphics.drawString(font, hint, textLeft + textWidth - font.width(hint),
                        boxTop + boxHeight - PAD - 8, HINT_COLOUR, false);
            }
        } else {
            renderOptions(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void portrait(GuiGraphics graphics, LivingEntity npc, int left, int top, int mouseX, int mouseY, float partialTick) {
        float t = ticks + partialTick;
        float centreX = left + PORTRAIT / 2F;
        float centreY = top + PORTRAIT / 2F;
        boolean near = Math.abs(mouseX - centreX) < PORTRAIT * 1.5F && Math.abs(mouseY - centreY) < PORTRAIT * 1.5F;
        float lookX = near ? Mth.clamp((centreX - mouseX) / PORTRAIT, -1F, 1F) : 0F;
        float lookY = near ? Mth.clamp((centreY - mouseY) / PORTRAIT, -1F, 1F) : 0F;
        float bodyTurn = Mth.sin(t * 0.021F) * 8F + lookX * 10F;
        float headTurn = bodyTurn + Mth.sin(t * 0.047F) * 22F + Mth.sin(t * 0.013F) * 10F + lookX * 25F;
        float headTilt = Mth.sin(t * 0.033F) * 6F - lookY * 15F;
        float bodyRot = npc.yBodyRot;
        float bodyRotO = npc.yBodyRotO;
        float yRot = npc.getYRot();
        float yRotO = npc.yRotO;
        float xRot = npc.getXRot();
        float xRotO = npc.xRotO;
        float headRot = npc.yHeadRot;
        float headRotO = npc.yHeadRotO;
        npc.yBodyRot = 180F + bodyTurn;
        npc.yBodyRotO = npc.yBodyRot;
        npc.setYRot(180F + headTurn);
        npc.yRotO = npc.getYRot();
        npc.setXRot(headTilt);
        npc.xRotO = headTilt;
        npc.yHeadRot = npc.getYRot();
        npc.yHeadRotO = npc.yHeadRot;
        graphics.enableScissor(left, top, left + PORTRAIT, top + PORTRAIT);
        Quaternionf pose = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf camera = new Quaternionf().rotateX(PORTRAIT_TILT * Mth.DEG_TO_RAD);
        pose.mul(camera);
        float scale = npc.getScale();
        Vector3f lift = new Vector3f(0F, npc.getBbHeight() / 2F + PORTRAIT_LIFT * scale, 0F);
        InventoryScreen.renderEntityInInventory(graphics, centreX, centreY, PORTRAIT_SCALE / scale, lift, pose, camera, npc);
        graphics.disableScissor();
        npc.yBodyRot = bodyRot;
        npc.yBodyRotO = bodyRotO;
        npc.setYRot(yRot);
        npc.yRotO = yRotO;
        npc.setXRot(xRot);
        npc.xRotO = xRotO;
        npc.yHeadRot = headRot;
        npc.yHeadRotO = headRotO;
    }

    private void renderHeader(GuiGraphics graphics) {
        int top = boxTop + PAD;
        graphics.drawString(font, speaker, textLeft, top, TEXT_COLOUR, false);
        Component label = Component.translatable(JojoNpc.Tier.of(trust).key());
        String value = (trust > 0 ? "+" : "") + trust;
        int right = textLeft + textWidth;
        int valueLeft = right - font.width(value);
        graphics.drawString(font, value, valueLeft, top, trust < 0 ? TRUST_BAD : TRUST_GOOD, false);
        graphics.drawString(font, label, valueLeft - 4 - font.width(label), top, TIER_COLOUR, false);
        long age = Util.getMillis() - deltaAt;
        if (trustDelta != 0 && age < DELTA_MS) {
            float t = age / (float) DELTA_MS;
            int alpha = Math.max(4, Math.round((1F - t) * 255F)) << 24;
            String delta = (trustDelta > 0 ? "+" : "") + trustDelta;
            int colour = (trustDelta > 0 ? TRUST_GOOD : TRUST_BAD) & 0xFFFFFF | alpha;
            graphics.drawString(font, delta, right - font.width(delta), top - 8 - Math.round(t * 6F), colour, true);
        }
        int splitY = top + HEADER - 4;
        graphics.fill(textLeft, splitY, textLeft + textWidth, splitY + 1, SPLIT_COLOUR);
    }

    private boolean signature(int index) {
        return trait != null && index < replies.size() && trait.signature().reply().equals(replies.get(index));
    }

    private void renderOptions(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Option> options = options();
        int hover = hovered(mouseX, mouseY);
        int splitY = optionsTop() - OPTION_SPLIT / 2 - 1;
        graphics.fill(textLeft, splitY, textLeft + textWidth, splitY + 1, SPLIT_COLOUR);
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int top = optionTop(i);
            int x = textLeft + shakeOffset(i);
            boolean picked = waiting >= 0 && shaking == i;
            boolean lit = picked || i == hover && option.enabled();
            if (lit) {
                graphics.fill(textLeft - 3, top - 2, textLeft + textWidth, top + OPTION_ROW - 2, OPTION_HOVER_FILL);
            }
            int colour = lit ? HOVER_COLOUR : waiting >= 0 || !option.enabled() ? OFF_COLOUR : option.colour();
            String number = (i + 1) + ".";
            graphics.drawString(font, number, x, top, lit ? HOVER_COLOUR : NUMBER_COLOUR, false);
            int indent = font.width("9.") + OPTION_INDENT + (lit ? 2 : 0);
            if (option.gift() && option.enabled() && mc.player != null) {
                graphics.pose().pushPose();
                graphics.pose().translate(x + indent, top - ICON_SCALE_GAP, 0);
                graphics.pose().scale(0.75F, 0.75F, 1F);
                graphics.renderItem(mc.player.getMainHandItem(), 0, 0);
                graphics.pose().popPose();
                indent += ICON * 3 / 4 + 2;
            }
            String label = font.plainSubstrByWidth(option.label().getString(), textWidth - indent);
            graphics.drawString(font, label, x + indent, top, colour, false);
        }
    }

    @Override
    public void onClose() {
        if (!farewell) {
            farewell = true;
            if (entity() instanceof LivingEntity) {
                NetworkManager.sendToServer(new NpcReplyPacket(entityId, text, JojoNpc.CLOSE));
            }
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (!farewell) {
            farewell = true;
            if (entity() instanceof LivingEntity) {
                NetworkManager.sendToServer(new NpcReplyPacket(entityId, text, JojoNpc.CLOSE));
            }
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
