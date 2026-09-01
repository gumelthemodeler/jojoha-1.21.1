package org.gumel.jojoha.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * The vine and the hook, straight out of Blockbench.
 *
 * <p>Plain vanilla {@code ModelPart} rather than GeckoLib, and deliberately. Both were authored in
 * the modded-entity format with no animations in them at all - a hook is a shape that gets pointed
 * somewhere and a vine is a shape that gets stretched, and neither needs a keyframe. Converting them
 * to geometry JSON to feed an animation system they make no use of would be a translation step with
 * nothing on the other side of it, and every translation step is somewhere the geometry can quietly
 * come out wrong.
 *
 * <p>So these are the exported definitions, transcribed. What Blockbench produced is what renders.
 */
public final class HermitGrappleModels {
    public static final ModelLayerLocation ROPE = layer("hermit_grapple");
    public static final ModelLayerLocation HOOK = layer("hermit_grapple_hook");

    /** How tall one vine segment is in model units - the length a segment covers before repeating. */
    public static final float SEGMENT_HEIGHT = 10F;

    private HermitGrappleModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, name), "main");
    }

    /**
     * One length of vine, hanging downward from its pivot.
     *
     * <p>Re-pivoted from the export. Blockbench put the origin at the foot of the segment with the
     * cube running up out of it, which is right for a plant growing off the ground and wrong for a
     * rope, where every segment needs to start where the last one ended. Moving the pivot to the top
     * means a segment can be dropped at a point, turned to face somewhere, and it hangs from there.
     */
    public static LayerDefinition createRopeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition main = root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12)
                        .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 10.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 0)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);

        // The crossed plane, so the vine has something to look like from every angle rather than
        // disappearing edge-on. Quarter turn about Y, which is what the export had.
        main.addOrReplaceChild("cross", CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-3.0F, -5.0F, 0.0F, 6.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    /** The barbed end, pivoted where the rope meets it. */
    public static LayerDefinition createHookLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 14)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.3F))
                        .texOffs(0, 23)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(1, -8)
                        .addBox(0.0F, -5.0F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
