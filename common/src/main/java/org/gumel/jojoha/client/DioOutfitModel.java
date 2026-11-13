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

public class DioOutfitModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dio_outfit"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/outfit/dio_outfit.png");

    private final ModelPart rightTrouser;
    private final ModelPart leftTrouser;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public DioOutfitModel(ModelPart root) {
        super(root);
        this.rightTrouser = rightLeg.getChild("trouser");
        this.leftTrouser = leftLeg.getChild("trouser");
        this.rightBoot = rightLeg.getChild("boot");
        this.leftBoot = leftLeg.getChild("boot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(54, 12).addBox(-4.0F, -6.5F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        head.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-1.5F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6F, -5.25F, -4.0F, 0.0F, 0.0F, -0.7854F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.25F))
                .texOffs(24, 12).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.3F))
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                .texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.15F)),
                PartPose.ZERO);

        body.addOrReplaceChild("body_r1", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.1F, 10.5F, -2.5F, 0.0F, 0.0F, -0.7854F));

        body.addOrReplaceChild("body_r2", CubeListBuilder.create()
                .texOffs(46, 57).mirror().addBox(-2.0F, -1.5F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false),
                PartPose.offsetAndRotation(2.5F, 11.75F, 0.0F, 0.0F, 0.0F, 0.8727F));

        body.addOrReplaceChild("body_r3", CubeListBuilder.create()
                .texOffs(46, 57).addBox(-3.0F, -1.5F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(-2.5F, 11.75F, 0.0F, 0.0F, 0.0F, -0.8727F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                .texOffs(32, 40).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F))
                .texOffs(16, 40).addBox(-3.0F, -3.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 32).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.12F)).mirror(false)
                .texOffs(32, 40).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false)
                .texOffs(16, 40).mirror().addBox(-1.0F, -3.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition rightLegTrouser = rightLeg.addOrReplaceChild("trouser", CubeListBuilder.create()
                .texOffs(24, 24).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                .texOffs(40, 24).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F))
                .texOffs(28, 52).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.4F)),
                PartPose.ZERO);

        rightLegTrouser.addOrReplaceChild("right_leg_r1", CubeListBuilder.create()
                .texOffs(16, 36).addBox(-1.5F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.5F, 4.75F, -2.5F, 0.0F, 0.0F, -0.7854F));
        rightLeg.addOrReplaceChild("boot", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-2.0F, 10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.119F))
                .texOffs(48, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.119F))
                .texOffs(48, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.11F))
                .texOffs(0, 48).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.29F)),
                PartPose.ZERO);

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition leftLegTrouser = leftLeg.addOrReplaceChild("trouser", CubeListBuilder.create()
                .texOffs(24, 24).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.12F)).mirror(false)
                .texOffs(40, 24).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false)
                .texOffs(28, 52).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.4F)).mirror(false),
                PartPose.ZERO);

        leftLegTrouser.addOrReplaceChild("left_leg_r1", CubeListBuilder.create()
                .texOffs(16, 36).mirror().addBox(-0.5F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offsetAndRotation(-0.5F, 4.75F, -2.5F, 0.0F, 0.0F, 0.7854F));
        leftLeg.addOrReplaceChild("boot", CubeListBuilder.create()
                .texOffs(48, 0).mirror().addBox(-2.0F, 10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.119F)).mirror(false)
                .texOffs(48, 0).mirror().addBox(-2.0F, 8.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.119F)).mirror(false)
                .texOffs(48, 0).mirror().addBox(-2.0F, 6.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.11F)).mirror(false)
                .texOffs(0, 48).mirror().addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.29F)).mirror(false),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 86, 64);
    }

    @Override
    public void showOnly(EquipmentSlot slot) {
        ArmorOutfits.showStandardParts(this, slot);

        boolean trousers = slot == EquipmentSlot.LEGS;
        boolean boots = slot == EquipmentSlot.FEET;
        rightTrouser.visible = trousers;
        leftTrouser.visible = trousers;
        rightBoot.visible = boots;
        leftBoot.visible = boots;
    }
}
