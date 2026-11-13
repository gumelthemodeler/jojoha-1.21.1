package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.stand.StandMode;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.anubis.Possession;
import org.gumel.jojoha.stand.skill.StandReinforced;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class CentralBarOverlay {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation FRAME = hud("combat_bar/combat_bar_central.png");
    private static final ResourceLocation PAGE_BAR = hud("combat_bar/central_page_bar.png");
    private static final ResourceLocation TRACK_LEFT = hud("combat_bar/central_spec_bar.png");
    private static final ResourceLocation TRACK_RIGHT = hud("combat_bar/central_hamon_bar.png");
    private static final ResourceLocation FILL_HAMON = hud("combat_bar/central_hamon_bar_filled.png");
    private static final ResourceLocation FILL_VAMPIRISM = hud("combat_bar/central_vampirism_bar_filled.png");
    private static final ResourceLocation FILL_STAND = hud("combat_bar/central_stand_bar_filled.png");
    private static final ResourceLocation STATE_FULL = hud("stand_state/stand_state_full.png");
    private static final ResourceLocation STATE_UNSUMMONED = hud("stand_state/stand_state_unsummoned.png");
    private static final ResourceLocation EYES_ATTACK = hud("stand_state/stand_attack_eyes.png");
    private static final ResourceLocation EYES_DEFENSE = hud("stand_state/stand_defense_eyes.png");
    private static final ResourceLocation EYES_UTILITY = hud("stand_state/stand_utility_eyes.png");

    private static final int FRAME_W = 222, FRAME_H = 61;

    private static final int BOTTOM_MARGIN = 0;

    private static final int SLOT_X = 28, SLOT_Y = 17;
    private static final int SLOT_SIZE = 19, SLOT_PITCH = 21;
    private static final int SLOTS = 8;

    private static final int PAGE_LEFT_X = 21, PAGE_RIGHT_X = 197;
    private static final int PAGE_Y = 17, PAGE_W = 4, PAGE_H = 19;

    private static final int GAUGE_W = 76, GAUGE_H = 6, GAUGE_Y = 7;
    private static final int GAUGE_LEFT_X = 7, GAUGE_RIGHT_X = 139;

    private static final int STATE_X = 91, STATE_Y = 0;
    private static final int STATE_W = 40, STATE_H = 12;
    private static final int EYES_OFFSET = 15, EYES_SIZE = 10;

    private static final int STATE_SUMMONED_Y_OFFSET = 1;

    private static final ResourceLocation SIDE_FRAME = hud("combat_bar/secondary_combat_bar.png");
    private static final ResourceLocation SIDE_FILL_STAND = hud("combat_bar/secondary_stand_bar_filled.png");
    private static final ResourceLocation SIDE_FILL_HAMON = hud("combat_bar/secondary_hamon_bar_filled.png");
    private static final ResourceLocation SIDE_FILL_VAMPIRISM = hud("combat_bar/secondary_vampirism_bar_filled.png");
    private static final ResourceLocation SIDE_FILL_ULTIMATE = hud("combat_bar/secondary_ultimatelifeform_bar_filled.png");
    private static final ResourceLocation SIDE_FILL_ANUBIS = hud("combat_bar/secondary_anubis_bar_filled.png");
    private static final int ULTIMATE_CYCLE_TICKS = 120;
    private static final float ULTIMATE_SATURATION = 0.6F;
    private static final int POSSESSED_PULSE_TICKS = 16;

    private static final int SIDE_W = 188, SIDE_H = 61;

    private static final int SIDE_SLOT_X = 12, SIDE_SLOT_Y = 16, SIDE_COLUMNS = 4;

    private static final int SIDE_PAGE_X = 5, SIDE_PAGE_TOP_Y = 16, SIDE_PAGE_BOTTOM_Y = 37;

    private static final int SIDE_GAUGE_X = 97, SIDE_GAUGE_W = 85;
    private static final int SIDE_GAUGE_STAND_Y = 16, SIDE_GAUGE_DISCIPLINE_Y = 23;

    private static final int SIDE_STATE_X = 16, SIDE_STATE_Y = 0;

    private static final int SIDE_SCREEN_MARGIN = 3;

    private static final int HOTBAR_HALF_W = 91;

    private static final int HOTBAR_CLEARANCE = 52;

    private static final float KEY_SCALE = 0.75F;

    private static final int KEY_COLOUR = 0xFFFFFFFF;

    private static final int SHADE_COOLING = 0xB0101010;
    private static final int SHADE_LOCKED = 0x90201020;

    private static final float COOLING_TINT = 0.34F;
    private static final float[] LOCKED_TINT = {0.40F, 0.30F, 0.44F};

    private static final int HOTBAR_H = 22;

    private static final int STATUS_SETTLE = 8;

    private static final float REVEAL_SECONDS = 0.22F;

    private static float reveal;
    private static long lastFrameNanos;

    private static final float GAUGE_EASE_PER_SECOND = 4.5F;

    private static final float GAUGE_SNAP = 0.4F;

    private static float shownSpec = -1F;
    private static float shownStand = -1F;

    private static float vampireBlend;

    private static final float VAMPIRE_BLEND_SECONDS = 2.2F;

    private CentralBarOverlay() {
    }

    public static int statusLift() {
        if (!central()) {
            return 0;
        }
        return Math.round((FRAME_H + BOTTOM_MARGIN - HOTBAR_H - STATUS_SETTLE) * eased());
    }

    public static boolean coversExperienceBar() {
        return central() && showing();
    }

    private static boolean central() {
        return JojohaClientConfig.centralBarMode();
    }

    private static int frameHeight() {
        return central() ? FRAME_H : SIDE_H;
    }

    public static boolean showing() {
        return reveal > 0.001F;
    }

    private static boolean wanted() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && !minecraft.player.isSpectator()
                && CombatBarOverlay.isVisible()
                && !(minecraft.screen instanceof PlayerMenuScreen);
    }

    private static float eased() {
        return reveal * reveal * (3F - 2F * reveal);
    }

    public static void render(GuiGraphics graphics) {
        advance();

        ActionIndicators.render(graphics, central() ? eased() : 0F, FRAME_W / 2);

        if (!showing()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        JojohaPlayerData data = ClientPlayerDataCache.data;

        boolean central = central();
        int frameH = frameHeight();
        int x = central ? (graphics.guiWidth() - FRAME_W) / 2 : sideX(graphics.guiWidth());
        int y = central ? graphics.guiHeight() - frameH - BOTTOM_MARGIN : sideY(graphics.guiWidth(), graphics.guiHeight());

        graphics.flush();

        int slide = central
                ? Math.round((1F - eased()) * (frameH + BOTTOM_MARGIN))
                : sideSlide(y, frameH, graphics.guiHeight());
        graphics.pose().pushPose();
        graphics.pose().translate(0F, slide, 0F);

        if (!central) {
            renderSide(graphics, data, x, y, minecraft);
            graphics.flush();
            graphics.pose().popPose();
            return;
        }

        graphics.blit(FRAME, x, y, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H);

        graphics.blit(TRACK_LEFT, x + GAUGE_LEFT_X, y + GAUGE_Y, 0, 0,
                GAUGE_W, GAUGE_H, GAUGE_W, GAUGE_H);

        if (data.discipline == Discipline.HAMON && vampireBlend < 1F) {
            fill(graphics, FILL_HAMON, x + GAUGE_LEFT_X, y + GAUGE_Y, shownSpec, 1F - vampireBlend);
        }
        if (vampireBlend > 0F) {
            fill(graphics, FILL_VAMPIRISM, x + GAUGE_LEFT_X, y + GAUGE_Y, shownSpec, vampireBlend);
        }
        gauge(graphics, TRACK_RIGHT, FILL_STAND, x + GAUGE_RIGHT_X, y + GAUGE_Y, shownStand);

        int pageX = CombatBarOverlay.currentSkillPage() == 0 ? PAGE_LEFT_X : PAGE_RIGHT_X;
        graphics.blit(PAGE_BAR, x + pageX, y + PAGE_Y, 0, 0, PAGE_W, PAGE_H, PAGE_W, PAGE_H);

        renderState(graphics, data, x + STATE_X, y + STATE_Y);
        renderSlots(graphics, data, minecraft, i -> x + SLOT_X + i * SLOT_PITCH, i -> y + SLOT_Y);

        graphics.flush();
        graphics.pose().popPose();
    }

    private static int sideX(int screenWidth) {
        int x = JojohaClientConfig.barCorner().x(screenWidth, SIDE_W, SIDE_SCREEN_MARGIN) + JojohaClientConfig.barOffsetX();
        return Mth.clamp(x, 0, Math.max(0, screenWidth - SIDE_W));
    }

    private static int sideY(int screenWidth, int screenHeight) {
        BarCorner corner = JojohaClientConfig.barCorner();
        int y = corner.y(screenHeight, SIDE_H, SIDE_SCREEN_MARGIN);
        if (!corner.top() && overHotbar(sideX(screenWidth), screenWidth)) {
            y -= HOTBAR_CLEARANCE;
        }
        y += JojohaClientConfig.barOffsetY();
        return Mth.clamp(y, 0, Math.max(0, screenHeight - SIDE_H));
    }

    private static boolean overHotbar(int x, int screenWidth) {
        int hotbarLeft = screenWidth / 2 - HOTBAR_HALF_W;
        int hotbarRight = screenWidth / 2 + HOTBAR_HALF_W;
        return x < hotbarRight && x + SIDE_W > hotbarLeft;
    }

    private static int sideSlide(int y, int frameH, int screenHeight) {
        float hidden = 1F - eased();
        return JojohaClientConfig.barCorner().top()
                ? -Math.round(hidden * (y + frameH))
                : Math.round(hidden * (screenHeight - y));
    }

    private static void renderSide(GuiGraphics graphics, JojohaPlayerData data, int x, int y, Minecraft minecraft) {
        graphics.blit(SIDE_FRAME, x, y, 0, 0, SIDE_W, SIDE_H, SIDE_W, SIDE_H);

        sideFill(graphics, SIDE_FILL_STAND, x + SIDE_GAUGE_X, y + SIDE_GAUGE_STAND_Y, shownStand, 1F);
        if (data.discipline == Discipline.HAMON && vampireBlend < 1F) {
            sideFill(graphics, SIDE_FILL_HAMON, x + SIDE_GAUGE_X, y + SIDE_GAUGE_DISCIPLINE_Y, shownSpec, 1F - vampireBlend);
        }
        if (data.discipline == Discipline.ANUBIS && vampireBlend < 1F) {
            sideFill(graphics, SIDE_FILL_ANUBIS, x + SIDE_GAUGE_X, y + SIDE_GAUGE_DISCIPLINE_Y, possession(data),
                    1F - vampireBlend, possessionTint(data, minecraft));
        }
        if (vampireBlend > 0F) {
            if (data.vampireStage == VampireStage.ULTIMATE_LIFEFORM) {
                sideFill(graphics, SIDE_FILL_ULTIMATE, x + SIDE_GAUGE_X, y + SIDE_GAUGE_DISCIPLINE_Y, shownSpec,
                        vampireBlend, ultimateTint(minecraft));
            } else {
                sideFill(graphics, SIDE_FILL_VAMPIRISM, x + SIDE_GAUGE_X, y + SIDE_GAUGE_DISCIPLINE_Y, shownSpec, vampireBlend);
            }
        }

        int pageY = CombatBarOverlay.currentSkillPage() == 0 ? SIDE_PAGE_TOP_Y : SIDE_PAGE_BOTTOM_Y;
        graphics.blit(PAGE_BAR, x + SIDE_PAGE_X, y + pageY, 0, 0, PAGE_W, PAGE_H, PAGE_W, PAGE_H);

        renderState(graphics, data, x + SIDE_STATE_X, y + SIDE_STATE_Y);

        renderSlots(graphics, data, minecraft,
                i -> x + SIDE_SLOT_X + (i % SIDE_COLUMNS) * SLOT_PITCH,
                i -> y + SIDE_SLOT_Y + (i / SIDE_COLUMNS) * SLOT_PITCH);
    }

    private static float possession(JojohaPlayerData data) {
        if (Possession.possessed(data)) {
            return 1F;
        }
        return data.anubisUse / (float) Possession.METER_FULL_TICKS;
    }

    private static int possessionTint(JojohaPlayerData data, Minecraft minecraft) {
        if (!Possession.possessed(data) || minecraft.level == null) {
            return 0xFFFFFF;
        }
        float pulse = (minecraft.level.getGameTime() % POSSESSED_PULSE_TICKS) / (float) POSSESSED_PULSE_TICKS;
        int shade = Math.round(255F * (0.65F + 0.35F * Mth.sin(pulse * Mth.TWO_PI)));
        return (shade << 16) | (shade << 8) | shade;
    }

    private static int ultimateTint(Minecraft minecraft) {
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        return Mth.hsvToRgb((tick % ULTIMATE_CYCLE_TICKS) / (float) ULTIMATE_CYCLE_TICKS, ULTIMATE_SATURATION, 1F)
                & 0xFFFFFF;
    }

    private static void sideFill(GuiGraphics graphics, ResourceLocation fill, int x, int y, float amount, float alpha) {
        sideFill(graphics, fill, x, y, amount, alpha, 0xFFFFFF);
    }

    private static void sideFill(GuiGraphics graphics, ResourceLocation fill, int x, int y, float amount, float alpha,
                                 int rgb) {
        int width = Math.round(SIDE_GAUGE_W * Mth.clamp(amount, 0F, 1F));
        if (width <= 0 || alpha <= 0.004F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F,
                Mth.clamp(alpha, 0F, 1F));
        graphics.blit(fill, x, y, 0, 0, width, GAUGE_H, SIDE_GAUGE_W, GAUGE_H);
        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private static ResourceLocation eyesFor(StandMode mode) {
        return switch (mode) {
            case DEFENSE -> EYES_DEFENSE;
            case UTILITY -> EYES_UTILITY;
            default -> EYES_ATTACK;
        };
    }

    private static void advance() {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0F : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;

        float step = Math.min(delta, 0.1F);

        reveal = Mth.approach(reveal, wanted() ? 1F : 0F, step / REVEAL_SECONDS);

        JojohaPlayerData data = ClientPlayerDataCache.data;
        shownSpec = easeGauge(shownSpec, data.specEnergy / JojohaPlayerData.MAX_SPEC_ENERGY, step);
        shownStand = easeGauge(shownStand, data.standEnergy / data.maxStandEnergy(), step);

        float target = data.vampireStage == VampireStage.NONE ? 0F : 1F;
        vampireBlend = Mth.approach(vampireBlend, target, step / VAMPIRE_BLEND_SECONDS);
    }

    private static float easeGauge(float shown, float target, float step) {
        target = Mth.clamp(target, 0F, 1F);

        if (shown < 0F || Math.abs(target - shown) > GAUGE_SNAP) {
            return target;
        }

        return shown + (target - shown) * (1F - (float) Math.exp(-GAUGE_EASE_PER_SECOND * step));
    }

    private static void fill(GuiGraphics graphics, ResourceLocation fill, int x, int y,
                            float amount, float alpha) {
        int width = Math.round(GAUGE_W * Mth.clamp(amount, 0F, 1F));
        if (width <= 0 || alpha <= 0.004F) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1F, 1F, 1F, Mth.clamp(alpha, 0F, 1F));

        graphics.blit(fill, x, y, 0, 0, width, GAUGE_H, GAUGE_W, GAUGE_H);

        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private static void gauge(GuiGraphics graphics, ResourceLocation track, ResourceLocation fill,
                              int x, int y, float amount) {
        graphics.blit(track, x, y, 0, 0, GAUGE_W, GAUGE_H, GAUGE_W, GAUGE_H);

        int width = Math.round(GAUGE_W * Mth.clamp(amount, 0F, 1F));
        if (width <= 0) {
            return;
        }

        graphics.blit(fill, x, y, 0, 0, width, GAUGE_H, GAUGE_W, GAUGE_H);
    }

    private static void renderState(GuiGraphics graphics, JojohaPlayerData data, int stateX, int stateTop) {
        if (!data.standSummoned) {
            graphics.blit(STATE_UNSUMMONED, stateX, stateTop, 0, 0,
                    STATE_W, STATE_H, STATE_W, STATE_H);
            return;
        }

        int stateY = stateTop + STATE_SUMMONED_Y_OFFSET;
        graphics.blit(STATE_FULL, stateX, stateY, 0, 0, STATE_W, STATE_H, STATE_W, STATE_H);

        graphics.blit(eyesFor(data.standMode), stateX + EYES_OFFSET, stateY, 0, 0,
                EYES_SIZE, EYES_SIZE, EYES_SIZE, EYES_SIZE);
    }

    private static void renderSlots(GuiGraphics graphics, JojohaPlayerData data, Minecraft minecraft,
                                    IntUnaryOperator wellX,
                                    IntUnaryOperator wellY) {
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int pageOffset = CombatBarOverlay.currentSkillPage() * StandSkills.SLOTS_PER_PAGE;

        for (int i = 0; i < SLOTS; i++) {
            int slotX = wellX.applyAsInt(i);
            int slotY = wellY.applyAsInt(i);

            ResourceLocation id = StandSkills.idInSlot(data, pageOffset + i);
            if (id == null) {
                drawKey(graphics, minecraft, StandSkillInput.slotKeyLabel(i), slotX, slotY);
                continue;
            }

            StandSkill skill = StandSkills.byId(id);

            ResourceLocation equippedHere = equippedInSlot(data, pageOffset + i);
            if (StandReinforced.isReinforced(data, equippedHere)) {
                drawReinforcedOutline(graphics, data, slotX, slotY);
            }

            boolean locked = skill != null
                    ? !skill.isUnlocked(data)
                    : !Moves.canEquip(data, id);

            boolean cooling = !locked && SkillCooldownView.cooling(data, id, now, minecraft.level);
            boolean benched = !locked && Moves.benched(data, id) != null;

            ResourceLocation icon = SkillIcons.of(id, !data.standOut());
            if (icon != null) {
                if (locked) {
                    graphics.setColor(LOCKED_TINT[0], LOCKED_TINT[1], LOCKED_TINT[2], 1F);
                } else if (cooling || benched) {
                    graphics.setColor(COOLING_TINT, COOLING_TINT, COOLING_TINT, 1F);
                }

                graphics.blit(icon, slotX, slotY, 0F, 0F,
                        SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

                graphics.setColor(1F, 1F, 1F, 1F);
            } else {
                SkillIcons.drawName(graphics, minecraft.font, id, slotX, slotY, SLOT_SIZE);
                if (locked || cooling || benched) {
                    graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE,
                            locked ? SHADE_LOCKED : SHADE_COOLING);
                }
            }

            if (cooling) {
                drawSweep(graphics, id, slotX, slotY);
                drawCooldown(graphics, minecraft, SkillCooldownView.remainingMillis(id), slotX, slotY);
            }
            noteReady(id, cooling, locked);
            drawReady(graphics, data, id, slotX, slotY);
            drawKey(graphics, minecraft, StandSkillInput.slotKeyLabel(i), slotX, slotY);
        }
    }

    private static ResourceLocation equippedInSlot(JojohaPlayerData data, int slot) {
        return data.equippedSkill(slot);
    }

    private static void drawReinforcedOutline(GuiGraphics graphics, JojohaPlayerData data,
                                              int slotX, int slotY) {
        int colour = 0xFF000000 | (StandShadowForm.auraColourFor(data.stand) & 0xFFFFFF);

        int right = slotX + SLOT_SIZE;
        int bottom = slotY + SLOT_SIZE;
        graphics.fill(slotX, slotY, right, slotY + 1, colour);
        graphics.fill(slotX, bottom - 1, right, bottom, colour);
        graphics.fill(slotX, slotY, slotX + 1, bottom, colour);
        graphics.fill(right - 1, slotY, right, bottom, colour);
    }

    private static final int SWEEP = 0x60FFFFFF;
    private static final long READY_MS = 450L;
    private static final float READY_FLASH = 0.55F;
    private static final float READY_REACH = 3.5F;
    private static final Set<ResourceLocation> WAS_COOLING = new HashSet<>();
    private static final Map<ResourceLocation, Long> READY_AT = new HashMap<>();

    private static void drawSweep(GuiGraphics graphics, ResourceLocation id, int slotX, int slotY) {
        float left = 1F - SkillCooldownView.recovered(id);
        if (left <= 0F) {
            return;
        }
        int top = slotY + Mth.floor(SLOT_SIZE * (1F - left));
        graphics.fill(slotX, top, slotX + SLOT_SIZE, slotY + SLOT_SIZE, SWEEP);
    }

    private static void noteReady(ResourceLocation id, boolean cooling, boolean locked) {
        if (cooling) {
            WAS_COOLING.add(id);
        } else if (WAS_COOLING.remove(id) && !locked) {
            READY_AT.put(id, Util.getMillis());
        }
    }

    private static void drawReady(GuiGraphics graphics, JojohaPlayerData data, ResourceLocation id,
                                  int slotX, int slotY) {
        Long at = READY_AT.get(id);
        if (at == null) {
            return;
        }
        long age = Util.getMillis() - at;
        if (age >= READY_MS) {
            READY_AT.remove(id);
            return;
        }
        float t = age / (float) READY_MS;
        float fade = (1F - t) * (1F - t);
        float ease = 1F - (1F - t) * (1F - t) * (1F - t);
        int aura = data.stand.isPresent()
                ? StandShadowForm.auraColourFor(data.stand) & 0xFFFFFF : 0xFFFFFF;

        graphics.pose().pushPose();
        graphics.pose().translate(0F, 0F, 50F);
        graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE,
                Math.round(0xFF * READY_FLASH * fade) << 24 | 0xFFFFFF);

        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        graphics.pose().scale(1F / scale, 1F / scale, 1F);
        int thick = Math.max(1, Math.round(scale * 0.5F));
        int grow = Math.round(ease * READY_REACH * scale);
        int left = Math.round(slotX * scale) - grow;
        int top = Math.round(slotY * scale) - grow;
        int right = Math.round((slotX + SLOT_SIZE) * scale) + grow;
        int bottom = Math.round((slotY + SLOT_SIZE) * scale) + grow;
        outline(graphics, left, top, right, bottom, thick, Math.round(0xE0 * (1F - t)) << 24 | aura);
        outline(graphics, left - thick, top - thick, right + thick, bottom + thick, thick,
                Math.round(0x50 * (1F - t)) << 24 | aura);
        graphics.pose().popPose();
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int thick, int colour) {
        if ((colour >>> 24) == 0) {
            return;
        }
        graphics.fill(left, top, right, top + thick, colour);
        graphics.fill(left, bottom - thick, right, bottom, colour);
        graphics.fill(left, top + thick, left + thick, bottom - thick, colour);
        graphics.fill(right - thick, top + thick, right, bottom - thick, colour);
    }

    private static void drawCooldown(GuiGraphics graphics, Minecraft minecraft, long remainingMillis,
                                     int slotX, int slotY) {
        if (remainingMillis <= 0L) {
            return;
        }
        Component text = Component.literal(cooldownLabel(remainingMillis));
        int width = minecraft.font.width(text);
        graphics.drawString(minecraft.font, text,
                slotX + (SLOT_SIZE - width + 1) / 2,
                slotY + (SLOT_SIZE - minecraft.font.lineHeight + 2) / 2,
                COOLDOWN_COLOUR, true);
    }

    static String cooldownLabel(long remainingMillis) {
        long tenths = (remainingMillis + 99L) / 100L;
        if (tenths >= 100L) {
            return Long.toString((remainingMillis + 999L) / 1000L);
        }
        return tenths / 10 + "." + tenths % 10;
    }

    private static final int COOLDOWN_COLOUR = 0xFFFFFFFF;

    private static void drawKey(GuiGraphics graphics, Minecraft minecraft, Component key,
                                int slotX, int slotY) {
        if (key == null) {
            return;
        }

        float width = minecraft.font.width(key) * KEY_SCALE;
        float height = minecraft.font.lineHeight * KEY_SCALE;

        graphics.pose().pushPose();
        graphics.pose().scale(KEY_SCALE, KEY_SCALE, 1F);
        graphics.drawString(minecraft.font, key,
                Math.round((slotX + SLOT_SIZE - 1 - width) / KEY_SCALE),
                Math.round((slotY + SLOT_SIZE - 1 - height) / KEY_SCALE),
                KEY_COLOUR, true);
        graphics.pose().popPose();
    }
}
