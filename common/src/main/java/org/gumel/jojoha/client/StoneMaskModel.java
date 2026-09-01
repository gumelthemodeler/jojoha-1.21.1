package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * The Stone Mask, worn.
 *
 * <p>Seven boxes on one part, exactly as Blockbench exported them. What changed is the frame around
 * them: the export extends {@code EntityModel<T>}, which is the shape for something that <em>is</em>
 * an entity and gets posed from limb swing and head yaw. A mask is neither. It has nothing to
 * animate and no entity of its own, so it is a plain {@link Model} drawn wherever its wearer's head
 * happens to be, and the pose comes from the head it is parented to rather than from anything here.
 *
 * <p>The only edit to the pose is the half turn described below, which the export needed because it
 * had the mask facing inward. Its height is untouched.
 *
 * <p>Two sheets, one mesh. Dormant stone and the thing it becomes are the same object with different
 * colour on it, so the texture is chosen at draw time rather than baked into a second model.
 */
public class StoneMaskModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stone_mask"), "main");

    /**
     * Fed, but not yet woken.
     *
     * <p>The state between the two the mask spends most of its life in once it has been used at all:
     * blood on cold stone, spines still in. Wearing it is what takes it from here to awake.
     */
    public static final ResourceLocation TEXTURE_BLOODIED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/stone_mask_bloodied.png");

    /** Dormant, and awake. The mask does not change shape when it turns, only colour. */
    public static final ResourceLocation TEXTURE_DORMANT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/stone_mask.png");
    public static final ResourceLocation TEXTURE_ACTIVATED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/stone_mask_activated.png");

    /**
     * The glow map: the eyes, and nothing else.
     *
     * <p>Eight opaque pixels on an otherwise empty sheet, which is exactly what an emissive map should
     * be - drawn additively, the transparent remainder contributes nothing and only the eyes add
     * light. This replaces adding the whole activated sheet on top of itself, which lifted the stone
     * along with the eyes because every lit texel was adding something.
     *
     * <p>Derived rather than drawn: the activated sheet is the dormant one with the eyes lit, so the
     * pixels that differ between them are the pixels that glow. The hand-made version it replaced
     * had been left behind by a texture update and lit eight pixels of empty space.
     */
    public static final ResourceLocation TEXTURE_GLOW =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/stone_mask_activated_e.png");

    private final ModelPart head;

    public StoneMaskModel(ModelPart root) {
        // Cutout and, crucially, no culling.
        //
        // Measured: all three sheets are hard alpha - 407 opaque pixels of 4096, none partial - so
        // blending buys nothing and an alpha test is both correct and cheaper. The half that
        // matters is the culling. Nine tenths of the sheet is clear, so the mask is full of gaps,
        // and with backfaces culled you looked through a gap and out the other side into the world
        // instead of at the inside of the mask. Drawing both faces is what closes it.
        super(RenderType::entityCutoutNoCull);
        this.head = root.getChild("Head");
    }

    /**
     * The mesh, exactly as authored.
     *
     * <p>Sixty-four square, which the item model agrees with - {@code stonemask_item.json} declares
     * the same texture size, so the same two sheets serve the mask in the hand and on the face
     * without a second set of art.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("Head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -3.2929F, -2.9113F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.3F))
                        .texOffs(0, 16).addBox(-4.0F, -3.2929F, -0.9113F, 8.0F, 8.0F, 6.0F, new CubeDeformation(0.5F))
                        .texOffs(32, 9).addBox(-2.0F, 2.9571F, 5.8387F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(24, 30).addBox(-4.0F, -3.2929F, -2.9113F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 20).addBox(-1.0F, -4.2929F, 0.0887F, 2.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 30).addBox(-1.0F, -4.2929F, -3.9113F, 2.0F, 4.0F, 10.0F, new CubeDeformation(0.2F))
                        .texOffs(32, 0).addBox(0.75F, -3.7929F, 1.0887F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.15F)),
                // Turned to face outward. The export has the mask looking backwards - what you saw
                // on a player was the inside of it - and a half turn on Y fixes that, but not on
                // its own: the boxes are not centred on the part origin, so rotating about that
                // origin swings them 2.177 further back as well as turning them. The z offset
                // absorbs exactly that, and the mask ends up occupying the same -5 to +5 depth it
                // did before, just the right way round.
                PartPose.offsetAndRotation(0.0F, -3.7071F, 1.0887F, 0.0F, (float) Math.PI, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int colour) {
        head.render(poseStack, buffer, packedLight, packedOverlay, colour);
    }
}
