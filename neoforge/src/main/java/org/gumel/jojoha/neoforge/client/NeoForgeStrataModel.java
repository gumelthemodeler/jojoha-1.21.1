package org.gumel.jojoha.neoforge.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.gumel.jojoha.client.StrataFaces;
import java.util.List;

public final class NeoForgeStrataModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> PATTERN = new ModelProperty<>();

    private final StrataFaces faces = new StrataFaces();

    public NeoForgeStrataModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return super.getModelData(level, pos, state, modelData).derive()
                .with(PATTERN, StrataFaces.pattern(level, pos, state))
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
                                    RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Integer pattern = data.get(PATTERN);
        return pattern == null || pattern == StrataFaces.PLAIN ? quads : faces.faces(quads, side, pattern);
    }
}
