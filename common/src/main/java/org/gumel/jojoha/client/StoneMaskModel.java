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

public class StoneMaskModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stone_mask"), "main");

    public static final ResourceLocation TEXTURE_DORMANT = texture("stone_mask");
    public static final ResourceLocation TEXTURE_ACTIVATED = texture("stone_mask_activated");
    public static final ResourceLocation TEXTURE_GLOW = texture("stone_mask_activated_e");

    public static final ResourceLocation TEXTURE_AJA_DORMANT = texture("stone_mask_aja");
    public static final ResourceLocation TEXTURE_AJA_ACTIVATED = texture("stone_mask_aja_activated");
    public static final ResourceLocation TEXTURE_AJA_GLOW = texture("stone_mask_aja_activated_e");

    public static final float CENTRE_Y = -4.0F;
    public static final float CENTRE_Z = -2.0F;

    private final ModelPart head;
    private final ModelPart stone;

    public StoneMaskModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.head = root.getChild("Head");
        this.stone = head.getChild("Stone");
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/mask/" + name + ".png");
    }

    public static ResourceLocation dormant(boolean aja) {
        return aja ? TEXTURE_AJA_DORMANT : TEXTURE_DORMANT;
    }

    public static ResourceLocation activated(boolean aja) {
        return aja ? TEXTURE_AJA_ACTIVATED : TEXTURE_ACTIVATED;
    }

    public static ResourceLocation glow(boolean aja) {
        return aja ? TEXTURE_AJA_GLOW : TEXTURE_GLOW;
    }

    public void setAja(boolean aja) {
        stone.visible = aja;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("Head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.15F))
                        .texOffs(50, 0).addBox(1.0F, -8.0F, 0.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(0.1F))
                        .texOffs(50, 0).mirror().addBox(-4.0F, -8.0F, 0.0F, 3.0F, 8.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                        .texOffs(36, 0).addBox(-1.0F, -9.0F, -5.0F, 2.0F, 4.0F, 5.0F, new CubeDeformation(0.25F))
                        .texOffs(24, 0).addBox(-4.0F, -9.0F, -5.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 9.0F, 4.0F, new CubeDeformation(0.5F))
                        .texOffs(0, 25).addBox(-4.0F, -9.0F, -3.75F, 8.0F, 9.0F, 4.0F, new CubeDeformation(0.7F)),
                PartPose.ZERO);

        head.addOrReplaceChild("Stone", CubeListBuilder.create()
                        .texOffs(40, 12).addBox(-1.0F, -8.0F, -6.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int colour) {
        head.render(poseStack, buffer, packedLight, packedOverlay, colour);
    }
}
