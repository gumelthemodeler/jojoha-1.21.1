public class respawn_totem<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "respawn_totem"), "main");
    private final ModelPart bone;
    private final ModelPart bone2;
    private final ModelPart bone3;

    public respawn_totem(ModelPart root) {
        this.bone = root.getChild("bone");
        this.bone2 = this.bone.getChild("bone2");
        this.bone3 = this.bone2.getChild("bone3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 21).addBox(-14.0F, -16.0F, 2.0F, 12.0F, 16.0F, 12.0F, new CubeDeformation(-0.01F))
        .texOffs(0, 0).addBox(-16.0F, -5.0F, 0.0F, 16.0F, 5.0F, 16.0F, new CubeDeformation(0.0F))
        .texOffs(48, 21).addBox(-12.0F, -32.0F, 4.0F, 8.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(48, 45).addBox(-13.0F, -34.0F, 3.0F, 10.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 24.0F, -8.0F));

        PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(0, 49).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(18, 49).addBox(-3.0F, -3.0F, 7.5F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, -27.0F, 3.5F));

        PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(0, 58).addBox(-3.0F, -1.0F, -6.0F, 6.0F, 5.0F, 3.0F, new CubeDeformation(-0.01F))
        .texOffs(0, 67).addBox(-3.0F, -1.5F, -6.0F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.15F))
        .texOffs(0, 67).addBox(-3.0F, -1.5F, 3.0F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.15F))
        .texOffs(0, 58).addBox(-3.0F, -1.0F, 3.0F, 6.0F, 5.0F, 3.0F, new CubeDeformation(-0.01F)), PartPose.offset(0.0F, -1.0F, 4.5F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
