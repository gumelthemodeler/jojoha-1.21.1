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
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;

/**
 * The meteorite armour, worn.
 *
 * <h2>Why this is a HumanoidModel and not the exported EntityModel</h2>
 *
 * <p>Armour has to move with the body it is on, and vanilla does that by copying the wearer's pose
 * onto a {@link HumanoidModel} - part by part, by name. So the model has to <em>be</em> one, with
 * parts called head, hat, body, right_arm, left_arm, right_leg and left_leg. The Blockbench export
 * is an {@code EntityModel} with a rig of its own: a root, a torso wrapper, and parts named Head,
 * Body, RightArm and so on. Handed to the armour layer as it stands, nothing would pose it and it
 * would sit rigid while the player walked.
 *
 * <h2>What the mapping cost</h2>
 *
 * <p>Almost nothing, because the rig was drawn against vanilla's. Flattening root (0, 12, 0) and
 * torso (0, 0, 0) into absolute positions gives body at (0, 0, 0), the arms at (-5, 2, 0) and
 * (5, 2, 0), and the legs at (-1.9, 12, 0) and (1.9, 12, 0) - which are vanilla's humanoid poses
 * exactly. Only the head differs: the export puts it a unit high, at (0, 1, 0) against vanilla's
 * (0, 0, 0), so that unit is pushed down into its child instead and the helmet lands where it was
 * drawn.
 *
 * <p>An empty "hat" is added beside the head, not under it: HumanoidModel pulls all seven parts
 * straight off the root and throws if one is missing. Nothing is drawn there.
 */
public class MeteoriteArmorModel extends HumanoidModel<LivingEntity> implements OutfitModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteorite_armor"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Jojoha.MOD_ID, "textures/entity/meteorite_armor.png");

    /**
     * How far the helmet stands off the skull, in pixels, on top of each box's authored inflation.
     *
     * <p>The player's head is a solid 8 by 8 by 8 and the helmet was drawn close enough to it that
     * the face came through. Padding every one of the helmet's boxes by the same amount pushes the
     * whole shell outward without changing its shape or where it sits - which a scale would not do,
     * because a scale grows from the neck pivot and would lift the helmet off the head as it grew.
     *
     * <p>This is the number to nudge if it still clips, or if it now looks too big.
     */
    private static final float HELMET_PADDING = 0.4F;

    public MeteoriteArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The export's head sits at an absolute (0, 1, 0); vanilla's is (0, 0, 0), so the difference
        // is carried by the child below rather than by moving the part the pose is copied onto.
        //
        // The offset has been adjusted from what was authored. Up and forward were judged by eye -
        // a pixel up and half a pixel forward, because the face came through at the brow. The
        // sideways figure was not: this part turns about 177.5 degrees on Y and its shell box is not
        // centred on the part origin, so the offset and where the helmet lands are different things.
        // Rotating the box centre and adding the offset put the shell 0.309 to the wearer's left of
        // the skull, and x is set to cancel exactly that. It is off by 0.2 backwards as well, which
        // is small enough to leave alone unless it shows.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // "hat" is a sibling of "head", not a child of it. HumanoidModel fetches all seven parts off
        // the root - head, hat, body, the arms and the legs - and getChild throws rather than
        // returning null, so putting it under the head crashed the moment armour was worn. Nothing
        // is drawn here; it exists because the constructor insists on finding it.
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


    /** Nothing unusual here - the standard rule covers it. */
    @Override
    public void showOnly(net.minecraft.world.entity.EquipmentSlot slot) {
        ArmorOutfits.showStandardParts(this, slot);
    }
}
