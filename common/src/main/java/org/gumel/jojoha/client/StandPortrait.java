package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The one Stand model the interface draws, wherever it is drawn.
 *
 * <p>The combat bar's portrait and the player menu both want a live render of the player's own
 * Stand, and both used to be answered by whichever of them owned a preview entity. This owns it
 * instead - once - because a second one would be actively harmful rather than merely wasteful.
 *
 * <h2>Why one, and why the clock is counted like this</h2>
 *
 * <p>GeckoLib keys its animation manager off the animatable's instance id, but the <em>model</em>
 * keeps its own {@code animTime} bookkeeping in a single field shared by every animatable that
 * renderer ever touches - including the real Stand standing in the world. So the preview's clock is
 * not private, and what is done to it is done to the Stand in the level.
 *
 * <p>Which is why it is a small counter ticked once per rendered frame, and not milliseconds. A
 * tickCount in the billions made that shared clock jump by roughly a billion ticks whenever the
 * world Stand and a preview alternated within one frame, and the degenerate bone transforms that
 * produced were silently discarded by the GPU - indistinguishable from nothing having been drawn.
 */
public final class StandPortrait {
    /**
     * How the model has to be turned to face the viewer.
     *
     * <p>Two rotations, and leaving either out is visible. The Z flip is the usual one every
     * inventory render needs, because this space has Y pointing the other way. The Y turn is the
     * one that is easy to miss: the model's own forward axis points <em>away</em> from the camera
     * here, so without it the Stand is drawn from behind - which is exactly what happened when the
     * player menu declared its own rotation and copied only the flip.
     *
     * <p>Two hundred degrees rather than a hundred and eighty, for a slight three-quarter angle
     * instead of standing dead-on. It shows depth, and a Stand drawn perfectly square to the camera
     * reads as a sprite rather than as a model.
     *
     * <p>It was briefly a hundred and eighty, on the reasoning that a pair of arms needs to be seen
     * symmetrically. That was solving the wrong problem: the previews were not turning with the
     * player because of the angle, they were turning because nothing was fixing the entity's own
     * facing - see faceForward. With that fixed the three-quarter angle works for both, and Star
     * Platinum keeps the view it was drawn for.
     */
    public static final Quaternionf ROTATION =
            new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(200));

    /**
     * How the player is framed, which for a bound Stand is how everything is framed.
     *
     * <p>These are the menu's own player display, read off it rather than matched to it by eye. It
     * anchors on half the player's bounding box and divides the scale by the player's own, so a
     * player who is taller or shorter than usual is still framed correctly - and the two constants
     * the Stand display uses instead, being fixed, are not.
     *
     * <p>Guessing at the gap is what the previous two attempts did, with an anchor lift and then a
     * separate drop for the arms, both tuned toward a target that was moving. Taking the same
     * numbers the player display takes means there is no gap to close.
     */
    private static float playerAnchor(net.minecraft.world.entity.player.Player player) {
        return player.getBbHeight() / 2F;
    }

    private static float playerScale(net.minecraft.world.entity.player.Player player, float scale) {
        return scale / player.getScale();
    }

    /**
     * Faces an entity squarely at the viewer for one draw, and puts it back afterwards.
     *
     * <p>Necessary because {@code renderEntityInInventory} does not touch rotation at all - it
     * translates, scales and applies the quaternion, and leaves the entity pointing wherever it was
     * already pointing. So a preview inherits whichever way the model happened to be turned, and the
     * player drawn under a bound Stand inherits which way the actual player is facing. Turn on the
     * spot and the portrait turns with you, which is the whole complaint.
     *
     * <p>Restoring matters more than setting: this is handed the real player, and a portrait that
     * left them rotated would be a menu that spins the character it is describing.
     */
    private static float[] faceForward(net.minecraft.world.entity.LivingEntity entity) {
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

    /**
     * Turns an entity toward the cursor, the way the menu's player display does.
     *
     * <p>The body turns at half the rate of the head, which is what stops it reading as a mannequin
     * on a turntable - a person tracking something with their eyes moves their head first and their
     * shoulders after.
     */
    private static float[] facePointer(net.minecraft.world.entity.LivingEntity entity,
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

    private static void restore(net.minecraft.world.entity.LivingEntity entity, float[] saved) {
        entity.yBodyRot = saved[0];
        entity.yBodyRotO = saved[1];
        entity.setYRot(saved[2]);
        entity.setXRot(saved[3]);
        entity.yHeadRot = saved[4];
        entity.yHeadRotO = saved[5];
    }

    private static StandEntity preview;

    /** Whether a Stand was drawn last frame, so obtaining one replays the materialise. */
    private static boolean hadStand;

    /** When the clock was last moved on, so it advances by elapsed time rather than by call. */
    private static long lastAdvance;

    /**
     * The most ticks one frame may add.
     *
     * <p>A pause, a stall or a dragged window can leave any amount of wall clock behind, and letting
     * all of it through would fast-forward the animation through the gap. Four is enough to smooth
     * an ordinary hitch and far too few to skip anything.
     */
    private static final int MAX_CATCHUP_TICKS = 4;

    private StandPortrait() {
    }

    /**
     * Called when the player has no Stand, so the next one to arrive fades in.
     *
     * <p>Kept separate from the render because the render is not called at all when there is
     * nothing to draw, and a flag that is only ever cleared by the thing it gates would never be.
     */
    public static void forget() {
        hadStand = false;
    }

    /**
     * Draws the Stand into a box, clipped to it.
     *
     * <p>Deliberately does no clipping of its own. {@code enableScissor} takes raw screen
     * coordinates and ignores the pose entirely, so a caller drawing under a scaled or translated
     * pose - the player menu does both - has to work out the rectangle itself. Clipping here with
     * the same numbers used to position the model would have silently cut the wrong region.
     *
     * @param scale   how large the model is drawn; roughly 0.85 of the box's height reads as a bust
     * @param anchorY where on the model the box is centred - higher looks further up the body
     */
    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, int x, int y,
                              int width, int height, float scale, float anchorY,
                              Quaternionf rotation) {
        render(guiGraphics, data, x, y, width, height, scale, anchorY, rotation,
                Float.NaN, Float.NaN);
    }

    /**
     * The same, turning to follow the cursor.
     *
     * <p>Not a second way of drawing a Stand - the same one, with the fixed angle replaced by a live
     * one. The maths is lifted from the menu's player display rather than reinvented, because the
     * two sit in the same box and swap places: a Stand that turned at a different rate, or leaned a
     * different way, would make flipping between the two pages feel like two different screens.
     *
     * <p>The convention comes with it, and it is worth naming because it is not the fixed path's.
     * Following the mouse puts the half-turn into the <em>entity</em> - body at 180 plus the yaw,
     * head with it - and leaves the quaternion as a plain Z flip. The fixed path does the opposite,
     * zeroing the entity and putting the whole turn in the quaternion. Mixing them gives a model
     * facing backwards, which is what the two hundred degrees in ROTATION is compensating for.
     *
     * <p>A NaN for the cursor means the fixed path, so one method serves both.
     */
    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, int x, int y,
                              int width, int height, float scale, float anchorY,
                              Quaternionf rotation, float mouseX, float mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !data.stand.isPresent()) {
            return;
        }

        if (preview == null) {
            // Forced visible: StandEntity starts invisible waiting on a spawn trigger that never
            // comes for something which was never added to a level.
            preview = new StandEntity(ModRegistries.STAND.get(), minecraft.level);
            preview.setInvisible(false);
            preview.setPreviewMode(true);
        }

        // The portrait mirrors the Trust Tier for free: StandModel already hides every non-arm bone
        // at PARTIAL and StandEntity already flickers at EMERGING, both keyed off exactly this.
        preview.setTrustTier(data.stand.trust());

        if (!hadStand) {
            hadStand = true;
            preview.tickCount = 0;
        }

        preview.setStandType(data.stand.standId());

        // And the look, which is not implied by the type - a Stand's skin lives on the player's
        // record, so a preview told only which Stand it is comes out wearing the default one and
        // quietly disagrees with the Stand standing in the world.
        preview.setSkin(data.stand.skin());

        advanceClock();

        // A bound Stand is drawn on top of its user, so the portrait shows the user first.
        //
        // Without this the showcase for Hermit Purple is a pair of arms hanging in a box with
        // nothing between them, which reads as a broken model rather than as a Stand that only has
        // arms. The player behind them is not decoration - it is what makes the shape legible.
        //
        // Both go in with the same position, scale and rotation, which is what keeps them aligned:
        // renderEntityInInventory overrides an entity's own facing from the quaternion it is handed,
        // so two entities given the same one cannot disagree about which way they are pointing.
        boolean bound = StandTypes.byIdOrDefault(data.stand.standId()).form().isBound();

        // One angle for everything, and it is whatever the caller asked for. A bound Stand used to
        // be forced square to the camera on the theory that both arms had to be visible at once;
        // that was fixing the wrong thing, and it cost every other Stand the view it was drawn for.
        //
        // A bound Stand borrows the player's framing outright rather than the display's own. Both
        // models put their origin at the feet, so the only way the arms can sit correctly on the
        // body is for both to be drawn at one anchor and one scale - and the anchor that is right
        // for a player is the one the menu already uses on them. See playerAnchor.
        float anchor = bound && minecraft.player != null
                ? playerAnchor(minecraft.player)
                : anchorY;
        float size = bound && minecraft.player != null
                ? playerScale(minecraft.player, scale)
                : scale;

        // Where the cursor has put it, if it is following one at all.
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

        // Posed before drawing, which is the part that stopped the preview following the player
        // about the world. Where it points now is the cursor's business, not the entity's.
        //
        // Same anchor, same scale, same angle as the body underneath - which is the whole of what
        // "linked" means here. Any of the three applied to one and not the other would be the arms
        // drifting off the shoulders again, and that includes the turn.
        if (follows) {
            facePointer(preview, yaw, pitch);
        } else {
            faceForward(preview);
        }

        InventoryScreen.renderEntityInInventory(guiGraphics, x + width / 2, y + height / 2, size,
                new Vector3f(0F, anchor, 0F), facing, tilt, preview);
    }

    /**
     * Moves the animation on by however much real time has passed, not by one per draw.
     *
     * <p>Counting draws was wrong twice over. It ran at the frame rate, so the same animation played
     * three times too fast at sixty frames a second and faster still above that - and once the
     * player menu began drawing the Stand twice in one frame, once for the page and once for the
     * display, it doubled again. Elapsed time is the same however many times it is asked.
     */
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
