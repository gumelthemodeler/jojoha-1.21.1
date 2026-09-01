package org.gumel.jojoha.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.StandEntity;

/**
 * Turns skill-slot key presses into requests, and drives a piloted Stand.
 *
 * <p>Shared by both loaders so the two never drift: each one owns only its key <em>registration</em>
 * and hands the mappings in here.
 */
public final class StandSkillInput {
    /** True while the local player is flying their Stand - read by the input mixin. */
    private static boolean piloting;

    /**
     * The slot keys, kept so the HUD can label each slot with whatever it is actually bound to.
     *
     * <p>Cached from the tick call rather than looked up, because the mappings are created in the
     * loader-specific client classes and common code has no way to reach them - and reading them
     * live means a rebind in the Controls screen shows up immediately.
     */
    private static KeyMapping[] slotKeys = new KeyMapping[0];

    /** Ticks each slot key has been held, for the moves that are charged rather than tapped. */
    private static int[] held = new int[0];
    /** Whether a row has already thrown itself at a full meter, latched until the key comes up. */
    private static boolean[] fired = new boolean[0];

    /** Which rows are running a held move, so the release can be sent exactly once. */
    private static boolean[] sustaining = new boolean[0];

    /**
     * What a slot's key is called, with the modifier shown where one is needed.
     *
     * <p>The rows past the last physical key are the same keys again with the modifier held - see
     * {@link #tick}. The label has to say so, or three of the eight slots on the bar would be
     * labelled identically to three others and there would be no way to tell from the screen which
     * key does what.
     */
    public static Component slotKeyLabel(int row) {
        if (row < 0 || slotKeys.length == 0) {
            return Component.empty();
        }

        boolean modified = row >= slotKeys.length;
        int index = modified ? row - slotKeys.length : row;
        if (index >= slotKeys.length) {
            return Component.empty();
        }

        Component key = slotKeys[index].getTranslatedKeyMessage();

        // Bracketed rather than spelled out. "Alt+Z" is five characters in a badge sized for one,
        // and at the scale these are drawn it turned into a smear; "[Z]" says the same thing in
        // three and keeps the key itself legible, which is the part being looked for.
        return modified ? Component.literal("[").append(key).append("]") : key;
    }

    private StandSkillInput() {
    }

    /**
     * Whether a held key should be winding this move up at the moment.
     *
     * <p>Only the conditions the client can answer for itself and that will not change while the
     * key is down. Energy is deliberately not among them: the pool moves during a hold, and a meter
     * that vanished halfway through because a drain tick crossed a threshold would be worse than
     * one that fills and is then refused - the server still has the final say either way.
     */
    private static boolean canCharge(StandSkill skill) {
        JojohaPlayerData data = ClientPlayerDataCache.data;

        // A Stand move with no Stand out. The bar used to charge and the meter used to appear
        // whether or not anything was summoned to throw it.
        if (skill.requiresStand() && !data.standSummoned) {
            return false;
        }

        if (!skill.isUnlocked(data)) {
            return false;
        }

        // Already running - see StandSkill.isRunning.
        return !skill.isRunning(data);
    }

    /** Drops every hold, for when the bar goes away underneath one. */
    private static void releaseAll() {
        boolean anything = false;
        for (int row = 0; row < held.length; row++) {
            if (held[row] > 0) {
                anything = true;
            }
            held[row] = 0;
            fired[row] = false;
        }

        if (anything) {
            TimeStopCharge.release();
        }
    }

    public static boolean isPiloting() {
        return piloting;
    }

    public static void tick(KeyMapping[] keys) {
        slotKeys = keys;

        // The slot keys only mean anything while the bar they are slots on is up. Left ungated they
        // fired whether or not anything was on screen, so a move could be thrown - time stop
        // included - with no bar, no cooldowns and no indication that a key was even bound to
        // something. Any hold in progress is dropped rather than left latched, or closing the bar
        // mid-charge would strand a key as permanently down.
        if (!CentralBarOverlay.showing()) {
            releaseAll();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();

        // The five keys address the visible page, so which move they mean depends on the page the
        // bar is showing. Resolved here rather than server-side because the page is a purely
        // client-side view state - the server has no idea which one is up.
        int pageOffset = CombatBarOverlay.currentSkillPage() * StandSkills.SLOTS_PER_PAGE;

        if (held.length != StandSkills.SLOTS_PER_PAGE) {
            held = new int[StandSkills.SLOTS_PER_PAGE];
            fired = new boolean[StandSkills.SLOTS_PER_PAGE];
            sustaining = new boolean[StandSkills.SLOTS_PER_PAGE];
        }

        boolean modifierDown = RawKey.modifierDown();

        for (int row = 0; row < StandSkills.SLOTS_PER_PAGE; row++) {
            // Five keys reach eight slots: the first five plainly, the rest by holding the
            // modifier as well. Which is why it is checked in both directions rather than only on
            // the modified rows - without the first half of this test, Alt and Z would fire the
            // sixth slot and the first one together, every time.
            boolean modified = row >= keys.length;
            int index = modified ? row - keys.length : row;
            if (index >= keys.length) {
                continue;
            }

            if (modifierDown != modified) {
                // Gated out this tick. Any charge it had is dropped rather than left standing,
                // because a meter frozen at half by letting go of the modifier is a meter that
                // never resolves and a key that appears stuck down.
                if (held[row] > 0) {
                    held[row] = 0;
                    TimeStopCharge.release();
                }
                continue;
            }

            KeyMapping key = keys[index];
            int slot = pageOffset + row;
            StandSkill skill = StandSkills.skillInSlot(ClientPlayerDataCache.data, slot);
            int chargeMax = skill == null ? 0 : skill.chargeMaxTicks();

            if (skill != null && skill.isSustained()) {
                held[row] = 0;

                // Read once each, into locals. pressed() carries state - it is an edge detector -
                // so calling it inside a condition that can short-circuit is how it quietly stops
                // being fed, and reading isDown twice in a tick invites two different answers.
                boolean pressed = RawKey.pressed(key);
                boolean down = RawKey.isDown(key);
                boolean running = minecraft.player != null && skill.isSustainActive(minecraft.player);

                // Whether the move is going is the world's answer, not ours - see
                // StandSkill.isSustainActive. The latch below is only a one-tick guard against
                // throwing twice while the first one is still in flight to the server, and it is
                // dropped the moment nothing is held and nothing is running, so a start the server
                // refused cannot leave it stuck the wrong way round.
                if (!down && !running) {
                    sustaining[row] = false;
                }

                if (pressed && !running && !sustaining[row] && minecraft.player != null) {
                    NetworkHandler.sendUseStandSkill(slot, 0);
                    sustaining[row] = true;
                } else if (running && !down) {
                    NetworkHandler.sendUseStandSkill(slot, 0);
                    sustaining[row] = false;
                }
                continue;
            }

            if (chargeMax <= 0) {
                held[row] = 0;
                if (RawKey.pressed(key)) {
                    if (minecraft.player != null) {
                        NetworkHandler.sendUseStandSkill(slot, 0);
                    }
                }
                continue;
            }

            // A held move fires on the release, so the press queue is drained rather than acted on -
            // left alone it would fire the move on the way down as well as on the way up.
            if (RawKey.pressed(key)) {
                // discarded on purpose
            }

            if (RawKey.isDown(key)) {
                // Already gone off at the top of the meter. Held keys past that point do nothing
                // until they come up, or the move would fire again on every tick the key stayed
                // down - and the latch is per row, so it cannot leak onto a different move.
                if (fired[row]) {
                    continue;
                }

                // Nothing winds up that cannot be thrown. This is not only about the packet the
                // server would reject anyway - a charge that cannot be spent still puts a meter on
                // screen, shakes the camera and gathers motes around the Stand, all of it promising
                // something that will not happen.
                if (!canCharge(skill)) {
                    if (held[row] > 0) {
                        held[row] = 0;
                        TimeStopCharge.release();
                    }
                    continue;
                }

                held[row] = Math.min(held[row] + 1, chargeMax);
                TimeStopCharge.hold(held[row], chargeMax);

                // Full meter, so it throws itself. Waiting for a release that can only make the
                // move worse is not a decision - the meter being full is the decision, and holding
                // past it was dead time in which the only thing that could happen was being
                // knocked out of a cast that was already paid for.
                if (held[row] >= chargeMax && minecraft.player != null) {
                    NetworkHandler.sendUseStandSkill(slot, held[row]);
                    held[row] = 0;
                    fired[row] = true;
                    TimeStopCharge.release();
                }
            } else {
                fired[row] = false;

                if (held[row] > 0) {
                    if (minecraft.player != null) {
                        NetworkHandler.sendUseStandSkill(slot, held[row]);
                    }
                    held[row] = 0;
                    TimeStopCharge.release();
                }
            }
        }

        tickPilot(minecraft);
    }

    /**
     * Keeps the camera and the steering stream in sync with the server's view of whether piloting
     * is on.
     *
     * <p>Driven off the synced flag rather than off the keypress, so the camera can never end up
     * attached to a Stand the server has already taken away - the Stand running out of energy or
     * being dismissed mid-flight both come back through the same flag and put the view back.
     */
    private static void tickPilot(Minecraft minecraft) {
        boolean shouldPilot = ClientPlayerDataCache.data.standPiloting
                && minecraft.player != null
                && minecraft.level != null;

        if (shouldPilot != piloting) {
            piloting = shouldPilot;
            applyCamera(minecraft, shouldPilot);
        } else if (piloting && minecraft.getCameraEntity() == minecraft.player) {
            // Retried rather than attached once on the edge. The flag can arrive before the Stand
            // it refers to - they are separate streams - and a single attempt that lost that race
            // left the view on the player for the whole flight with nothing to put it right.
            applyCamera(minecraft, true);
        }

        if (!piloting || minecraft.player == null) {
            return;
        }

        // One packet per tick while flying - see PilotPosePacket for why it is a stream rather
        // than edge-triggered.
        var options = minecraft.options;
        float forward = (options.keyUp.isDown() ? 1F : 0F) - (options.keyDown.isDown() ? 1F : 0F);
        float strafe = (options.keyLeft.isDown() ? 1F : 0F) - (options.keyRight.isDown() ? 1F : 0F);
        boolean up = options.keyJump.isDown();
        boolean down = options.keyShift.isDown();

        // This client flies the Stand and then tells the server where it went - it is not asking.
        // See PilotSystem.advance for why the server no longer simulates its own copy.
        StandEntityLookup.localStand(minecraft)
                .filter(entity -> entity instanceof StandEntity)
                .map(entity -> (StandEntity) entity)
                .filter(StandEntity::isPiloted)
                .ifPresent(stand -> {
                    stand.setLocallyPiloted(true);
                    org.gumel.jojoha.stand.skill.PilotSystem.advance(
                            minecraft.player, stand, forward, strafe, up, down);
                    NetworkHandler.sendPilotPose(stand.getX(), stand.getY(), stand.getZ(),
                            stand.getYRot(), stand.getXRot());
                });
    }

    private static void applyCamera(Minecraft minecraft, boolean pilot) {
        if (minecraft.player == null) {
            return;
        }

        if (!pilot) {
            minecraft.setCameraEntity(minecraft.player);

            // Handed back explicitly. The flag is what stops this client applying the server's
            // positions, so leaving it set would strand the Stand on whatever the last predicted
            // position was and it would never follow its owner again.
            StandEntityLookup.localStand(minecraft)
                    .filter(entity -> entity instanceof StandEntity)
                    .map(entity -> (StandEntity) entity)
                    .ifPresent(stand -> {
                        stand.setLocallyPiloted(false);
                        stand.setPilotVelocity(net.minecraft.world.phys.Vec3.ZERO);
                    });
            return;
        }

        StandEntityLookup.localStand(minecraft).ifPresentOrElse(
                minecraft::setCameraEntity,
                () -> minecraft.setCameraEntity(minecraft.player));
    }
}
