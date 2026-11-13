package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.entity.npc.JojoNpc;

public final class NpcRenderer extends HumanoidMobRenderer<JojoNpc, PlayerModel<JojoNpc>> {

    private final PlayerModel<JojoNpc> wide;
    private final PlayerModel<JojoNpc> slim;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = model;
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public void render(JojoNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = DefaultPlayerSkin.get(npc.getUUID()).model() == PlayerSkin.Model.SLIM ? slim : wide;
        super.render(npc, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(JojoNpc npc) {
        return DefaultPlayerSkin.get(npc.getUUID()).texture();
    }
}
