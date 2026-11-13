package org.gumel.jojoha.fabric.client;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.client.StrataFaces;
import java.util.List;
import java.util.function.Supplier;

public final class FabricStrataModel extends ForwardingBakedModel {
    private static final Direction[] SIDES = Direction.values();

    private final StrataFaces faces = new StrataFaces();
    private RenderMaterial material;

    public FabricStrataModel(BakedModel wrapped) {
        super(wrapped);
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos,
                               Supplier<RandomSource> random, RenderContext context) {
        int pattern = StrataFaces.pattern(view, pos, state);
        if (pattern == StrataFaces.PLAIN) {
            super.emitBlockQuads(view, state, pos, random, context);
            return;
        }
        QuadEmitter emitter = context.getEmitter();
        RenderMaterial standard = material();
        for (int i = 0; i <= SIDES.length; i++) {
            Direction side = i < SIDES.length ? SIDES[i] : null;
            if (!context.hasTransform() && context.isFaceCulled(side)) {
                continue;
            }
            List<BakedQuad> quads = faces.faces(wrapped.getQuads(state, side, random.get()), side, pattern);
            for (BakedQuad quad : quads) {
                emitter.fromVanilla(quad, standard, side);
                emitter.emit();
            }
        }
    }

    private RenderMaterial material() {
        if (material == null) {
            material = RendererAccess.INSTANCE.getRenderer().materialFinder()
                    .shadeMode(ShadeMode.VANILLA)
                    .ambientOcclusion(wrapped.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE)
                    .find();
        }
        return material;
    }
}
