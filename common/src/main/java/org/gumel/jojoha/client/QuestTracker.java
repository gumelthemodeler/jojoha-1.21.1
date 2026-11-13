package org.gumel.jojoha.client;

import net.minecraft.world.entity.npc.Villager;
import org.gumel.jojoha.entity.VampireEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.Quests;
import org.gumel.jojoha.worldevent.WorldEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class QuestTracker {
    private static final ResourceLocation SEPARATOR = PlayerMenuScreen.menu("frames/active_quest_separator.png");
    private static final ResourceLocation SEPARATOR_TINT = PlayerMenuScreen.menu("frames/active_quest_separator_tint.png");
    private static final ResourceLocation PIP_EMPTY = PlayerMenuScreen.menu("frames/quest_pip_empty.png");
    private static final ResourceLocation PIP_FULL = PlayerMenuScreen.menu("frames/quest_pip_full.png");
    private static final ResourceLocation MOON = PlayerMenuScreen.menu("frames/quest_moon.png");
    private static final int WIDTH = 114;
    private static final int SEPARATOR_H = 4;
    private static final int MARGIN = 6;
    private static final int LINE = 10;
    private static final int PANEL_PAD = 6;
    private static final int ICON_GAP = 5;
    private static final int COUNT_ROOM = 30;
    private static final int PIP = 7;
    private static final int PIP_GAP = 2;
    private static final int MAX_PIPS = 10;
    private static final int MOON_SIZE = 9;
    private static final long SLIDE_MS = 450L;
    private static final long PULSE_MS = 700L;
    private static final long AWAKE_MS = 10000L;
    private static final float IDLE_ALPHA = 0.4F;
    private static final float FADE_MS = 600F;
    private static final double CLOSE = 16.0;
    private static final double NEAR = 64.0;
    private static final double GIVER_RANGE = 96.0;
    private static final int DAWN = 23000;
    private static final int DAY = 24000;
    private static final int TITLE_COLOUR = 0xFFE0B040;
    private static final int GOAL_COLOUR = 0xFFE0E0E0;
    private static final int COUNT_COLOUR = 0xFFFFFFFF;
    private static final int PULSE_COLOUR = 0xFFFFDD55;
    private static final int SEEN_COLOUR = 0xFFD07070;
    private static final int DIR_COLOUR = 0xFFB8B0A0;
    private static final int SHIMMER_COLOUR = 0xFFFFF4D0;
    private static final int NIGHT_COLOUR = 0xFFC02020;
    private static final int DAWN_COLOUR = 0xFFE0A0A0;
    private static final int SLAY_COLOUR = 0xFFE06050;
    private static final int HUNT_COLOUR = 0xFF70A0E0;
    private static final int SURVIVE_COLOUR = 0xFF80D070;
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static float shown = 1F;
    private static long lastFrame;

    private record Target(Vec3 at, double distance) {
    }

    private QuestTracker() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()
                || mc.screen instanceof NpcDialogueScreen || mc.screen instanceof PlayerMenuScreen) {
            return;
        }
        ClientQuests.Entry entry = ClientQuests.tracked();
        if (entry == null) {
            return;
        }
        Font font = mc.font;
        long now = Util.getMillis();
        float slide = Mth.clamp((now - ClientQuests.addedAt(entry.id())) / (float) SLIDE_MS, 0F, 1F);
        float eased = 1F - (1F - slide) * (1F - slide) * (1F - slide);
        float pulse = 1F - Mth.clamp((now - ClientQuests.changedAt(entry.id())) / (float) PULSE_MS, 0F, 1F);
        Quests.Objective objective = QuestIcons.objective(entry.id());
        Target target = target(mc, entry);
        boolean night = WorldEventSky.active(WorldEvent.VAMPIRE_NIGHT)
                && (objective == Quests.Objective.SLAY || objective == Quests.Objective.SURVIVE);

        boolean awake = now - ClientQuests.changedAt(entry.id()) < AWAKE_MS
                || now - ClientQuests.addedAt(entry.id()) < AWAKE_MS
                || mc.screen != null
                || target != null && target.distance() <= CLOSE;
        float step = lastFrame == 0L ? 1F : Mth.clamp((now - lastFrame) / FADE_MS, 0F, 1F);
        lastFrame = now;
        shown += ((awake ? 1F : IDLE_ALPHA) - shown) * step;
        float alpha = shown;

        String base = "quest.jojoha." + entry.id();
        String direction = target == null ? "" : direction(mc, entry, target);
        List<FormattedCharSequence> heading = font.split(
                Component.translatable(base + (entry.ready() ? ".return" : ".name")), WIDTH);
        List<FormattedCharSequence> goal = entry.ready() ? List.of()
                : font.split(Component.translatable(base + ".track"), WIDTH - (entry.needed() > MAX_PIPS ? COUNT_ROOM : 0));
        boolean pips = !entry.ready() && entry.needed() > 1 && entry.needed() <= MAX_PIPS;
        boolean bar = !entry.ready() && entry.needed() > MAX_PIPS;
        int content = heading.size() * LINE;
        if (!entry.ready()) {
            content += 1 + SEPARATOR_H + 3 + goal.size() * LINE;
            content += pips ? PIP + 3 : bar ? QuestIcons.BAR_H + 3 : 0;
        }
        content += direction.isEmpty() ? 0 : LINE;
        content += night ? LINE : 0;
        int panelWidth = PANEL_PAD * 2 + QuestIcons.ICON + ICON_GAP + WIDTH;
        int panelHeight = Math.max(QuestIcons.ICON + PANEL_PAD * 2, content + PANEL_PAD * 2 - 2);
        int panelLeft = graphics.guiWidth() - panelWidth - MARGIN + Math.round((1F - eased) * (panelWidth + MARGIN * 2));
        int panelTop = graphics.guiHeight() / 3 - PANEL_PAD;
        int left = panelLeft + PANEL_PAD + QuestIcons.ICON + ICON_GAP;
        int top = panelTop + PANEL_PAD;

        graphics.setColor(1F, 1F, 1F, alpha);
        DialogueFrame.draw(graphics, panelLeft, panelTop, panelWidth, panelHeight);
        QuestIcons.icon(graphics, objective, panelLeft + PANEL_PAD, top - 3);
        graphics.setColor(1F, 1F, 1F, 1F);

        int y = top;
        if (entry.ready()) {
            for (FormattedCharSequence line : heading) {
                shimmer(graphics, font, line, left, y, now, alpha);
                y += LINE;
            }
        } else {
            for (FormattedCharSequence line : heading) {
                graphics.drawString(font, line, left, y, fade(TITLE_COLOUR, alpha), true);
                y += LINE;
            }
            separator(graphics, objective, left, y + 1, alpha, night ? nightPulse(now) : 0F);
            y += 1 + SEPARATOR_H + 3;
            int goalColour = fade(blend(GOAL_COLOUR, PULSE_COLOUR, pulse), alpha);
            for (int i = 0; i < goal.size(); i++) {
                graphics.drawString(font, goal.get(i), left, y, goalColour, true);
                if (i == 0 && bar) {
                    String count = entry.done() + "/" + entry.needed();
                    graphics.pose().pushPose();
                    float grow = 1F + pulse * 0.35F;
                    graphics.pose().translate(left + WIDTH - font.width(count) / 2F, y + 4F, 0F);
                    graphics.pose().scale(grow, grow, 1F);
                    graphics.drawString(font, count, -font.width(count) / 2, -4,
                            fade(blend(COUNT_COLOUR, PULSE_COLOUR, pulse), alpha), true);
                    graphics.pose().popPose();
                }
                y += LINE;
            }
            if (pips) {
                pips(graphics, entry, left, y, pulse, alpha);
                y += PIP + 3;
            } else if (bar) {
                graphics.setColor(1F, 1F, 1F, alpha);
                QuestIcons.bar(graphics, left, y, WIDTH, entry.done() / (float) Math.max(1, entry.needed()), false);
                graphics.setColor(1F, 1F, 1F, 1F);
                y += QuestIcons.BAR_H + 3;
            }
        }
        if (!direction.isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(direction, WIDTH), left, y,
                    fade(entry.marked() && !entry.ready() ? SEEN_COLOUR : DIR_COLOUR, alpha), true);
            y += LINE;
        }
        if (night) {
            dawn(graphics, mc, font, left, y, alpha);
        }
    }

    @Nullable
    private static Target target(Minecraft mc, ClientQuests.Entry entry) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Vec3 eye = mc.player.position();
        if (entry.ready()) {
            Quests.Quest quest = Quests.byId(entry.id());
            String giver = quest == null ? Quests.HAMON : quest.giver();
            Entity nearest = null;
            double best = GIVER_RANGE * GIVER_RANGE;
            for (Entity entity : mc.level.entitiesForRendering()) {
                boolean gives = Quests.VAMPIRE.equals(giver) ? entity instanceof VampireEntity
                        : Quests.HAMON.equals(giver) ? entity instanceof HamonStudent
                        : entity instanceof Villager villager && !villager.isBaby();
                if (gives && entity.isAlive()) {
                    double d = entity.distanceToSqr(eye);
                    if (d < best) {
                        best = d;
                        nearest = entity;
                    }
                }
            }
            return nearest == null ? null : new Target(nearest.position(), Math.sqrt(best));
        }
        if (entry.marked()) {
            BlockPos at = BlockPos.of(entry.mark());
            Vec3 spot = new Vec3(at.getX() + 0.5, eye.y, at.getZ() + 0.5);
            return new Target(spot, spot.distanceTo(eye));
        }
        return null;
    }

    private static String direction(Minecraft mc, ClientQuests.Entry entry, Target target) {
        if (mc.player == null) {
            return "";
        }
        double dx = target.at().x - mc.player.getX();
        double dz = target.at().z - mc.player.getZ();
        float bearing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        float turn = Mth.wrapDegrees(bearing - mc.player.getYRot());
        int index = Math.floorMod(Math.round(turn / 45F), ARROWS.length);
        String word = target.distance() <= CLOSE ? "close" : target.distance() <= NEAR ? "near" : "far";
        if (entry.marked() && !entry.ready()) {
            return ARROWS[index] + " " + Component.translatable("menu.jojoha.quests.seen", (int) Math.round(target.distance())).getString();
        }
        return ARROWS[index] + " " + Component.translatable("menu.jojoha.quests.dir." + word).getString();
    }

    private static void separator(GuiGraphics graphics, @Nullable Quests.Objective objective, int x, int y, float alpha,
                                  float night) {
        int colour = typeColour(objective);
        if (colour == 0 && night <= 0F) {
            graphics.setColor(1F, 1F, 1F, alpha);
            graphics.blit(SEPARATOR, x, y, 0F, 0F, WIDTH, SEPARATOR_H, WIDTH, SEPARATOR_H);
            graphics.setColor(1F, 1F, 1F, 1F);
            return;
        }
        int tint = blend(colour == 0 ? TITLE_COLOUR : colour, NIGHT_COLOUR, night);
        graphics.setColor(FastColor.ARGB32.red(tint) / 255F, FastColor.ARGB32.green(tint) / 255F,
                FastColor.ARGB32.blue(tint) / 255F, alpha);
        graphics.blit(SEPARATOR_TINT, x, y, 0F, 0F, WIDTH, SEPARATOR_H, WIDTH, SEPARATOR_H);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    private static int typeColour(@Nullable Quests.Objective objective) {
        if (objective == null) {
            return 0;
        }
        return switch (objective) {
            case SLAY -> SLAY_COLOUR;
            case HUNT -> HUNT_COLOUR;
            case SURVIVE -> SURVIVE_COLOUR;
            default -> 0;
        };
    }

    private static float nightPulse(long now) {
        return 0.35F + 0.35F * Mth.sin(now / 400F);
    }

    private static void pips(GuiGraphics graphics, ClientQuests.Entry entry, int x, int y, float pulse, float alpha) {
        for (int i = 0; i < entry.needed(); i++) {
            boolean full = i < entry.done();
            boolean fresh = full && i == entry.done() - 1 && pulse > 0F;
            float grow = fresh ? 1F + pulse * 0.4F : 1F;
            float flash = fresh ? 1F + pulse * 0.6F : 1F;
            int px = x + i * (PIP + PIP_GAP);
            graphics.pose().pushPose();
            graphics.pose().translate(px + PIP / 2F, y + PIP / 2F, 0F);
            graphics.pose().scale(grow, grow, 1F);
            graphics.setColor(flash, flash, flash, alpha);
            graphics.blit(full ? PIP_FULL : PIP_EMPTY, -PIP / 2, -PIP / 2, 0F, 0F, PIP, PIP, PIP, PIP);
            graphics.pose().popPose();
        }
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    private static void shimmer(GuiGraphics graphics, Font font, FormattedCharSequence line, int x, int y, long now,
                                float alpha) {
        int[] at = {x, 0};
        line.accept((index, style, codepoint) -> {
            String ch = Character.toString(codepoint);
            float wave = Math.max(0F, Mth.sin(now / 260F - at[1] * 0.35F));
            int colour = blend(TITLE_COLOUR, SHIMMER_COLOUR, wave * wave * wave * wave);
            graphics.drawString(font, ch, at[0], y, fade(colour, alpha), true);
            at[0] += font.width(ch);
            at[1]++;
            return true;
        });
    }

    private static void dawn(GuiGraphics graphics, Minecraft mc, Font font, int x, int y, float alpha) {
        if (mc.level == null) {
            return;
        }
        int left = Math.floorMod(DAWN - (int) (mc.level.getDayTime() % DAY), DAY);
        int minutes = left / 1200;
        String text = left > DAY / 2 || minutes < 1
                ? Component.translatable("menu.jojoha.quests.dawn_soon").getString()
                : Component.translatable("menu.jojoha.quests.dawn", minutes).getString();
        float bob = Mth.sin(Util.getMillis() / 500F);
        graphics.setColor(1F, 1F, 1F, alpha);
        graphics.blit(MOON, x, y + Math.round(bob * 0.6F) - 1, 0F, 0F, MOON_SIZE, MOON_SIZE, MOON_SIZE, MOON_SIZE);
        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.drawString(font, text, x + MOON_SIZE + 3, y, fade(DAWN_COLOUR, alpha), true);
    }

    private static int fade(int colour, float alpha) {
        return colour & 0xFFFFFF | Math.max(4, Math.round(alpha * 255F)) << 24;
    }

    private static int blend(int from, int to, float amount) {
        if (amount <= 0F) {
            return from;
        }
        return FastColor.ARGB32.lerp(Math.min(1F, amount), from, to);
    }
}
