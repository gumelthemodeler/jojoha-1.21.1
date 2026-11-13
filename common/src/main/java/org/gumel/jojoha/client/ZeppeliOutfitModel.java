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

public class ZeppeliOutfitModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "zeppeli_outfit"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/outfit/zeppeli_outfit.png");

    private final ModelPart coat;
    private final ModelPart waist;
    private final ModelPart rightTrouser;
    private final ModelPart leftTrouser;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public ZeppeliOutfitModel(ModelPart root) {
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

        PartDefinition p1 = rightTrouser.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(40, 55).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.119F))
                .texOffs(56, 55).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.ZERO);
        PartDefinition p2 = leftTrouser.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(40, 55).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.119F)).mirror(false)
                .texOffs(56, 55).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false),
                PartPose.ZERO);
        PartDefinition p3 = rightBoot.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(72, 39).addBox(-2.0F, 10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.1F))
                .texOffs(48, 71).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.29F)),
                PartPose.ZERO);
        PartDefinition p4 = leftBoot.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(72, 39).mirror().addBox(-2.0F, 10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
                .texOffs(48, 71).mirror().addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.29F)).mirror(false),
                PartPose.ZERO);
        PartDefinition p5 = right_arm.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(24, 55).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.15F))
                .texOffs(56, 39).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F))
                .texOffs(64, 32).addBox(-3.0F, 5.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.4F)),
                PartPose.ZERO);
        PartDefinition p6 = left_arm.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(24, 55).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.15F)).mirror(false)
                .texOffs(56, 39).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)).mirror(false)
                .texOffs(64, 32).mirror().addBox(-1.0F, 5.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.4F)).mirror(false),
                PartPose.ZERO);
        PartDefinition p7 = coat.addOrReplaceChild("piece", CubeListBuilder.create().texOffs(32, 39).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                .texOffs(0, 42).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.3F))
                .texOffs(32, 28).addBox(-4.5F, -3.0F, -2.5F, 9.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(72, 51).addBox(-2.2F, 0.0F, -5.0F, 0.0F, 12.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 74).addBox(2.2F, 0.0F, -5.0F, 0.0F, 12.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(32, 12).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        PartDefinition p8 = head.addOrReplaceChild("piece", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.75F, 0.0F));
        PartDefinition p9 = p8.addOrReplaceChild("Head_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 1.8F, -6.0F, 12.0F, 0.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12).addBox(-4.0F, -5.3F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.15F)),
                PartPose.offsetAndRotation(-0.35F, -9.25F, 0.0F, 0.0F, 0.0F, -0.2182F));

        return LayerDefinition.create(mesh, 128, 128);
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
