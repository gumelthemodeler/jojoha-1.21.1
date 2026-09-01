package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;

/**
 * Fades the user's own Stand out when it gets between them and what they are looking at.
 *
 * <p>A Stand fights at arm's length in front of its user, which in first person means a body-sized
 * object filling the middle of the screen at exactly the moment the fight matters most. Thinning it
 * out keeps the fight visible without hiding the Stand, and reads as it being a projection of will
 * rather than a solid thing standing in the way.
 *
 * <p>Driven by distance to the camera rather than by the front stance specifically, because the
 * problem is not the stance - it is proximity. Blocking, barraging and simply hovering close all
 * cause it, and a distance ramp covers every one of them without a special case each.
 */
public final class StandViewAlpha {
    /** Beyond this the Stand is unaffected; below the near mark it sits at the floor. */
    private static final double FADE_START_DISTANCE = 4.5;
    private static final double FADE_FULL_DISTANCE = 1.2;
    /**
     * Never fully invisible - the user still needs to read what their Stand is doing.
     *
     * <p>Low enough to see a fight straight through it, which is the whole reason the fade exists;
     * the silhouette and its aura still carry where the Stand is and what it is swinging at.
     */
    private static final float MIN_ALPHA = 0.14F;

    private StandViewAlpha() {
    }

    /**
     * The Stand's own alpha, thinned if it is crowding the camera.
     *
     * <p>Only ever applies to the viewer's own Stand in first person. Somebody else's Stand is part
     * of the scene and should be solid; in third person the camera is already outside the fight.
     */
    public static float of(StandEntity stand) {
        float base = stand.getRenderAlpha();

        // A bound Stand is never drawn as an entity, by anybody, from any angle.
        //
        // It is drawn onto its user instead - on the player model in third person, and over the
        // hand in first - so the entity render would be a second copy of it standing a few frames
        // behind the real one. See StandBoundArmsLayer.
        //
        // Checked before the viewer is, unlike everything below: whose Stand it is has no bearing on
        // this. The old rule hid it only for its owner and only in first person, which left everyone
        // else looking at a Hermit Purple hovering beside its user rather than growing out of them.
        if (stand.getStandType().form().isBound()) {
            return 0F;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return base;
        }
        if (stand.getOwnerUuid().filter(minecraft.player.getUUID()::equals).isEmpty()) {
            return base;
        }

        double distance = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(stand.position());
        if (distance >= FADE_START_DISTANCE) {
            return base;
        }

        float closeness = (float) Mth.clamp(
                (FADE_START_DISTANCE - distance) / (FADE_START_DISTANCE - FADE_FULL_DISTANCE), 0.0, 1.0);
        return base * Mth.lerp(closeness, 1F, MIN_ALPHA);
    }
}
