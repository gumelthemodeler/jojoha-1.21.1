package org.gumel.jojoha.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Plays the Stand Arrow stab animation on a player's vanilla model.
 *
 * <p>Tracked per player UUID rather than on the local player alone, because everyone watching
 * should see the ritual, not just whoever is performing it. The server broadcasts the start (see
 * {@code StandRitualEffectPacket}) and each client runs its own copy of the timeline from there.
 */
public final class PlayerStabAnimation {
    public static final ResourceLocation ANIMATION_FILE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "animations/player_stab.animation.json");

    /** The stab itself. Main hand only - the arrow refuses to be used from the off hand. */
    public static final String STAB_ANIMATION = "stab2";
    /** The drop back to earth once the awakening stops holding the player up. */
    public static final String LAND_ANIMATION = "land";
    /** The recoil as the Stand breaks out, played after the stab and its pause - drives no arrow bone, the arrow is spent by then. */
    public static final String AWAKEN_ANIMATION = "awaken";

    /**
     * Raising the Stone Mask and turning it onto the face.
     *
     * <p>Drives head and both arms, plus a Head2 bone that the player model has no part for. That
     * bone is the mask's own placement and is deliberately unbound here for the same reason the
     * arrow's prop bone is: the mask is a real item in a real hand for the first second and a
     * quarter, and the arm carrying it is what the viewer actually sees. What happens at the end of
     * that bone's travel is handled by taking the item away - see StoneMaskRitual.
     */
    public static final String EQUIP_MASK_ANIMATION = "equip_mask";


    /** Animation bone name -> the vanilla PlayerModel part it drives. */
    private record BoneBinding(String bone, ModelPart part) {
    }

    private record ActiveStab(float startTick, String animationName) {
    }

    private static final Map<String, Optional<BedrockAnimation>> LOADED = new HashMap<>();

    /** Player UUID -> when their stab began and which variant is playing. */
    private static final Map<UUID, ActiveStab> ACTIVE = new HashMap<>();

    private PlayerStabAnimation() {
    }

    /** Called from the S2C handler when the server says a player has begun the ritual. */
    public static void begin(UUID playerId, String animationName, float clientTimeTicks) {
        ACTIVE.put(playerId, new ActiveStab(clientTimeTicks, animationName));
    }

    /** Drops finished entries so the map doesn't accumulate players who've long since moved on. */
    public static void tick(float clientTimeTicks) {
        ACTIVE.entrySet().removeIf(entry -> {
            BedrockAnimation anim = animation(entry.getValue().animationName());
            return anim == null || clientTimeTicks - entry.getValue().startTick() > anim.lengthTicks();
        });
    }

    /** Drops one player's animation immediately, for a sequence that was called off mid-play. */
    public static void stop(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /**
     * Poses the model for a mid-stab player. Returns false if this player isn't stabbing (or the
     * animation is unavailable), in which case the caller leaves vanilla's own pose alone.
     *
     * <p>The animated parts are reset to their rest pose first: Blockbench keyframes are offsets
     * from rest, exactly like vanilla's own {@code AnimationChannel.Targets}, so applying them on
     * top of the walk/idle pose vanilla just computed would compound the two and read as a limb
     * spasm rather than a clean stab.
     */
    public static boolean apply(PlayerModel<?> model, Player player, float clientTimeTicks) {
        ActiveStab active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return false;
        }

        BedrockAnimation anim = animation(active.animationName());
        if (anim == null) {
            return false;
        }

        float elapsedTicks = clientTimeTicks - active.startTick();
        if (elapsedTicks < 0F || elapsedTicks > anim.lengthTicks()) {
            return false;
        }

        float seconds = elapsedTicks / 20F;

        for (BoneBinding binding : bindings(model)) {
            if (!anim.drives(binding.bone())) {
                continue;
            }

            binding.part().resetPose();

            Vector3f rotation = anim.sample(binding.bone(), BedrockAnimation.Channel.ROTATION, seconds);
            if (rotation != null) {
                binding.part().offsetRotation(rotation);
            }

            Vector3f position = anim.sample(binding.bone(), BedrockAnimation.Channel.POSITION, seconds);
            if (position != null) {
                binding.part().offsetPos(position);
            }
        }

        copyOverlays(model);
        return true;
    }

    /**
     * Puts every part this animation touches back to its rest pose.
     *
     * <p>Needed because {@code offsetPos} writes to a part's x/y/z, and vanilla's {@code setupAnim}
     * only reliably recomputes <em>rotations</em> each frame - so a leftover position offset
     * survives the animation and warps the model afterwards. Worse, one {@code PlayerModel}
     * instance renders every player, so a stale offset from the person who used the arrow would
     * smear onto everyone else too. Clearing before vanilla poses the model means it always builds
     * from a clean rest pose.
     */
    public static void resetParts(PlayerModel<?> model) {
        for (BoneBinding binding : bindings(model)) {
            binding.part().resetPose();
        }
        copyOverlays(model);
    }

    // "standArrow" in the source file is the arrow prop held during the ritual; the vanilla player
    // model has no such part, so it's deliberately unbound - the real item in hand reads as the
    // arrow already.
    private static BoneBinding[] bindings(PlayerModel<?> model) {
        return new BoneBinding[] {
                new BoneBinding("torso", model.body),
                new BoneBinding("head", model.head),
                new BoneBinding("right_arm", model.rightArm),
                new BoneBinding("left_arm", model.leftArm),
                new BoneBinding("right_leg", model.rightLeg),
                new BoneBinding("left_leg", model.leftLeg),
        };
    }

    /** Keeps the cosmetic overlay layers glued to the body parts they shadow. */
    private static void copyOverlays(PlayerModel<?> model) {
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private static BedrockAnimation animation(String name) {
        return LOADED.computeIfAbsent(name, n -> BedrockAnimation.load(ANIMATION_FILE, n)).orElse(null);
    }
}
