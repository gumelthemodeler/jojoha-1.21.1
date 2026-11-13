package org.gumel.jojoha.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.data.StandGrowth;
import org.gumel.jojoha.data.StatGrowth;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.fortune.Cursed;
import org.gumel.jojoha.fortune.Devout;
import org.gumel.jojoha.fortune.Fortune;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.registry.ModTraits;
import org.gumel.jojoha.skill.DisciplinePassives;
import org.gumel.jojoha.skill.Inflicts;
import org.gumel.jojoha.stand.BondRank;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.heaven.Ascension;
import org.gumel.jojoha.stand.passive.PassiveLoadout;
import org.gumel.jojoha.stand.passive.StandPassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

final class MenuInfoPage {
    private final PlayerMenuScreen screen;

    MenuInfoPage(PlayerMenuScreen screen) {
        this.screen = screen;
    }

    private static final int DISPLAY_X = 5, DISPLAY_Y = 5, DISPLAY_W = 86, DISPLAY_H = 54;

    private static final float DISPLAY_SCALE = 18F;

    private static final float DISPLAY_ANCHOR = 1.08F;

    private static final float PLAYER_SCALE = 19F;

    private static final int BOX_X = 5, BOX_Y = 60, BOX_W = 86, BOX_H = 38;

    private static final int INFO_Y = 99, INFO_H = 36;

    private static final int INFO_PAIR = 2;

    private static final int INFO_LINES = 2;

    private static final int TRUST_FILL_START = 6;

    private static final int[] TRUST_SEGMENT_END = {23, 43, 79, 99, 118};

    private static final float[][] CHART_CORNERS = {
            {35.5F, 4F}, {64F, 18F}, {64F, 52F}, {35.5F, 66F}, {7F, 52F}, {7F, 18F}};

    private static final float[][] CHART_LABEL_CORNERS = {
            {35.5F, 1F}, {66F, 17.5F}, {66F, 52.5F}, {35.5F, 69F}, {5F, 52.5F}, {5F, 17.5F}};

    private static final float CHART_MIDDLE_X = 35.5F, CHART_MIDDLE_Y = 35F;

    private static final float CHART_FLOOR = 0.06F;

    private static final int CHART_STAND_FILL = 0xB0A41CB0, CHART_STAND_EDGE = 0xFFE07AEA;

    private static final int CHART_PLAYER_FILL = 0xB01F675E, CHART_PLAYER_EDGE = 0xFF5FD8C6;

    private static final int RANGE_CLOSE = 40, RANGE_LONG = 99;

    private static final int TRAY_X = 105, TRAY_Y = 115, TRAY_W = 131, TRAY_H = 44;

    private static final int TRAY_GAP = 2;

    private static final int TRAY_CELL = 44;

    private static final int TRAY_NAMED = 30;

    private static final int TRAY_PITCH_GAP = 4;

    private static final int TRAY_SUB_GAP = 9;

    private static final int COLOR_TRAY_SPLIT = 0x50FFB8D9;

    private static final int TRAY_ROW = TRAY_W / (PlayerMenuScreen.ICON + TRAY_GAP);

    private static final float BAR_MS = 180F;

    private static final int MAX_TRUST_TIER = 3;

    static final int COLOR_PASSIVE = 0xFFFFB8D9;

    private static final ResourceLocation SUB_TRAITS_ICON = ResourceLocation.fromNamespaceAndPath("jojoha",
            "textures/gui/icons/player/passives/sub_traits.png");

    private static final int COLOR_PASSIVE_IDLE = 0xFF4A5A66;

    private static final int COLOR_VAMPIRE = 0xFFE04848;
    private static final int COLOR_PILLAR_MAN = 0xFFE0A050;
    private static final int COLOR_ULTIMATE = 0xFFC890FF;
    private static final int COLOR_HAMON = 0xFFFFC94A;
    private static final int COLOR_ANUBIS = 0xFF7FA8FF;

    static int fateColour(Fate fate) {
        return switch (fate) {
            case HUMAN -> PlayerMenuScreen.COLOR_ACCENT;
            case VAMPIRE -> COLOR_VAMPIRE;
            case PILLAR_MAN -> COLOR_PILLAR_MAN;
            case ULTIMATE_LIFEFORM -> COLOR_ULTIMATE;
        };
    }

    static int disciplineColour(Discipline discipline) {
        return switch (discipline) {
            case NONE -> PlayerMenuScreen.COLOR_ACCENT;
            case HAMON -> COLOR_HAMON;
            case ANUBIS -> COLOR_ANUBIS;
        };
    }

    void renderLeftPage(GuiGraphics guiGraphics, JojohaPlayerData data) {
        int px = PlayerMenuScreen.LEFT_X;
        int py = PlayerMenuScreen.LEFT_Y;

        renderDisplay(guiGraphics, data, px + DISPLAY_X, py + DISPLAY_Y);
        renderIdentity(guiGraphics, data, px, py);
    }

    private void renderIdentity(GuiGraphics guiGraphics, JojohaPlayerData data, int px, int py) {
        int x = px + BOX_X + 3;
        StandType type = PlayerMenuScreen.standType(data);
        if (PlayerMenuScreen.showingStand && type != null) {
            renderStandIdentity(guiGraphics, data, type, x, py);
            return;
        }

        Component fate = Component.translatable(Fate.of(data).translationKey());
        int fateColour = fateColour(Fate.of(data));
        int disciplineColour = disciplineColour(data.discipline);
        Fortune fortune = Fortune.of(data);

        infoRow(guiGraphics, "FATE", PlayerMenuScreen.upper(fate.getString()), fateColour, x, infoY(py + BOX_Y, BOX_H, 0),
                () -> List.of(Component.translatable("menu.jojoha.fate", fate.copy().withColor(fateColour))
                        .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT))));
        infoRow(guiGraphics, "FORTUNE", fortune == null ? "NONE"
                        : PlayerMenuScreen.upper(Component.translatable(fortune.nameKey()).getString()), x,
                infoY(py + BOX_Y, BOX_H, 1), () -> fortuneLines(data));
        infoRow(guiGraphics, "DISCIPLINE", disciplineName(data), disciplineColour, x, infoY(py + INFO_Y, INFO_H, 0),
                () -> disciplineLines(data));
        infoRow(guiGraphics, "TRAIT", traitName(data), x, infoY(py + INFO_Y, INFO_H, 1), () -> traitLines(data));
    }

    private void renderStandIdentity(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type, int x, int py) {
        Component name = Component.translatable("stand." + type.id().getNamespace() + "." + type.id().getPath());
        Component archetype = Component.translatable(type.archetype().translationKey());
        Component trust = Component.translatable("trust_tier.jojoha." + data.stand.trust().key());
        int standColour = StandShadowForm.auraColourFor(data.stand) | 0xFF000000;
        infoRow(guiGraphics, "STAND", PlayerMenuScreen.upper(name.getString()), standColour, x, infoY(py + BOX_Y, BOX_H, 0),
                () -> List.of(name.copy().withStyle(style -> style.withColor(standColour))));
        infoRow(guiGraphics, "ARCHETYPE", PlayerMenuScreen.upper(archetype.getString()), x, infoY(py + BOX_Y, BOX_H, 1),
                () -> List.of(archetype.copy().withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)),
                        screen.figures(Component.translatable(type.archetype().descriptionKey()))));
        infoRow(guiGraphics, "RANGE", type.range().name(), x, infoY(py + INFO_Y, INFO_H, 0),
                () -> List.of(Component.literal(type.range() == StandRange.LONG
                        ? "Long range" : "Close range").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT))));
        infoRow(guiGraphics, "TRUST", PlayerMenuScreen.upper(trust.getString()), x, infoY(py + INFO_Y, INFO_H, 1),
                () -> List.of(trust.copy().withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT))));
    }

    private int infoY(int top, int height, int row) {
        int rowHeight = screen.lineStep() * INFO_LINES;
        int gap = Math.max(0, (height - rowHeight * INFO_PAIR) / (INFO_PAIR + 1));
        return top + gap + row * (rowHeight + gap);
    }

    private void infoRow(GuiGraphics guiGraphics, String label, String value, int x, int y,
                         Supplier<List<Component>> hover) {
        infoRow(guiGraphics, label, value, PlayerMenuScreen.COLOR_ACCENT, x, y, hover);
    }

    private void infoRow(GuiGraphics guiGraphics, String label, String value, int colour, int x, int y,
                         Supplier<List<Component>> hover) {
        int step = screen.lineStep();
        boolean over = screen.within(x, y, BOX_W - 6, step * INFO_LINES);

        screen.smallRaw(guiGraphics, label, x, y, over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_DIM);
        screen.smallLeft(guiGraphics, value, x, y + step, BOX_W - 6, over ? PlayerMenuScreen.COLOR_HOVER : colour);

        if (over) {
            List<Component> lines = hover.get();
            if (!lines.isEmpty()) {
                screen.tooltip = List.copyOf(lines);
            }
        }
    }

    void describe(List<Component> lines, String nameKey, String descKey) {
        screen.more |= Brief.add(lines, nameKey, descKey, screen.detailed, false, PlayerMenuScreen.COLOR_DIM, PlayerMenuScreen.COLOR_FIGURE);
    }

    private List<Component> disciplineLines(JojohaPlayerData data) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("menu.jojoha.discipline",
                        Component.translatable("discipline.jojoha." + data.discipline.name().toLowerCase(Locale.ROOT))
                                .withColor(disciplineColour(data.discipline)))
                .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
        for (HamonPath path : HamonPaths.all()) {
            if (data.discipline == Discipline.HAMON && data.hasHamonPath(path.id())) {
                lines.add(Component.literal(path.displayName()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_TEXT)));
            }
        }
        return lines;
    }

    private List<Component> traitLines(JojohaPlayerData data) {
        ModTraits.Trait trait =
                data.trait == null ? null : ModTraits.byId(data.trait);
        if (trait == null) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(trait.nameKey()).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
        describe(lines, trait.nameKey(), trait.descriptionKey());
        return lines;
    }

    private static String traitName(JojohaPlayerData data) {
        if (data.trait == null) {
            return "NONE";
        }
        ModTraits.Trait trait =
                ModTraits.byId(data.trait);
        return PlayerMenuScreen.upper(trait == null ? data.trait.getPath()
                : Component.translatable(trait.nameKey()).getString());
    }

    private List<Component> fortuneLines(JojohaPlayerData data) {
        Fortune fortune = Fortune.of(data);
        if (fortune == null) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        Component name = Component.translatable(fortune.nameKey());
        if (fortune == Fortune.LUCKY) {
            name = Component.translatable("menu.jojoha.fortune.phobia", name,
                    Component.translatable(Fortune.phobiaKey(data.phobia)));
        } else if (fortune == Fortune.HONORBOUND) {
            name = Component.translatable("menu.jojoha.fortune.honor", name, Math.round(data.honor));
        }
        lines.add(Component.translatable("menu.jojoha.fortune", name).withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
        describe(lines, fortune.nameKey(), fortune.descriptionKey());
        if (fortune == Fortune.LUCKY) {
            lines.add(screen.figures(Component.translatable(Fortune.phobiaKey(data.phobia) + ".desc")));
        }
        if (fortune == Fortune.DEVOUT) {
            for (Devout.Vow vow : Devout.Vow.values()) {
                lines.add(screen.figures(Component.translatable("menu.jojoha.fortune.curse",
                        Component.translatable(vow.key()), Component.translatable(vow.key() + ".desc"))));
            }
            lines.add(screen.figures(Component.translatable("menu.jojoha.fortune.devout.faith",
                    Devout.faith(data), Devout.FAITH_MOST)));
        }
        if (fortune == Fortune.CURSED) {
            for (int curse : Cursed.ALL) {
                if ((data.curses & curse) != 0) {
                    lines.add(screen.figures(Component.translatable("menu.jojoha.fortune.curse",
                            Component.translatable(Cursed.nameKey(curse)),
                            Component.translatable(Cursed.nameKey(curse) + ".desc"))));
                }
            }
            int sense = Cursed.sense(data);
            if ((data.curses & Cursed.BLINDED) != 0 && sense != 0) {
                lines.add(screen.figures(Component.translatable("menu.jojoha.fortune.curse",
                        Component.translatable(Cursed.senseKey(sense)),
                        Component.translatable(Cursed.senseKey(sense) + ".desc"))));
            }
        }
        return lines;
    }

    private static String disciplineName(JojohaPlayerData data) {
        return PlayerMenuScreen.upper(data.discipline.name());
    }

    private void renderDisplay(GuiGraphics guiGraphics, JojohaPlayerData data, int viewX, int viewY) {
        if (PlayerMenuScreen.showingStand) {
            if (data.stand.isPresent()) {
                clipped(guiGraphics, viewX, viewY, DISPLAY_W, DISPLAY_H,
                        () -> StandPortrait.render(guiGraphics, data, viewX, viewY, DISPLAY_W, DISPLAY_H,
                                DISPLAY_SCALE, DISPLAY_ANCHOR, StandPortrait.ROTATION,
                                (float) screen.localMouseX, (float) screen.localMouseY));
            }
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                clipped(guiGraphics, viewX, viewY, DISPLAY_W, DISPLAY_H,
                        () -> renderPlayerFollowingMouse(guiGraphics, player, viewX, viewY, DISPLAY_W, DISPLAY_H,
                                PLAYER_SCALE));
            }
        }

        if (!screen.within(viewX, viewY, DISPLAY_W, DISPLAY_H)) {
            return;
        }

        List<Component> lines = new ArrayList<>();
        StandType type = PlayerMenuScreen.standType(data);
        if (PlayerMenuScreen.showingStand && type != null) {
            screen.standTooltip(guiGraphics, data, type, viewX, viewY, DISPLAY_W, DISPLAY_H);
            lines.addAll(screen.tooltip);
            lines.add(Component.translatable(type.archetype().translationKey())
                    .withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
            lines.add(screen.figures(Component.translatable(type.archetype().descriptionKey())));
        } else if (PlayerMenuScreen.showingStand) {
            lines.add(Component.translatable("menu.jojoha.no_stand").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        } else {
            lines.add(Component.translatable("menu.jojoha.still_you").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_ACCENT)));
        }
        screen.tooltip = List.copyOf(lines);
    }

    private record Passive(ResourceLocation id, String nameKey, String descKey, boolean on, int colour) {
    }

    private void collectTraits(JojohaPlayerData data, List<Passive> main, List<Passive> minor) {
        if (PlayerMenuScreen.showingStand) {
            StandType type = PlayerMenuScreen.standType(data);
            if (type == null) {
                return;
            }
            for (ResourceLocation passiveId : passiveRows(data, type)) {
                StandPassive passive = StandPassives.byId(passiveId);
                if (passive == null) {
                    continue;
                }
                ResourceLocation family = passive.replaces() == null ? passiveId : passive.replaces();
                boolean own = type.passives().contains(family);
                boolean always = PassiveLoadout.isArchetype(data, passiveId);
                (always ? minor : main).add(new Passive(passiveId, passive.translationKey(),
                        passive.translationKey() + ".desc", true, COLOR_PASSIVE));
            }
            return;
        }
        for (DisciplinePassives.DisciplinePassive passive
                : DisciplinePassives.carried(data)) {
            (passive.minor() ? minor : main).add(new Passive(passive.id(), passive.translationKey(),
                    passive.descriptionKey(), passive.active().test(data), COLOR_PASSIVE));
        }
    }

    private void drawTraits(GuiGraphics guiGraphics, JojohaPlayerData data) {
        int step = screen.lineStep();

        List<Passive> main = new ArrayList<>();
        List<Passive> minor = new ArrayList<>();
        collectTraits(data, main, minor);

        if (main.isEmpty() && minor.isEmpty()) {
            screen.smallRaw(guiGraphics, "PASSIVES", TRAY_X, TRAY_Y, PlayerMenuScreen.COLOR_DIM);
            screen.smallRaw(guiGraphics, PlayerMenuScreen.showingStand && !data.stand.isPresent() ? "No Stand yet" : "None yet",
                    TRAY_X, TRAY_Y + (TRAY_H - step) / 2, COLOR_PASSIVE_IDLE);
            return;
        }

        int count = main.size() + (minor.isEmpty() ? 0 : 1);
        int rows = count > TRAY_ROW ? 2 : 1;
        int across = (count + rows - 1) / rows;
        int cell = Math.min(TRAY_CELL, TRAY_W / across);
        boolean named = rows == 1 && cell >= TRAY_NAMED;

        int tall = rows == 1 ? step + TRAY_GAP + PlayerMenuScreen.ICON + (named ? TRAY_GAP + step : 0) : 2 * PlayerMenuScreen.ICON + TRAY_GAP;
        int top = TRAY_Y + Math.max(0, (TRAY_H - tall) / 2);
        if (rows == 1) {
            screen.smallRaw(guiGraphics, "PASSIVES", TRAY_X, top, COLOR_PASSIVE);
            top += step + TRAY_GAP;
        }

        int columns = (main.size() + 1) / 2;
        int subWidth = minor.isEmpty() ? 0 : TRAY_SUB_GAP + PlayerMenuScreen.ICON;
        int pitch = Math.min(PlayerMenuScreen.ICON + TRAY_PITCH_GAP,
                (TRAY_W - subWidth + TRAY_PITCH_GAP) / Math.max(1, columns));
        int gridWidth = columns * pitch - (pitch - PlayerMenuScreen.ICON);
        int gridX = TRAY_X + (TRAY_W - gridWidth - subWidth) / 2;
        if (rows == 2 && subWidth > 0) {
            int lineX = gridX + gridWidth + TRAY_SUB_GAP / 2;
            guiGraphics.fill(lineX, top + 2, lineX + 1, top + 2 * PlayerMenuScreen.ICON + TRAY_GAP - 2, COLOR_TRAY_SPLIT);
        }

        for (int i = 0; i < count; i++) {
            boolean sub = i >= main.size();
            int cellX;
            int x;
            int y;
            if (rows == 1) {
                cellX = TRAY_X + (TRAY_W - count * cell) / 2 + i * cell;
                x = cellX + (cell - PlayerMenuScreen.ICON) / 2;
                y = top;
            } else {
                cell = PlayerMenuScreen.ICON;
                x = sub ? gridX + gridWidth + TRAY_SUB_GAP : gridX + (i % columns) * pitch;
                y = sub ? top + (PlayerMenuScreen.ICON + TRAY_GAP) / 2 : top + (i / columns) * (PlayerMenuScreen.ICON + TRAY_GAP);
                cellX = x;
            }
            String name;
            int colour;

            if (sub) {
                PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.SKILL_BASE_ICON, x, y, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);
                PlayerMenuScreen.blit(guiGraphics, SUB_TRAITS_ICON, x + 1, y + 1, SkillIcons.SIZE, SkillIcons.SIZE);
                PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.SKILL_ICON_FRAME, x, y, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);
                String total = String.valueOf(minor.size());
                guiGraphics.drawString(screen.font(), total, x + PlayerMenuScreen.ICON - screen.font().width(total),
                        y + PlayerMenuScreen.ICON - screen.font().lineHeight + 1, PlayerMenuScreen.COLOR_TEXT, true);
                name = Component.translatable("menu.jojoha.sub_traits").getString();
                colour = COLOR_PASSIVE;
            } else {
                Passive trait = main.get(i);
                drawTrait(guiGraphics, trait, x, y);
                name = Component.translatable(trait.nameKey()).getString();
                colour = trait.on() ? trait.colour() : PlayerMenuScreen.COLOR_DIM;
            }

            if (named) {
                String shown = screen.trim(name, Math.round((cell - TRAY_GAP) / screen.textScale));
                int width = Math.round(screen.font().width(shown) * screen.textScale);
                screen.smallRaw(guiGraphics, shown, cellX + (cell - width) / 2, y + PlayerMenuScreen.ICON + TRAY_GAP, colour);
            }

            if (screen.within(cellX, y, cell, PlayerMenuScreen.ICON + (named ? TRAY_GAP + step : 0))) {
                if (sub) {
                    subTraitTooltip(minor);
                } else {
                    traitTooltip(main.get(i));
                }
            }
        }
    }

    private void drawTrait(GuiGraphics guiGraphics, Passive trait, int x, int y) {
        PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.SKILL_BASE_ICON, x, y, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);

        ResourceLocation icon = SkillIcons.of(trait.id());
        if (icon != null) {
            PlayerMenuScreen.blit(guiGraphics, icon, x + 1, y + 1, SkillIcons.SIZE, SkillIcons.SIZE);
        } else {
            SkillIcons.drawName(guiGraphics, screen.font(), Component.translatable(trait.nameKey()), x + 1, y + 1,
                    SkillIcons.SIZE);
        }

        PlayerMenuScreen.blit(guiGraphics, PlayerMenuScreen.SKILL_ICON_FRAME, x, y, PlayerMenuScreen.ICON, PlayerMenuScreen.ICON);
        if (!trait.on()) {
            guiGraphics.fill(x + 1, y + 1, x + PlayerMenuScreen.ICON - 1, y + PlayerMenuScreen.ICON - 1, PlayerMenuScreen.COLOR_LOCKED);
        }
    }

    private void traitTooltip(Passive trait) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(trait.nameKey()).withStyle(style -> style.withColor(trait.colour())));
        describe(lines, trait.nameKey(), trait.descKey());
        lines.add(Component.literal(trait.on() ? "In effect" : "Not yet")
                .withStyle(style -> style.withColor(trait.on() ? PlayerMenuScreen.COLOR_TEXT : PlayerMenuScreen.COLOR_DIM)));
        screen.tooltip = lines;
    }

    private void subTraitTooltip(List<Passive> traits) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("menu.jojoha.sub_traits").withStyle(style -> style.withColor(COLOR_PASSIVE)));
        lines.add(Component.translatable("menu.jojoha.sub_traits.hint").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)));
        for (Passive trait : traits) {
            Component name = Component.translatable(trait.nameKey())
                    .withStyle(style -> style.withColor(trait.on() ? PlayerMenuScreen.COLOR_TEXT : PlayerMenuScreen.COLOR_DIM));
            if (screen.detailed) {
                lines.add(name);
                lines.add(Component.literal("  ").append(screen.figures(Component.translatable(trait.descKey()))));
                continue;
            }
            screen.more |= Brief.has(trait.nameKey());
            lines.add(Component.empty().append(name)
                    .append(Component.literal(": ").withStyle(style -> style.withColor(PlayerMenuScreen.COLOR_DIM)))
                    .append(Brief.summary(trait.nameKey(), trait.descKey(), PlayerMenuScreen.COLOR_DIM, PlayerMenuScreen.COLOR_FIGURE)));
        }
        screen.tooltip = lines;
    }

    private static List<ResourceLocation> passiveRows(JojohaPlayerData data, StandType type) {
        return PassiveLoadout.inEffect(data).stream()
                .filter(id -> Ascension.shows(data, id))
                .toList();
    }

    void renderCentre(GuiGraphics guiGraphics, JojohaPlayerData data) {
        drawTrustBar(guiGraphics, data);
        collectTrustTooltip(data);
        drawChart(guiGraphics, data);
        drawTraits(guiGraphics, data);
    }

    private void renderPlayerFollowingMouse(GuiGraphics guiGraphics, LocalPlayer player,
                                            int viewX, int viewY, int viewW, int viewH, float scale) {
        float centreX = viewX + viewW / 2F;
        float centreY = viewY + viewH / 2F;

        float yaw = (float) Math.atan((centreX - screen.localMouseX) / 40F);
        float pitch = (float) Math.atan((centreY - screen.localMouseY) / 40F);

        Quaternionf facing = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf tilt = new Quaternionf().rotateX(pitch * 20F * ((float) Math.PI / 180F));
        facing.mul(tilt);

        float bodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headRotO = player.yHeadRotO;
        float headRot = player.yHeadRot;

        player.yBodyRot = 180F + yaw * 20F;
        player.setYRot(180F + yaw * 40F);
        player.setXRot(-pitch * 20F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();

        float size = player.getScale();
        MenuIdlePose.drawing(true);
        try {
            InventoryScreen.renderEntityInInventory(guiGraphics, centreX, centreY, scale / size,
                    new Vector3f(0F, player.getBbHeight() / 2F, 0F), facing, tilt, player);
        } finally {
            MenuIdlePose.drawing(false);
        }

        player.yBodyRot = bodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headRotO;
        player.yHeadRot = headRot;
    }

    private static int statValue(JojohaPlayerData data, boolean stand, int stat) {
        if (stand) {
            return data.stand.isPresent() ? data.stand.stats()[stat] : 0;
        }
        return StatPoints.playerStat(data, stat);
    }

    private void drawChart(GuiGraphics guiGraphics, JojohaPlayerData data) {
        boolean stand = PlayerMenuScreen.showingStand;
        String[] labels = chartLabels(stand);
        boolean charted = !stand || data.stand.isPresent();

        if (charted) {
            int[] values = chartValues(data, stand);
            float[][] shape = new float[6][2];
            for (int i = 0; i < 6; i++) {
                float reach = Mth.clamp(values[i] / (float) StatPoints.MAX_STAT, CHART_FLOOR, 1F);
                shape[i][0] = CHART_MIDDLE_X + (CHART_CORNERS[i][0] - CHART_MIDDLE_X) * reach;
                shape[i][1] = CHART_MIDDLE_Y + (CHART_CORNERS[i][1] - CHART_MIDDLE_Y) * reach;
            }
            fillShape(guiGraphics, shape, stand ? CHART_STAND_FILL : CHART_PLAYER_FILL,
                    stand ? CHART_STAND_EDGE : CHART_PLAYER_EDGE);
        }

        int step = screen.lineStep();
        for (int i = 0; i < 6; i++) {
            int width = Math.round(screen.font().width(labels[i]) * screen.textScale);
            int cx = PlayerMenuScreen.CHART_X + Math.round(CHART_LABEL_CORNERS[i][0]);
            int cy = PlayerMenuScreen.CHART_Y + Math.round(CHART_LABEL_CORNERS[i][1]);
            int x;
            int y;
            if (i == 0) {
                x = cx - width / 2;
                y = cy - step - 1;
            } else if (i == 3) {
                x = cx - width / 2;
                y = cy + 2;
            } else if (i < 3) {
                x = cx + 2;
                y = cy - step / 2;
            } else {
                x = cx - 2 - width;
                y = cy - step / 2;
            }
            boolean over = screen.within(x - 3, y - 3, width + 6, step + 6);
            screen.smallRaw(guiGraphics, labels[i], x, y, !charted ? COLOR_PASSIVE_IDLE : over ? PlayerMenuScreen.COLOR_HOVER : PlayerMenuScreen.COLOR_TEXT);
            if (over && charted) {
                chartTooltip(data, stand, i);
            }
        }
    }

    private static String[] chartLabels(boolean stand) {
        return stand
                ? new String[]{"POW", "SPD", "END", "PRO", "RNG", "POT"}
                : new String[]{"STR", "AGI", "END", "VIT", "OVR", "WOR"};
    }

    private static int[] chartValues(JojohaPlayerData data, boolean stand) {
        if (stand) {
            StandType type = PlayerMenuScreen.standType(data);
            int range = type != null && type.range() == StandRange.LONG ? RANGE_LONG : RANGE_CLOSE;
            return new int[]{data.stand.power(), data.stand.speed(), data.stand.endurance(),
                    data.stand.protection(), range, data.stand.potential()};
        }
        int overall = (data.strength + data.vitality + data.agility + data.endurance + data.worthiness) / 5;
        return new int[]{data.strength, data.agility, data.endurance, data.vitality, overall, data.worthiness};
    }

    private void chartTooltip(JojohaPlayerData data, boolean stand, int corner) {
        if (stand && corner == 4) {
            StandType type = PlayerMenuScreen.standType(data);
            boolean far = type != null && type.range() == StandRange.LONG;
            screen.tooltip = List.of(
                    Component.translatable("menu.jojoha.range").withStyle(ChatFormatting.WHITE),
                    Component.translatable(far ? "menu.jojoha.range.long" : "menu.jojoha.range.close"),
                    Component.translatable("menu.jojoha.range.fixed").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (!stand && corner == 4) {
            screen.tooltip = List.of(
                    Component.translatable("menu.jojoha.overall").withStyle(ChatFormatting.WHITE),
                    Component.translatable("menu.jojoha.overall_average"),
                    Component.literal(String.valueOf(chartValues(data, false)[4]))
                            .withStyle(ChatFormatting.WHITE));
            return;
        }
        int[] standOrder = {StatPoints.POWER, StatPoints.SPEED, StatPoints.ENDURANCE, StatPoints.PROTECTION, -1,
                StatPoints.POTENTIAL};
        int[] playerOrder = {StatPoints.STRENGTH, StatPoints.AGILITY, StatPoints.ENDURANCE, StatPoints.VITALITY, -1,
                StatPoints.WORTHINESS};
        int stat = stand ? standOrder[corner] : playerOrder[corner];
        statTooltip(data, stand && stat != StatPoints.ENDURANCE, stat);
    }

    private static void fillShape(GuiGraphics guiGraphics, float[][] shape, int fill, int edge) {
        boolean[] inside = new boolean[PlayerMenuScreen.CHART_W * PlayerMenuScreen.CHART_H];
        for (int y = 0; y < PlayerMenuScreen.CHART_H; y++) {
            for (int x = 0; x < PlayerMenuScreen.CHART_W; x++) {
                inside[y * PlayerMenuScreen.CHART_W + x] = contains(shape, x + 0.5F, y + 0.5F);
            }
        }
        for (int y = 0; y < PlayerMenuScreen.CHART_H; y++) {
            int runStart = -1;
            int runColour = 0;
            for (int x = 0; x <= PlayerMenuScreen.CHART_W; x++) {
                int colour = 0;
                if (x < PlayerMenuScreen.CHART_W && inside[y * PlayerMenuScreen.CHART_W + x]) {
                    boolean border = x == 0 || y == 0 || x == PlayerMenuScreen.CHART_W - 1 || y == PlayerMenuScreen.CHART_H - 1
                            || !inside[y * PlayerMenuScreen.CHART_W + x - 1] || !inside[y * PlayerMenuScreen.CHART_W + x + 1]
                            || !inside[(y - 1) * PlayerMenuScreen.CHART_W + x] || !inside[(y + 1) * PlayerMenuScreen.CHART_W + x];
                    colour = border ? edge : fill;
                }
                if (colour != runColour) {
                    if (runColour != 0) {
                        guiGraphics.fill(PlayerMenuScreen.CHART_X + runStart, PlayerMenuScreen.CHART_Y + y, PlayerMenuScreen.CHART_X + x, PlayerMenuScreen.CHART_Y + y + 1, runColour);
                    }
                    runStart = x;
                    runColour = colour;
                }
            }
        }
    }

    static boolean contains(float[][] shape, float x, float y) {
        boolean in = false;
        for (int i = 0, j = shape.length - 1; i < shape.length; j = i++) {
            float xi = shape[i][0], yi = shape[i][1], xj = shape[j][0], yj = shape[j][1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    private boolean noStatYet(JojohaPlayerData data, boolean stand, int i) {
        if (stand && !data.stand.isPresent()) {
            screen.tooltip = List.of(Component.translatable("stat.jojoha.stand." + StatPoints.STAND_NAMES[i])
                            .withStyle(ChatFormatting.WHITE),
                    Component.translatable("menu.jojoha.no_stand").withStyle(ChatFormatting.DARK_GRAY));
            return true;
        }
        return false;
    }

    private static String statKey(boolean stand, int i) {
        return (stand ? "stand." : "") + (stand ? StatPoints.STAND_NAMES[i] : StatPoints.PLAYER_NAMES[i]);
    }

    private void statTooltip(JojohaPlayerData data, boolean stand, int i) {
        if (noStatYet(data, stand, i)) {
            return;
        }
        String name = statKey(stand, i);
        int level = statValue(data, stand, i);
        boolean shared = !stand && i == StatPoints.ENDURANCE;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("stat.jojoha." + name)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" " + level).withStyle(ChatFormatting.GRAY)));
        if (shared) {
            lines.add(Component.translatable("menu.jojoha.shared_stat")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        lines.add(Component.translatable("stat.jojoha." + name + ".how").withStyle(ChatFormatting.GRAY));

        if (level >= StatPoints.MAX_STAT) {
            lines.add(Component.translatable("stat.jojoha.maxed").withStyle(ChatFormatting.GRAY));
        } else {
            int percent = Math.round(StatGrowth.towardsNext(data, stand, i) * 100F);
            lines.add(Component.translatable("stat.jojoha.progress",
                    Component.literal(String.valueOf(level + 1)).withStyle(ChatFormatting.WHITE),
                    Component.literal(percent + "%").withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }

        List<StatPerk> perks = new ArrayList<>(StatPerk.of(stand, i));
        if (shared && data.stand.isPresent()) {
            perks.addAll(StatPerk.of(true, StatPoints.STAND_ENDURANCE));
        }
        if (perks.isEmpty() && screen.detailed) {
            lines.add(Component.translatable("menu.jojoha.no_milestones").withStyle(ChatFormatting.DARK_GRAY));
        }
        boolean nextShown = false;
        int listed = 0;
        for (StatPerk perk : perks) {
            boolean reached = perk.reached(level);
            boolean next = !reached && !nextShown;
            nextShown |= next;
            if (!screen.detailed && !next) {
                continue;
            }
            listed++;
            lines.add(Component.literal(perk.threshold() + " ")
                    .withStyle(reached ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                    .append(Component.translatable(perk.nameKey()).withStyle(reached || next
                            ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)));
            if (reached || next) {
                lines.add(Component.literal("  ").append(Component.translatable(perk.descriptionKey()))
                        .withStyle(reached ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
            }
        }
        screen.more |= listed < perks.size();

        List<Inflicts.Status> found = new ArrayList<>();
        for (StatPerk perk : perks) {
            for (Inflicts.Status status : Inflicts.of(perk)) {
                if (!found.contains(status)) {
                    found.add(status);
                }
            }
        }
        screen.keywords = found;
        screen.tooltip = lines;
    }

    private void collectTrustTooltip(JojohaPlayerData data) {
        if (!data.stand.isPresent() || !screen.within(PlayerMenuScreen.TRUST_X, PlayerMenuScreen.TRUST_Y, PlayerMenuScreen.TRUST_W, PlayerMenuScreen.TRUST_H)) {
            return;
        }
        BondRank rank = BondRank.of(data.stand);
        Component standing = Component.translatable("trust_tier.jojoha." + data.stand.trust().key());
        if (rank != BondRank.NONE) {
            standing = Component.translatable(rank.translationKey());
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("trust.jojoha.tooltip.title", standing)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("trust.jojoha.tooltip.how",
                Component.literal(String.valueOf(BondRank.averageStat(data.stand)))
                        .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        int next = StandGrowth.nextTrustAverage(data.stand);
        lines.add(next < 0
                ? Component.translatable("trust.jojoha.tooltip.max")
                : Component.translatable("trust.jojoha.tooltip.next",
                        Component.literal(String.valueOf(next)).withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GRAY));
        if (rank.energyBonus() > 0F) {
            lines.add(Component.translatable("trust.jojoha.tooltip.energy", Math.round(rank.energyBonus())));
        }
        screen.tooltip = lines;
    }

    private void drawTrustBar(GuiGraphics guiGraphics, JojohaPlayerData data) {
        if (!data.stand.isPresent()) {
            return;
        }
        int filled = trustFill(data.stand);
        if (filled > TRUST_FILL_START) {
            PlayerMenuScreen.blitRegion(guiGraphics, PlayerMenuScreen.TRUST_FULL, PlayerMenuScreen.TRUST_X, PlayerMenuScreen.TRUST_Y, 0F, 0F, filled, PlayerMenuScreen.TRUST_H, PlayerMenuScreen.TRUST_W, PlayerMenuScreen.TRUST_H);
        }
    }

    private static int trustFill(StandData stand) {
        int average = BondRank.averageStat(stand);
        int tier = Mth.clamp(stand.trustTier(), 0, MAX_TRUST_TIER);
        int[] from = {StandGrowth.trustAverage(0), StandGrowth.trustAverage(1),
                StandGrowth.trustAverage(2), StandGrowth.trustAverage(3),
                BondRank.RANK_I.average()};
        int[] to = {from[1], from[2], from[3], from[4], BondRank.RESONANT.average()};

        int filled = TRUST_FILL_START;
        for (int segment = 0; segment < TRUST_SEGMENT_END.length; segment++) {
            float share;
            if (segment < MAX_TRUST_TIER) {
                share = tier > segment ? 1F : tier == segment ? progress(average, from[segment], to[segment]) : 0F;
            } else {
                share = tier < MAX_TRUST_TIER ? 0F : progress(average, from[segment], to[segment]);
            }
            if (share <= 0F) {
                break;
            }
            int start = segment == 0 ? TRUST_FILL_START : TRUST_SEGMENT_END[segment - 1];
            filled = start + Math.round((TRUST_SEGMENT_END[segment] - start) * share);
            if (share < 1F) {
                break;
            }
        }
        return filled;
    }

    static float progress(int value, int from, int to) {
        return to <= from ? 1F : Mth.clamp((value - from) / (float) (to - from), 0F, 1F);
    }

    void clipped(GuiGraphics guiGraphics, int x, int y, int w, int h, Runnable draw) {
        guiGraphics.enableScissor(screen.screenX(x), screen.screenY(y), screen.screenX(x + w), screen.screenY(y + h));
        draw.run();
        guiGraphics.flush();
        guiGraphics.disableScissor();
    }

    int neededLift() {
        if (!PlayerMenuScreen.combatBarOpen && barProgress() <= 0F) {
            return 0;
        }

        int overhang = screen.offsetY + (PlayerMenuScreen.CB_Y + PlayerMenuScreen.CB_H) * screen.scale - (screen.height - PlayerMenuScreen.SCREEN_EDGE);
        return Mth.clamp(overhang, 0, Math.max(0, screen.offsetY - PlayerMenuScreen.TOP_OVERHANG * screen.scale - PlayerMenuScreen.SCREEN_EDGE));
    }

    float barProgress() {
        float target = PlayerMenuScreen.combatBarOpen ? 1F : 0F;
        if (screen.barChangedAt == Long.MIN_VALUE) {
            return target;
        }

        float t = Mth.clamp((System.currentTimeMillis() - screen.barChangedAt) / BAR_MS, 0F, 1F);
        return Mth.lerp(t, screen.barProgressAtChange, target);
    }
}
