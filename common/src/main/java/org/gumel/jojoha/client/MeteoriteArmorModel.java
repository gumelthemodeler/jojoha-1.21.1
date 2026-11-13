package org.gumel.jojoha.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;

public class MeteoriteArmorModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteorite_armor"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/outfit/meteorite_armor.png");

    private static final float HELMET_PADDING = 0.4F;

    public MeteoriteArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        head.addOrReplaceChild("Head_r1", CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-0.375F, -4.875F, -1.375F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.15F + HELMET_PADDING))
                        .texOffs(0, 30).addBox(-2.125F, -5.275F, -6.375F, 2.0F, 4.0F, 10.0F, new CubeDeformation(0.2F + HELMET_PADDING))
                        .texOffs(56, 0).addBox(-2.125F, -5.375F, -2.375F, 2.0F, 4.0F, 6.0F, new CubeDeformation(0.0F + HELMET_PADDING))
                        .texOffs(32, 0).addBox(-5.125F, -4.375F, -5.375F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F + HELMET_PADDING))
                        .texOffs(32, 12).addBox(-3.125F, 1.875F, 3.375F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F + HELMET_PADDING))
                        .texOffs(0, 16).addBox(-5.125F, -4.375F, -3.375F, 8.0F, 8.0F, 6.0F, new CubeDeformation(0.5F + HELMET_PADDING))
                        .texOffs(0, 0).addBox(-5.125F, -4.375F, -5.375F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.3F + HELMET_PADDING)),
                PartPose.offsetAndRotation(-1.184F, -3.625F, -1.125F, 0.0F, -3.098F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(28, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                        .texOffs(24, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("Body_r1", CubeListBuilder.create()
                        .texOffs(24, 42).addBox(-4.0F, -5.5F, -1.0F, 8.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 15.5F, 2.5F, 0.0873F, 0.0F, 0.0F));
        body.addOrReplaceChild("Body_r2", CubeListBuilder.create()
                        .texOffs(0, 44).mirror().addBox(-1.5F, -5.5F, -2.5F, 3.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offsetAndRotation(3.5F, 15.5F, 0.0F, 0.0F, 0.0F, -0.1745F));
        body.addOrReplaceChild("Body_r3", CubeListBuilder.create()
                        .texOffs(0, 44).addBox(-1.5F, -5.5F, -2.5F, 3.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, 15.5F, 0.0F, 0.0F, 0.0F, 0.1745F));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(52, 12).addBox(-3.0F, 3.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.1F))
                        .texOffs(52, 23).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.1F))
                        .texOffs(32, 58).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("Right_Arm_r1", CubeListBuilder.create()
                        .texOffs(60, 41).addBox(-2.5F, -3.0F, 2.4F, 5.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 41).addBox(-2.5F, -3.0F, -2.4F, 5.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.5F, -1.0F, 0.0F, 0.0F, 0.0F, 0.7854F));
        rightArm.addOrReplaceChild("Right_Arm_r2", CubeListBuilder.create()
                        .texOffs(48, 32).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(-1.5F, 0.5F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(52, 12).mirror().addBox(-1.0F, 3.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                        .texOffs(52, 23).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                        .texOffs(32, 58).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("Left_Arm_r1", CubeListBuilder.create()
                        .texOffs(60, 41).mirror().addBox(-2.5F, -3.0F, 2.4F, 5.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                        .texOffs(60, 41).mirror().addBox(-2.5F, -3.0F, -2.4F, 5.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offsetAndRotation(2.5F, -1.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
        leftArm.addOrReplaceChild("Left_Arm_r2", CubeListBuilder.create()
                        .texOffs(48, 32).mirror().addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false),
                PartPose.offsetAndRotation(1.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.2618F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(16, 55).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.1F))
                        .texOffs(16, 67).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.3F))
                        .texOffs(48, 58).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.12F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(16, 55).mirror().addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                        .texOffs(16, 67).mirror().addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false)
                        .texOffs(48, 58).mirror().addBox(-2.0F, 1.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.12F)).mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void showOnly(EquipmentSlot slot) {
        ArmorOutfits.showStandardParts(this, slot);
    }
}
