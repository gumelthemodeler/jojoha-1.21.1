package org.gumel.jojoha.client.pillar;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class PillarModels {
    public static final ResourceLocation LIGHT_BLADE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/pillar/light_blade.png");
    public static final ResourceLocation RIB_SPIKE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/pillar/rib_spike.png");

    private static ModelPart rightBlade;
    private static ModelPart leftBlade;
    private static ModelPart ribSpike;

    private PillarModels() {
    }

    public static ModelPart rightBlade() {
        if (rightBlade == null) {
            bakeBlades();
        }
        return rightBlade;
    }

    public static ModelPart leftBlade() {
        if (leftBlade == null) {
            bakeBlades();
        }
        return leftBlade;
    }

    public static ModelPart ribSpike() {
        if (ribSpike == null) {
            ribSpike = ribSpikeLayer().bakeRoot().getChild("bone");
        }
        return ribSpike;
    }

    private static void bakeBlades() {
        ModelPart root = lightBladeLayer().bakeRoot();
        rightBlade = root.getChild("right_arm");
        leftBlade = root.getChild("left_arm");
    }

    private static LayerDefinition lightBladeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(6, 0)
                .addBox(-1.0F, -4.0F, 2.0F, 0.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(6, 0)
                .addBox(1.0F, -4.0F, 2.0F, 0.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static LayerDefinition ribSpikeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6.0F, 0.0F, -11.5F, 9.0F, 0.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.ZERO);
        bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(14, 17)
                        .addBox(-1.0F, 0.0F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, -0.5F, -6.0F, 0.0F, 0.0436F, 0.0F));
        bone.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, 0.0F, -2.0F, 0.0F, 0.4363F, 0.0F));
        bone.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 11)
                        .addBox(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.4363F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
