package org.gumel.jojoha.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.entity.npc.NpcStanding;
import org.gumel.jojoha.network.packet.FactionStandingPacket;
import org.jetbrains.annotations.Nullable;

public final class FactionBanner {
    private static final ResourceLocation HAMON = PlayerMenuScreen.menu("frames/hamon_faction_logo.png");
    private static final ResourceLocation VAMPIRE = PlayerMenuScreen.menu("frames/vampire_faction_logo.png");
    private static final int LOGO_TEX = 32;
    private static final int LOGO = 64;
    private static final int NAME_SCALE = 2;
    private static final int BAR_W = 120;
    private static final int SPARKS = 18;
    private static final long QUEST_GAP_MS = 250L;
    private static final long SLAM_MS = 260L;
    private static final long FLASH_MS = 220L;
    private static final long RING_MS = 520L;
    private static final long SPARK_MS = 800L;
    private static final long TEXT_IN_MS = 300L;
    private static final long COUNT_MS = 800L;
    private static final long HOLD_MS = 4200L;
    private static final long OUT_MS = 450L;
    private static final int HAMON_COLOUR = 0xFFE0B040;
    private static final int VAMPIRE_COLOUR = 0xFFD04060;
    private static final int HEAD_COLOUR = 0xFFD8D8D8;
    private static final int DIM_COLOUR = 0xFF8A8A8A;
    private static final int FRIENDLY = 0xFF70E070;
    private static final int NEUTRAL = 0xFFD8D0B8;
    private static final int HOSTILE = 0xFFE05050;
    private static final int[] BOUNDS = {NpcStanding.LOWEST, NpcStanding.HATED + 1, NpcStanding.HOSTILE + 1,
            NpcStanding.FRIENDLY, NpcStanding.HONOURED, NpcStanding.HIGHEST + 1};

    private record Spark(float angle, float speed, int size) {
    }

    private record Shown(FactionStandingPacket packet, long at, Spark[] sparks, boolean levelled) {
    }

    @Nullable
    private static Shown shown;
    private static boolean struck;

    private FactionBanner() {
    }

    public static void show(FactionStandingPacket packet) {
        long now = Util.getMillis();
        long delay = packet.quest() ? QuestBanner.goneIn() + QUEST_GAP_MS : 0L;
        RandomSource random = RandomSource.create(now);
        Spark[] sparks = new Spark[SPARKS];
        for (int i = 0; i < SPARKS; i++) {
            float angle = (i + random.nextFloat() * 0.6F) / SPARKS * Mth.TWO_PI;
            sparks[i] = new Spark(angle, 40F + random.nextFloat() * 50F, random.nextFloat() < 0.3F ? 3 : 2);
        }
        int before = packet.total() - packet.gain();
        boolean levelled = NpcStanding.Level.of(before) != NpcStanding.Level.of(packet.total());
        shown = new Shown(packet, now + delay, sparks, levelled);
        struck = false;
    }

    private static int colour(NpcStanding.Level level) {
        return switch (level) {
            case HATED, HOSTILE -> HOSTILE;
            case NEUTRAL -> NEUTRAL;
            case FRIENDLY, HONOURED -> FRIENDLY;
        };
    }

    private static boolean vampire(String faction) {
        return NpcStanding.VAMPIRE.equals(faction);
    }

    private static int fade(int colour, float alpha) {
        return colour & 0xFFFFFF | Math.max(4, Math.round(alpha * 255F)) << 24;
    }

    private static Style tint(int colour) {
        return Style.EMPTY.withColor(colour & 0xFFFFFF);
    }

    private static void strike(Shown current) {
        struck = true;
        int gain = current.packet().gain();
        SoundEvent sound = current.levelled() ? SoundEvents.PLAYER_LEVELUP
                : gain < 0 ? SoundEvents.NOTE_BLOCK_BASS.value() : SoundEvents.NOTE_BLOCK_BELL.value();
        float pitch = current.levelled() ? 1.2F : gain < 0 ? 0.6F : 1.0F;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.7F));
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        Shown current = shown;
        if (current == null || mc.options.hideGui) {
            return;
        }
        long age = Util.getMillis() - current.at();
        if (age < 0L) {
            return;
        }
        if (age > HOLD_MS + OUT_MS) {
            shown = null;
            return;
        }
        if (!struck && age >= SLAM_MS) {
            strike(current);
        }
        float fadeOut = 1F - Mth.clamp((age - HOLD_MS) / (float) OUT_MS, 0F, 1F);
        float logoAlpha = Math.min(Mth.clamp(age / (float) (SLAM_MS / 2), 0F, 1F), fadeOut);
        long hit = age - SLAM_MS;

        FactionStandingPacket packet = current.packet();
        Font font = mc.font;
        float counted = Mth.clamp((hit - TEXT_IN_MS) / (float) COUNT_MS, 0F, 1F);
        int before = packet.total() - packet.gain();
        int value = Math.round(Mth.lerp(1F - (1F - counted) * (1F - counted), before, packet.total()));
        NpcStanding.Level level = NpcStanding.Level.of(value);
        int levelColour = colour(level);
        int factionColour = vampire(packet.faction()) ? VAMPIRE_COLOUR : HAMON_COLOUR;

        int centre = graphics.guiWidth() / 2;
        int top = Math.round(graphics.guiHeight() * 0.08F);
        float logoY = top + LOGO / 2F;

        graphics.pose().pushPose();
        graphics.pose().translate(0F, 0F, 300F);

        if (hit >= 0L) {
            ring(graphics, centre, logoY, hit, levelColour, fadeOut, 0L);
            if (current.levelled()) {
                ring(graphics, centre, logoY, hit, levelColour, fadeOut, 160L);
            }
            sparks(graphics, current.sparks(), centre, logoY, hit, current.levelled() ? factionColour : levelColour, fadeOut);
        }

        float slam = Mth.clamp(age / (float) SLAM_MS, 0F, 1F);
        float scale = slam < 1F ? 2.4F - 1.4F * slam * slam : 1F + bounce(hit);
        graphics.pose().pushPose();
        graphics.pose().translate(centre, logoY, 0F);
        graphics.pose().scale(scale, scale, 1F);
        ResourceLocation logo = vampire(packet.faction()) ? VAMPIRE : HAMON;
        graphics.setColor(1F, 1F, 1F, logoAlpha);
        graphics.blit(logo, -LOGO / 2, -LOGO / 2, LOGO, LOGO, 0F, 0F, LOGO_TEX, LOGO_TEX, LOGO_TEX, LOGO_TEX);
        if (hit >= 0L && hit < FLASH_MS) {
            float flash = 1F - hit / (float) FLASH_MS;
            graphics.setColor(20F, 20F, 20F, flash * logoAlpha);
            graphics.blit(logo, -LOGO / 2, -LOGO / 2, LOGO, LOGO, 0F, 0F, LOGO_TEX, LOGO_TEX, LOGO_TEX, LOGO_TEX);
        }
        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.pose().popPose();

        float textIn = Mth.clamp((hit - 80L) / (float) TEXT_IN_MS, 0F, 1F);
        float textAlpha = Math.min(textIn, fadeOut);
        if (textAlpha > 0.02F) {
            int rise = Math.round((1F - textIn) * 6F);
            int y = top + LOGO + 6 + rise;
            Component name = Component.translatable("faction.jojoha." + packet.faction());
            graphics.pose().pushPose();
            graphics.pose().translate(centre, y, 0F);
            graphics.pose().scale(NAME_SCALE, NAME_SCALE, 1F);
            graphics.drawString(font, name, -font.width(name) / 2, 0, fade(factionColour, textAlpha), true);
            graphics.pose().popPose();
            y += 8 * NAME_SCALE + 5;

            Component line = line(packet, level, value, levelColour);
            graphics.drawString(font, line, centre - font.width(line) / 2, y, fade(0xFFFFFFFF, textAlpha), true);
            y += 13;
            bar(graphics, font, centre, y, value, level, textAlpha);
        }
        graphics.pose().popPose();
    }

    private static float bounce(long hit) {
        if (hit < 0L || hit > 300L) {
            return 0F;
        }
        float t = hit / 300F;
        return Mth.sin(t * Mth.PI) * 0.12F * (1F - t);
    }

    private static Component line(FactionStandingPacket packet, NpcStanding.Level level, int value, int levelColour) {
        String number = "(" + (value > 0 ? "+" : "") + value + ")";
        Component rank = Component.translatable(level.key()).withStyle(tint(levelColour));
        if (packet.gain() == 0) {
            return Component.translatable("faction.jojoha.current").withStyle(tint(HEAD_COLOUR))
                    .append(" ").append(rank).append(Component.literal(" " + number).withStyle(tint(levelColour)));
        }
        String gain = (packet.gain() > 0 ? "+" : "") + packet.gain();
        MutableComponent text = Component.translatable(packet.gain() < 0 ? "faction.jojoha.decreased" : "faction.jojoha.increased")
                .withStyle(tint(HEAD_COLOUR));
        return text.append(" ")
                .append(Component.literal(gain).withStyle(tint(packet.gain() < 0 ? HOSTILE : FRIENDLY)).withStyle(ChatFormatting.BOLD))
                .append(Component.literal("  →  ").withStyle(tint(DIM_COLOUR)))
                .append(rank)
                .append(Component.literal(" " + number).withStyle(tint(levelColour)));
    }

    private static void bar(GuiGraphics graphics, Font font, int centre, int y, int value, NpcStanding.Level level, float alpha) {
        int index = level.ordinal();
        int low = BOUNDS[index];
        int high = BOUNDS[index + 1];
        float filled = high - low <= 1 ? 1F : (value - low) / (float) (high - low - 1);
        Component here = Component.translatable(level.key());
        NpcStanding.Level[] all = NpcStanding.Level.values();
        Component next = index + 1 < all.length ? Component.translatable(all[index + 1].key())
                : Component.translatable("faction.jojoha.max");
        int left = centre - BAR_W / 2;
        graphics.drawString(font, here, left - 6 - font.width(here), y - 2, fade(DIM_COLOUR, alpha), true);
        graphics.drawString(font, next, left + BAR_W + 6, y - 2, fade(DIM_COLOUR, alpha), true);
        graphics.setColor(1F, 1F, 1F, alpha);
        QuestIcons.bar(graphics, left, y, BAR_W, filled, level == NpcStanding.Level.HONOURED);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    private static void ring(GuiGraphics graphics, int cx, float cy, long hit, int colour, float alpha, long lag) {
        long t = hit - lag;
        if (t < 0L || t > RING_MS) {
            return;
        }
        float p = t / (float) RING_MS;
        float eased = 1F - (1F - p) * (1F - p);
        int radius = Math.round(LOGO / 2F + eased * 46F);
        float a = (1F - p) * alpha * 0.9F;
        int dots = Math.max(12, Math.round(radius * Mth.TWO_PI / 4F));
        int argb = fade(colour, a);
        for (int i = 0; i < dots; i++) {
            float angle = i / (float) dots * Mth.TWO_PI;
            int x = (cx + Math.round(Mth.cos(angle) * radius)) & ~1;
            int y = Math.round(cy + Mth.sin(angle) * radius) & ~1;
            graphics.fill(x, y, x + 2, y + 2, argb);
        }
    }

    private static void sparks(GuiGraphics graphics, Spark[] sparks, int cx, float cy, long hit, int colour, float alpha) {
        if (hit > SPARK_MS) {
            return;
        }
        float t = hit / 1000F;
        float p = hit / (float) SPARK_MS;
        int argb = fade(colour, (1F - p) * alpha);
        int white = fade(0xFFFFFFFF, (1F - p) * alpha);
        for (Spark spark : sparks) {
            float out = LOGO / 2F - 4F + spark.speed() * t * (1.6F - p);
            int x = (cx + Math.round(Mth.cos(spark.angle()) * out)) & ~1;
            int y = Math.round(cy + Mth.sin(spark.angle()) * out + 30F * t * t) & ~1;
            graphics.fill(x, y, x + spark.size(), y + spark.size(), p < 0.25F ? white : argb);
        }
    }
}
