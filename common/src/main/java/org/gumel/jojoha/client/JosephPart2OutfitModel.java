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

public class JosephPart2OutfitModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "joseph_p2_outfit"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/outfit/joseph_p2_outfit.png");

    private final ModelPart coat;
    private final ModelPart waist;
    private final ModelPart rightTrouser;
    private final ModelPart leftTrouser;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public JosephPart2OutfitModel(ModelPart root) {
        super(root);
        this.coat = body.getChild("coat");
        this.waist = body.getChild("waist");
        this.rightTrouser = rightLeg.getChild("trouser");
        this.leftTrouser = leftLeg.getChild("trouser");
        this.rightBoot = rightLeg.getChild("boot");
        this.leftBoot = leftLeg.getChild("boot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition coat = body.addOrReplaceChild("coat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition waist = body.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition rightTrouser = rightLeg.addOrReplaceChild("trouser", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightBoot = rightLeg.addOrReplaceChild("boot", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition leftTrouser = leftLeg.addOrReplaceChild("trouser", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftBoot = leftLeg.addOrReplaceChild("boot", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition p1 = coat.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.36F))
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        PartDefinition p2 = coat.addOrReplaceChild("piece2", CubeListBuilder.create().texOffs(38, 53).addBox(0.0F, 1.1166F, 3.3467F, 3.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        PartDefinition p3 = p2.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 52).addBox(-4.0F, -2.0F, -4.0F, 10.0F, 3.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, 1.0F, -0.5F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p4 = p2.addOrReplaceChild("bone", CubeListBuilder.create(),
                PartPose.ZERO);
        PartDefinition p5 = right_arm.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.09F))
                .texOffs(24, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)),
                PartPose.ZERO);
        PartDefinition p6 = left_arm.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.09F)).mirror(false)
                .texOffs(24, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)).mirror(false),
                PartPose.ZERO);
        PartDefinition p7 = rightTrouser.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.1F))
                .texOffs(16, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.349F)),
                PartPose.offset(-0.1F, 0.0F, 0.0F));
        PartDefinition p8 = leftTrouser.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                .texOffs(16, 32).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.349F)).mirror(false),
                PartPose.offset(0.1F, 0.0F, 0.0F));
        PartDefinition p9 = rightBoot.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.09F))
                .texOffs(40, 0).addBox(-2.0F, 5.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.349F)),
                PartPose.offset(-0.1F, 0.0F, 0.0F));
        PartDefinition p10 = leftBoot.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(32, 32).mirror().addBox(-2.0F, 5.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.09F)).mirror(false)
                .texOffs(40, 0).mirror().addBox(-2.0F, 5.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.349F)).mirror(false),
                PartPose.offset(0.1F, 0.0F, 0.0F));
        PartDefinition p11 = waist.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(40, 18).addBox(-2.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.2F))
                .texOffs(40, 11).addBox(-2.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.101F))
                .texOffs(0, 0).addBox(1.0F, -2.5F, -2.25F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void showOnly(EquipmentSlot slot) {
        ArmorOutfits.showStandardParts(this, slot);

        boolean trousers = slot == EquipmentSlot.LEGS;
        boolean boots = slot == EquipmentSlot.FEET;
        coat.visible = slot == EquipmentSlot.CHEST;
        waist.visible = trousers;
        rightTrouser.visible = trousers;
        leftTrouser.visible = trousers;
        rightBoot.visible = boots;
        leftBoot.visible = boots;
    }
}
