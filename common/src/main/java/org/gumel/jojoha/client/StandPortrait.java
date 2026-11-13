package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

public final class StandPortrait {
    public static final Quaternionf ROTATION =
            new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(200));

    private static float playerAnchor(Player player) {
        return player.getBbHeight() / 2F;
    }

    private static float playerScale(Player player, float scale) {
        return scale / player.getScale();
    }

    private static float[] faceForward(LivingEntity entity) {
        float[] saved = {entity.yBodyRot, entity.yBodyRotO, entity.getYRot(), entity.getXRot(),
                entity.yHeadRot, entity.yHeadRotO};

        entity.yBodyRot = 0F;
        entity.yBodyRotO = 0F;
        entity.setYRot(0F);
        entity.setXRot(0F);
        entity.yHeadRot = 0F;
        entity.yHeadRotO = 0F;
        return saved;
    }

    private static float[] facePointer(LivingEntity entity,
                                       float yaw, float pitch) {
        float[] saved = {entity.yBodyRot, entity.yBodyRotO, entity.getYRot(), entity.getXRot(),
                entity.yHeadRot, entity.yHeadRotO};

        entity.yBodyRot = 180F + yaw * 20F;
        entity.yBodyRotO = entity.yBodyRot;
        entity.setYRot(180F + yaw * 40F);
        entity.setXRot(-pitch * 20F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        return saved;
    }

    private static void restore(LivingEntity entity, float[] saved) {
        entity.yBodyRot = saved[0];
        entity.yBodyRotO = saved[1];
        entity.setYRot(saved[2]);
        entity.setXRot(saved[3]);
        entity.yHeadRot = saved[4];
        entity.yHeadRotO = saved[5];
    }

    private static StandEntity preview;

    private static boolean hadStand;

    private static long lastAdvance;

    private static final int MAX_CATCHUP_TICKS = 4;

    private static final int SHOWN_FROM_TICKS = 40;

    private StandPortrait() {
    }

    private static final float BLADE_FILL = 0.8F;
    private static final int BLADE_SHEET = 32;

    public static void forget() {
        hadStand = false;
    }

    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, int x, int y,
                              int width, int height, float scale, float anchorY,
                              Quaternionf rotation) {
        render(guiGraphics, data, x, y, width, height, scale, anchorY, rotation,
                Float.NaN, Float.NaN);
    }

    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, int x, int y,
                              int width, int height, float scale, float anchorY,
                              Quaternionf rotation, float mouseX, float mouseY) {
        render(guiGraphics, data, data.stand.skin(), x, y, width, height, scale, anchorY, rotation, mouseX, mouseY);
    }

    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, int skin, int x, int y,
                              int width, int height, float scale, float anchorY,
                              Quaternionf rotation, float mouseX, float mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !data.stand.isPresent()) {
            return;
        }

        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        if (!type.form().hasBody()) {
            int size = Math.round(Math.min(width, height) * BLADE_FILL);
            guiGraphics.enableScissor(x, y, x + width, y + height);
            guiGraphics.blit(Objects.requireNonNullElse(RemoteSkins.texture(skin), type.textureFor(skin)), x + (width - size) / 2, y + (height - size) / 2, size, size,
                    0F, 0F, BLADE_SHEET, BLADE_SHEET, BLADE_SHEET, BLADE_SHEET);
            guiGraphics.disableScissor();
            return;
        }

        prepare(minecraft, data, skin);

        boolean bound = StandTypes.byIdOrDefault(data.stand.standId()).form().isBound();

        float anchor = bound && minecraft.player != null
                ? playerAnchor(minecraft.player)
                : anchorY;
        float size = bound && minecraft.player != null
                ? playerScale(minecraft.player, scale)
                : scale;

        boolean follows = !Float.isNaN(mouseX);
        float centreX = x + width / 2F;
        float centreY = y + height / 2F;

        float yaw = follows ? (float) Math.atan((centreX - mouseX) / 40F) : 0F;
        float pitch = follows ? (float) Math.atan((centreY - mouseY) / 40F) : 0F;

        Quaternionf facing = rotation;
        Quaternionf tilt = null;
        if (follows) {
            tilt = new Quaternionf().rotateX(pitch * 20F * ((float) Math.PI / 180F));
            facing = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        }

        if (bound && minecraft.player != null) {
            float[] saved = follows
                    ? facePointer(minecraft.player, yaw, pitch)
                    : faceForward(minecraft.player);

            InventoryScreen.renderEntityInInventory(guiGraphics, x + width / 2, y + height / 2,
                    size, new Vector3f(0F, anchor, 0F), facing, tilt, minecraft.player);
            restore(minecraft.player, saved);
        }

        if (follows) {
            facePointer(preview, yaw, pitch);
        } else {
            faceForward(preview);
        }

        InventoryScreen.renderEntityInInventory(guiGraphics, x + width / 2, y + height / 2, size,
                new Vector3f(0F, anchor, 0F), facing, tilt, preview);
    }

    static StandEntity prepare(JojohaPlayerData data, int skin) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !data.stand.isPresent()) {
            return null;
        }
        return prepare(minecraft, data, skin);
    }

    private static StandEntity prepare(Minecraft minecraft, JojohaPlayerData data, int skin) {
        if (preview == null || preview.level() != minecraft.level) {
            preview = new StandEntity(ModRegistries.STAND.get(), minecraft.level);
            preview.setInvisible(false);
            preview.setPreviewMode(true);
            if (hadStand) {
                preview.tickCount = SHOWN_FROM_TICKS;
            }
            lastAdvance = 0L;
        }

        preview.setTrustTier(data.stand.trust());

        if (!hadStand) {
            hadStand = true;
            preview.tickCount = 0;
        }

        preview.setStandType(data.stand.standId());

        preview.setSkin(skin);
        preview.setLook(data.restPose, data.barrageShout);

        advanceClock();
        return preview;
    }

    static float[] hold(LivingEntity entity, float yaw) {
        float[] saved = faceForward(entity);
        entity.yBodyRot = yaw;
        entity.yBodyRotO = yaw;
        entity.setYRot(yaw);
        entity.yHeadRot = yaw;
        entity.yHeadRotO = yaw;
        return saved;
    }

    static void release(LivingEntity entity, float[] saved) {
        restore(entity, saved);
    }

    private static void advanceClock() {
        long now = System.currentTimeMillis();
        if (lastAdvance == 0L) {
            lastAdvance = now;
            return;
        }

        long due = (now - lastAdvance) / 50L;
        if (due <= 0L) {
            return;
        }

        preview.tickCount += (int) Math.min(due, MAX_CATCHUP_TICKS);
        lastAdvance += due * 50L;
    }
}
