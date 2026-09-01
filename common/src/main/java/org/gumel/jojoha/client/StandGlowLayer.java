package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib's glow layer, with the missing-file case handled.
 *
 * <p>{@code AutoGlowingTexture} looks the mask up through the resource manager and then calls
 * {@code Optional.get()} on the result without asking whether anything came back. For a Stand that
 * ships a {@code _glowmask} that is fine; for one that does not it throws, and because this runs
 * inside the entity render the whole draw goes with it.
 *
 * <p>Which is exactly what happened to Star Platinum. Hermit Purple has a mask and rendered; every
 * other Stand had its portrait blanked outright the moment the layer was added, and the failure
 * looks nothing like its cause - an empty box does not suggest a texture lookup.
 *
 * <p>So the file is checked first, and a Stand without one simply does not get a glow pass. That
 * keeps the original intent - the presence of the mask is the switch - and makes it true rather
 * than aspirational.
 *
 * <h2>Cached, because this runs every frame</h2>
 *
 * <p>The lookup is a resource manager hit, and it would otherwise happen once per layer per Stand
 * per frame. The answer only changes on a resource reload, and the cache is keyed by texture so a
 * Stand switching skins is a different question with its own answer.
 */
public class StandGlowLayer extends AutoGlowingGeoLayer<StandEntity> {
    private static final java.util.Map<ResourceLocation, Boolean> HAS_MASK =
            new java.util.concurrent.ConcurrentHashMap<>();

    public StandGlowLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel model,
                       RenderType renderType, MultiBufferSource buffers, VertexConsumer consumer,
                       float partialTick, int light, int overlay) {
        if (!hasMask(getRenderer().getTextureLocation(animatable))) {
            return;
        }

        super.render(poseStack, animatable, model, renderType, buffers, consumer,
                partialTick, light, overlay);
    }

    /**
     * Whether a mask sits beside this texture, remembered so the disk is not asked every frame.
     *
     * <p>Shared with the first-person arm pass, which has to ask the same question before it reaches for
     * the emissive render type - see StandFirstPersonArms. The cache is the point of sharing it:
     * two callers asking per frame would otherwise be two resource lookups per frame.
     */
    static boolean hasMask(ResourceLocation texture) {
        return HAS_MASK.computeIfAbsent(texture, path -> {
            String withMask = path.getPath().replace(".png", "_glowmask.png");
            return Minecraft.getInstance().getResourceManager()
                    .getResource(ResourceLocation.fromNamespaceAndPath(path.getNamespace(), withMask))
                    .isPresent();
        });
    }

    /** Dropped on a resource reload, when a pack could have added or removed a mask. */
    public static void forget() {
        HAS_MASK.clear();
    }
}
