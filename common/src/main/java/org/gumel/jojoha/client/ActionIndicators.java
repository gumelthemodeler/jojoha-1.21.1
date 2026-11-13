package org.gumel.jojoha.client;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.MovementCharges;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class ActionIndicators {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation CHARGES = hud("combat_bar/leap_dash_indicator.png");
    private static final ResourceLocation CHARGE_SPENT = hud("combat_bar/leap_dash_indicator_cooldown.png");
    private static final ResourceLocation PARRY_FULL = hud("parry/parry_indicator_full.png");
    private static final ResourceLocation PARRY_EMPTY = hud("parry/parry_indicator_empty.png");
    private static final ResourceLocation PARRY_PERFECT = hud("parry/parry_indicator_perfect.png");

    private static final int CHARGES_W = 31, CHARGES_H = 11;
    private static final int CHEVRON = 11;

    private static final int[] LEAP_AT = {0, 6};
    private static final int[] DASH_AT = {13, 19};

    private static final int PARRY_SIZE = 16;
    private static final int PARRY_ART_LEFT = 3, PARRY_ART_TOP = 2, PARRY_ART_H = 11;

    private static final int HOTBAR_HALF_W = 91, HOTBAR_H = 22;
    private static final int GAP = 4;

    private static final int OFFHAND_W = 29;
    private static final int ATTACK_INDICATOR_W = 24;

    private static final ResourceLocation[] MOVEMENT_NODES = {
            id("stand_dash"), id("stand_leap"), id("dodge"), id("burst")};

    private ActionIndicators() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    static void render(GuiGraphics graphics, float centralOut, int centralHalfW) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null
                || minecraft.player.isSpectator()) {
            return;
        }
        JojohaPlayerData data = ClientPlayerDataCache.data;
        boolean charges = false;
        for (ResourceLocation node : MOVEMENT_NODES) {
            charges |= data.hasNode(node);
        }
        boolean parry = true;

        int middle = graphics.guiWidth() / 2;
        int hotbarTop = graphics.guiHeight() - HOTBAR_H;

        boolean offhand = !minecraft.player.getOffhandItem().isEmpty();
        boolean offhandLeft = minecraft.player.getMainArm() == HumanoidArm.RIGHT;
        boolean attackRight = minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR;
        int left = HOTBAR_HALF_W + (offhand && offhandLeft ? OFFHAND_W : 0)
                + (attackRight && !offhandLeft ? ATTACK_INDICATOR_W : 0);
        int right = HOTBAR_HALF_W + (offhand && !offhandLeft ? OFFHAND_W : 0)
                + (attackRight && offhandLeft ? ATTACK_INDICATOR_W : 0);
        left = Math.max(left, Math.round(left + (centralHalfW - left) * centralOut));
        right = Math.max(right, Math.round(right + (centralHalfW - right) * centralOut));

        long now = minecraft.level.getGameTime();
        if (charges) {
            int x = middle - left - GAP - CHARGES_W;
            int y = hotbarTop + (HOTBAR_H - CHARGES_H) / 2;
            graphics.blit(CHARGES, x, y, 0, 0, CHARGES_W, CHARGES_H, CHARGES_W, CHARGES_H);
            spent(graphics, data, MovementCharges.Kind.LEAP, LEAP_AT, x, y, now);
            spent(graphics, data, MovementCharges.Kind.DASH, DASH_AT, x, y, now);
        }
        if (parry) {
            int x = middle + right + GAP - PARRY_ART_LEFT;
            int y = hotbarTop + (HOTBAR_H - PARRY_ART_H) / 2 - PARRY_ART_TOP;
            drawParry(graphics, data, x, y, now, minecraft);
        }
    }

    private static void spent(GuiGraphics graphics, JojohaPlayerData data, MovementCharges.Kind kind, int[] at,
                              int x, int y, long now) {
        int ready = 0;
        for (int i = 0; i < MovementCharges.CHARGES; i++) {
            if (!SkillCooldownView.cooling(data, MovementCharges.slot(kind, i), now, Minecraft.getInstance().level)) {
                ready++;
            }
        }
        for (int i = ready; i < at.length; i++) {
            graphics.blit(CHARGE_SPENT, x + at[i], y, 0, 0, CHEVRON, CHEVRON, CHEVRON, CHEVRON);
        }
    }

    private static void drawParry(GuiGraphics graphics, JojohaPlayerData data, int x, int y, long now,
                                  Minecraft minecraft) {
        if (SkillCooldownView.cooling(data, Defence.PARRY_PERFECT, now, minecraft.level)) {
            graphics.blit(PARRY_PERFECT, x, y, 0, 0, PARRY_SIZE, PARRY_SIZE, PARRY_SIZE, PARRY_SIZE);
            return;
        }
        if (!SkillCooldownView.cooling(data, Defence.PARRY_CHARGE, now, minecraft.level)) {
            graphics.blit(PARRY_FULL, x, y, 0, 0, PARRY_SIZE, PARRY_SIZE, PARRY_SIZE, PARRY_SIZE);
            return;
        }
        graphics.blit(PARRY_EMPTY, x, y, 0, 0, PARRY_SIZE, PARRY_SIZE, PARRY_SIZE, PARRY_SIZE);
        int filled = Math.round(PARRY_ART_H * SkillCooldownView.recovered(Defence.PARRY_CHARGE));
        if (filled > 0) {
            int top = PARRY_ART_TOP + PARRY_ART_H - filled;
            graphics.blit(PARRY_FULL, x, y + top, 0, top, PARRY_SIZE, filled, PARRY_SIZE, PARRY_SIZE);
        }
    }
}
