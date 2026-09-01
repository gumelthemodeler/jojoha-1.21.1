package org.gumel.jojoha.stand.skill;

import net.minecraft.world.entity.Entity;

/**
 * Holds a frozen entity's interpolation still as well as its position.
 *
 * <h2>Why a cancelled tick is not enough</h2>
 *
 * <p>Freezing a projectile is done by cancelling its tick outright, which stops it moving. It does
 * not stop it being <em>drawn</em> moving, and those are different problems.
 *
 * <p>Every entity carries a second copy of where it was and which way it faced last tick -
 * {@code xOld}, {@code yRotO} and the rest - and the renderer draws it somewhere between the two,
 * sliding from the old values to the current ones as the partial tick runs from nought to one. That
 * is what makes movement smooth at frame rates above twenty.
 *
 * <p>A normal tick copies the current values into the old ones before doing anything else, so the
 * pair only ever differ by one tick's worth of travel. Cancel the tick and that copy never happens:
 * whatever gap existed at the instant of freezing is preserved forever, and the renderer keeps
 * sweeping across it twenty times a second. The arrow does not move an inch and never stops
 * twitching - it is being drawn, over and over, travelling the last step it took before time
 * stopped.
 *
 * <p>An arrow shows it worst because it is drawn pointing along its own pitch and yaw, so the gap
 * appears as the whole shaft waving rather than as a body shifting slightly.
 *
 * <p>So this does the one thing the cancelled tick would have done and nothing else: it collapses
 * the old values onto the current ones. With nothing between them there is nothing to interpolate,
 * and a held projectile points flatly wherever it was pointing when the world stopped.
 */
public final class FrozenMotion {
    private FrozenMotion() {
    }

    /**
     * Collapses an entity's previous position and facing onto its current ones.
     *
     * <p>Called from both sides. The server's copy matters for anything that reads the old values
     * back; the client's is the one actually being drawn, and is where the twitch was visible.
     */
    public static void hold(Entity entity) {
        entity.xOld = entity.getX();
        entity.yOld = entity.getY();
        entity.zOld = entity.getZ();

        // xo/yo/zo are a separate pair from xOld/yOld/zOld and are the ones most render paths
        // actually read. Both are set, because which of them a given renderer uses is not something
        // to guess at per entity type.
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();

        entity.xRotO = entity.getXRot();
        entity.yRotO = entity.getYRot();
    }
}
