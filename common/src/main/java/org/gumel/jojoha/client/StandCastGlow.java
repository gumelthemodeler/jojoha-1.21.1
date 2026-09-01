package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;

/**
 * How white the Stand is burning as a time stop is wound up.
 *
 * <p>Drawn through the white overlay - the same channel that turns a player white as they take a
 * Stand, and the same one vanilla uses for a creeper about to go off. It replaces a set of additive
 * passes over the model, which lit it without ever making it white: additive light adds to what is
 * already there, so a dark texture stayed dark and only the pale parts of the Stand brightened. The
 * overlay drives every texel toward white by the same amount, which is what makes it read as the
 * thing itself changing rather than as a lamp being pointed at it.
 *
 * <p>A ramp, not a switch. It starts at nothing, follows the charge up while the key is held, and
 * only reaches full white when the move is actually cast - so how bright the Stand is burning is how
 * close the stop is to being thrown.
 */
public final class StandCastGlow {
    /** How white the Stand gets at the end of a full hold, before the cast takes it the rest. */
    private static final float CHARGE_PEAK = 0.82F;

    /** How quickly the white arrives and lets go, per tick. */
    private static final float RISE = 0.12F;
    private static final float FALL = 0.055F;

    private static float white;

    private StandCastGlow() {
    }

    /** Call once per client tick. */
    public static void tick(boolean casting) {
        // The dispel window is counted as cast because it bridges a real gap: the key comes up on
        // the client and the server's casting flag arrives a tick or two later, and without it the
        // white would sag at exactly the moment the move is meant to peak.
        float wanted = casting || TimeStopCharge.dispelling()
                ? 1F
                : TimeStopCharge.charge() * CHARGE_PEAK;

        white = Mth.approach(white, wanted, wanted > white ? RISE : FALL);
    }

    public static void clear() {
        white = 0F;
    }

    /** How white this Stand should be drawn, 0 to 1. Only the caster's own Stand burns. */
    public static float whiteFor(StandEntity stand) {
        if (white <= 0.001F) {
            return 0F;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && stand.getOwner() != null
                && stand.getOwner().getUUID().equals(minecraft.player.getUUID())
                ? white
                : 0F;
    }
}
