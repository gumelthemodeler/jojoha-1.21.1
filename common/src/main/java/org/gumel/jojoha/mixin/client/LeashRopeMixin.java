package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.ThornRope;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/**
 * Draws Hermit Purple's lasso as a vine rather than as a lead.
 *
 * <p>Lasso of Thorns is built on vanilla's leash, deliberately - being led is a dozen behaviours and
 * vanilla has all of them working. The one thing it does not get for free is how the rope looks, and
 * a thorned Stand tying somebody up with a piece of farm twine rather undercuts the move.
 *
 * <h2>Where this had to go</h2>
 *
 * <p>{@code renderLeash} moved off {@code MobRenderer} and up into {@code EntityRenderer} in 1.21,
 * and it is private - so it is reached by descriptor rather than by name alone. Injecting at its
 * head and cancelling is the whole of the change: vanilla's version never runs, and the vine is
 * drawn in the same place with the same pose.
 *
 * <h2>Only ours</h2>
 *
 * <p>A cow on an ordinary lead must still look like a cow on a lead, so this cannot simply replace
 * every leash in the game. What makes the difference is whether the holder has Hermit Purple out,
 * and the client can tell without being sent anything new: a bound Stand sits exactly on its user,
 * so a Hermit Purple within arm's reach of the holder and owned by them is the answer. Cheap,
 * because it only asks about a player who is holding a leash in the first place.
 */
@Mixin(EntityRenderer.class)
public abstract class LeashRopeMixin {
    /** How thick a slice around the holder to look in for their Stand. */
    private static final double STAND_SEARCH = 1.5;

    /**
     * How much longer the vine is drawn than the gap it spans.
     *
     * <p>Vanilla's leash hangs, and a vine drawn dead straight between two moving things reads as a
     * steel bar. A little more length than the distance gives it somewhere to sag, and because the
     * curve solver works from a length rather than a shape, that is all it needs to be told.
     */
    private static final double SLACK = 0.55;

    @Inject(method = "renderLeash(Lnet/minecraft/world/entity/Entity;F"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$thornLeash(Entity leashed, float partialTick, PoseStack poseStack,
                                   MultiBufferSource buffers, Entity holder, CallbackInfo ci) {
        if (!(holder instanceof Player player) || !jojoha$hasHermitPurple(player)) {
            return;
        }

        // The pose is already at the leashed entity, so everything is measured from there.
        Vec3 from = jojoha$at(leashed, partialTick)
                .add(0, leashed.getBbHeight() * 0.6, 0);

        // Vanilla attaches at the holder's rope-hold point, and so does the grapple - the same
        // place, so a lasso and a grapple leave the same hand.
        Vec3 to = player.getRopeHoldPosition(partialTick);

        Vec3 origin = jojoha$at(leashed, partialTick);
        Vec3 start = from.subtract(origin);
        Vec3 end = to.subtract(origin);

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);

        Vec3 span = end.subtract(start);
        ThornRope.draw(poseStack, buffers, leashed.level(), span, from, span.length() + SLACK,
                org.gumel.jojoha.client.HermitSkins.of(player));

        poseStack.popPose();
        ci.cancel();
    }

    /** Where an entity is drawn this frame. */
    private static Vec3 jojoha$at(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    /**
     * Whether this player's leashes are vines.
     *
     * <p>Asked of the Stand they have, not the Stand they happen to be showing. Tying it to
     * manifestation meant the lasso turned back into farm twine the moment the vines were put away -
     * and the mob is still on the end of it either way, so a rope changing material because the
     * Stand went quiet is the odd reading, not the persistent one.
     *
     * <p>Answered two ways, because the client is only told so much. Its own player's Stand is in
     * the data cache whether summoned or not, so that answer is exact. Anybody else's is not synced
     * at all, and the only visible evidence is their Stand standing there - so for other players it
     * still comes down to Hermit Purple being out. Worth the asymmetry: the exact answer covers the
     * rope the player is holding, and the approximate one covers a rope across the street.
     */
    private static boolean jojoha$hasHermitPurple(Player player) {
        if (player == net.minecraft.client.Minecraft.getInstance().player) {
            var data = org.gumel.jojoha.data.ClientPlayerDataCache.data;
            return data != null && data.stand.isPresent()
                    && StandTypes.HERMIT_PURPLE_ID.equals(data.stand.standId());
        }

        List<StandEntity> stands = player.level().getEntitiesOfClass(StandEntity.class,
                player.getBoundingBox().inflate(STAND_SEARCH),
                stand -> stand.getOwnerUuid().filter(player.getUUID()::equals).isPresent());

        for (StandEntity stand : stands) {
            if (StandTypes.HERMIT_PURPLE_ID.equals(stand.getStandType().id())) {
                return true;
            }
        }
        return false;
    }
}
