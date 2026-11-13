package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.Jojoha;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StrataFaces {
    public static final ResourceLocation BLOCK = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ashy_rockland_stone");
    private static final ResourceLocation SIDE = texture("ashy_rockland_stone_side");
    private static final ResourceLocation TOP = texture("ashy_rockland_stone_top");
    private static final ResourceLocation TOP_VARIANT = texture("ashy_rockland_stone_top_variant");
    private static final ResourceLocation CONNECTED_TEXTURE = texture("ashy_rockland_stone_connected");
    private static final ResourceLocation CENTRAL_TEXTURE = texture("ashy_rockland_stone_central");

    public static final int PLAIN = 0;
    public static final int CONNECTED = 1;
    public static final int CENTRAL = 2;

    private final Map<BakedQuad, BakedQuad> connected = new ConcurrentHashMap<>();
    private final Map<BakedQuad, BakedQuad> central = new ConcurrentHashMap<>();

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "block/" + name);
    }

    public static boolean wraps(ModelResourceLocation location) {
        return location != null && BLOCK.equals(location.id())
                && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant());
    }

    public static int pattern(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int pattern = 0;
        for (Direction face : Direction.values()) {
            pattern |= face(level, pos, state, face) << shift(face);
        }
        return pattern;
    }

    private static int shift(Direction face) {
        return face.getAxis().isVertical() ? (face == Direction.UP ? 8 : 10) : face.get2DDataValue() * 2;
    }

    private static int face(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        Direction right = face.getAxis().isVertical() ? Direction.EAST : face.getCounterClockWise();
        boolean joined = level.getBlockState(pos.relative(right)).is(state.getBlock())
                || level.getBlockState(pos.relative(right.getOpposite())).is(state.getBlock());
        if (!joined) {
            return PLAIN;
        }
        int along = pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ();
        return Math.floorMod(along, 2) == 0 ? CONNECTED : CENTRAL;
    }

    public List<BakedQuad> faces(List<BakedQuad> quads, Direction side, int pattern) {
        if (side == null || quads.isEmpty()) {
            return quads;
        }
        int choice = (pattern >> shift(side)) & 3;
        if (choice == PLAIN) {
            return quads;
        }
        Map<BakedQuad, BakedQuad> cache = choice == CONNECTED ? connected : central;
        ResourceLocation target = choice == CONNECTED ? CONNECTED_TEXTURE : CENTRAL_TEXTURE;
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(strata(quad.getSprite().contents().name())
                    ? cache.computeIfAbsent(quad, original -> retexture(original, sprite(target)))
                    : quad);
        }
        return out;
    }

    private static boolean strata(ResourceLocation texture) {
        return SIDE.equals(texture) || TOP.equals(texture) || TOP_VARIANT.equals(texture);
    }

    private static TextureAtlasSprite sprite(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(id);
    }

    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite to) {
        TextureAtlasSprite from = quad.getSprite();
        int[] data = quad.getVertices().clone();
        int stride = data.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int at = vertex * stride + 4;
            float u = Float.intBitsToFloat(data[at]);
            float v = Float.intBitsToFloat(data[at + 1]);
            data[at] = Float.floatToRawIntBits(to.getU(from.getUOffset(u)));
            data[at + 1] = Float.floatToRawIntBits(to.getV(from.getVOffset(v)));
        }
        return new BakedQuad(data, quad.getTintIndex(), quad.getDirection(), to, quad.isShade());
    }
}
