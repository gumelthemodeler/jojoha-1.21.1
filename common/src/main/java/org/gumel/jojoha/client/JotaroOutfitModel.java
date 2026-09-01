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

/**
 * Jotaro's Part 3 coat, cap and boots, worn in place of the meteorite plate.
 *
 * <h2>The mapping</h2>
 *
 * <p>The export is flat - every part straight on the root - and the body, arms and legs are already
 * at vanilla's humanoid poses. Only the head differs, sitting a unit high at (0, 1, 0), the same
 * quirk the meteorite export had. Rather than move the part - the pose is overwritten every frame by
 * the wearer's, so a change there would not survive - that unit is added to each of the head's
 * boxes, which is baked into the geometry and does.
 *
 * <h2>The boots</h2>
 *
 * <p>The export has RightBoot and LeftBoot as root parts of their own, and a {@link HumanoidModel}
 * will never draw them: it renders seven parts by name and those are not among them. They are also
 * at exactly the legs' pose, which is the clue to what they are - the same limb, drawn twice.
 *
 * <p>So each leg becomes an empty container at the vanilla pose with two children: the trouser and
 * the boot, both at zero, because their part poses were identical to the leg's. The container is
 * what the wearer's pose is copied onto, so both children follow the leg; and having them separate
 * is what lets the leggings slot show one and the boots slot the other, from one model.
 */
public class JotaroOutfitModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "jotaro_outfit_p1"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/jotaro_outfit_p1.png");

    private final ModelPart rightTrouser;
    private final ModelPart leftTrouser;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public JotaroOutfitModel(ModelPart root) {
        super(root);
        this.rightTrouser = rightLeg.getChild("trouser");
        this.leftTrouser = leftLeg.getChild("trouser");
        this.rightBoot = rightLeg.getChild("boot");
        this.leftBoot = leftLeg.getChild("boot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head boxes carry +1 on y against the export, which is the unit its part offset had.
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 27).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.15F))
                        .texOffs(28, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.35F))
                        .texOffs(0, 54).addBox(-4.0F, -5.0F, -8.0F, 8.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 33).addBox(-4.0F, -9.0F, -4.5F, 8.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(32, 11).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.12F))
                        .texOffs(32, 27).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.3F))
                        .texOffs(0, 16).addBox(-4.5F, -3.0F, -2.5F, 9.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(24, 54).addBox(-3.0F, 0.0F, -5.0F, 0.0F, 12.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 18).addBox(3.0F, 0.0F, -5.0F, 0.0F, 12.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 58).addBox(3.0F, -1.0F, -3.0F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 38).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.15F))
                        .texOffs(32, 43).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F))
                        .texOffs(56, 11).addBox(-3.0F, 5.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.4F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 38).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.15F)).mirror(false)
                        .texOffs(32, 43).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)).mirror(false)
                        .texOffs(56, 11).mirror().addBox(-1.0F, 5.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.4F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Empty containers - the pose lands here, the geometry hangs underneath.
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("trouser", CubeListBuilder.create()
                        .texOffs(16, 38).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.119F))
                        .texOffs(48, 43).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.ZERO);
        rightLeg.addOrReplaceChild("boot", CubeListBuilder.create()
                        .texOffs(0, 73).addBox(-2.0F, 10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.119F))
                        .texOffs(0, 84).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.29F)),
                PartPose.ZERO);

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("trouser", CubeListBuilder.create()
                        .texOffs(16, 38).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.119F)).mirror(false)
                        .texOffs(48, 43).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false),
                PartPose.ZERO);
        leftLeg.addOrReplaceChild("boot", CubeListBuilder.create()
                        .texOffs(0, 73).mirror().addBox(-2.0F, 10.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.119F)).mirror(false)
                        .texOffs(0, 84).mirror().addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.29F)).mirror(false),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * As the shared rule, except that legs and feet split the same limb between them: the leggings
     * show the trouser and the boots show the boot, and neither shows the other.
     */
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
