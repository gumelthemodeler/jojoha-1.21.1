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

public class CaesarOutfitModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "caesar_outfit"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/outfit/caesar_outfit.png");

    private final ModelPart coat;
    private final ModelPart waist;
    private final ModelPart rightTrouser;
    private final ModelPart leftTrouser;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public CaesarOutfitModel(ModelPart root) {
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

        PartDefinition p1 = coat.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(24, 20).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 9.0F, 4.0F, new CubeDeformation(0.36F))
                .texOffs(0, 20).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 9.0F, 4.0F, new CubeDeformation(0.1F))
                .texOffs(0, 0).addBox(-4.5F, -3.0F, -2.5F, 9.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(32, 47).addBox(2.25F, 0.0F, -4.0F, 0.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(32, 47).mirror().addBox(-2.25F, 0.0F, -4.0F, 0.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.ZERO);
        PartDefinition p2 = head.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(31, 58).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        PartDefinition p3 = p2.addOrReplaceChild("bone", CubeListBuilder.create(),
                PartPose.ZERO);
        PartDefinition p4 = right_arm.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.09F))
                .texOffs(0, 33).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)),
                PartPose.ZERO);
        PartDefinition p5 = left_arm.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.09F)).mirror(false)
                .texOffs(0, 33).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)).mirror(false),
                PartPose.ZERO);
        PartDefinition p6 = rightTrouser.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(40, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.1F))
                .texOffs(40, 45).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.349F))
                .texOffs(0, 56).addBox(-2.5F, 3.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-0.1F, 0.0F, 0.0F));
        PartDefinition p7 = leftTrouser.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(40, 33).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                .texOffs(40, 45).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.349F)).mirror(false)
                .texOffs(0, 56).mirror().addBox(-2.5F, 3.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(0.1F, 0.0F, 0.0F));
        PartDefinition p8 = rightBoot.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(16, 47).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.09F))
                .texOffs(48, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.349F)),
                PartPose.offset(-0.1F, 0.0F, 0.0F));
        PartDefinition p9 = leftBoot.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(16, 47).mirror().addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.09F)).mirror(false)
                .texOffs(48, 0).mirror().addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.349F)).mirror(false),
                PartPose.offset(0.1F, 0.0F, 0.0F));
        PartDefinition p10 = waist.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(16, 40).addBox(-2.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.3F))
                .texOffs(16, 33).addBox(-2.0F, -3.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.101F)),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        PartDefinition p11 = p10.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 65).addBox(-5.0F, -2.0F, 0.0F, 7.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -2.0F, -2.5F, 0.0F, 0.0F, -0.7854F));

        return LayerDefinition.create(mesh, 100, 100);
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
