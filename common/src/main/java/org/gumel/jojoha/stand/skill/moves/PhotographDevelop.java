package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class PhotographDevelop {
    private static final MapColor[] NEUTRALS = {
            MapColor.COLOR_BLACK, MapColor.COLOR_GRAY, MapColor.COLOR_LIGHT_GRAY, MapColor.SNOW};

    private static final MapColor.Brightness[] SHADES = {
            MapColor.Brightness.LOWEST, MapColor.Brightness.LOW,
            MapColor.Brightness.NORMAL, MapColor.Brightness.HIGH};

    private static byte[] rampBytes;
    private static float[] rampLuma;

    private PhotographDevelop() {
    }

    public static void develop(ServerLevel level, ItemStack map, BlockPos found) {
        MapItemSavedData.addTargetDecoration(map, found, "jojoha_subject", MapDecorationTypes.RED_X);

        MapId id = map.get(DataComponents.MAP_ID);
        MapItemSavedData data = id == null ? null : level.getMapData(id);
        if (data == null) {
            return;
        }

        greyscale(data);
    }

    private static void greyscale(MapItemSavedData data) {
        buildRamp();

        for (int i = 0; i < data.colors.length; i++) {
            byte packed = data.colors[i];
            if (packed == 0) {
                continue;
            }

            int colourId = (packed & 0xFF) >> 2;
            int shadeId = packed & 3;

            MapColor source = MapColor.byId(colourId);
            if (source == null) {
                continue;
            }

            data.colors[i] = nearest(luma(source, SHADES[shadeId]));
        }

        data.setDirty();
    }

    private static byte nearest(float target) {
        int best = 0;
        float bestGap = Float.MAX_VALUE;

        for (int i = 0; i < rampLuma.length; i++) {
            float gap = Math.abs(rampLuma[i] - target);
            if (gap < bestGap) {
                bestGap = gap;
                best = i;
            }
        }
        return rampBytes[best];
    }

    private static void buildRamp() {
        if (rampBytes != null) {
            return;
        }

        rampBytes = new byte[NEUTRALS.length * SHADES.length];
        rampLuma = new float[rampBytes.length];

        int at = 0;
        for (MapColor neutral : NEUTRALS) {
            for (MapColor.Brightness shade : SHADES) {
                rampBytes[at] = neutral.getPackedId(shade);
                rampLuma[at] = luma(neutral, shade);
                at++;
            }
        }
    }

    private static float luma(MapColor colour, MapColor.Brightness shade) {
        float r = ((colour.col >> 16) & 0xFF) / 255F;
        float g = ((colour.col >> 8) & 0xFF) / 255F;
        float b = (colour.col & 0xFF) / 255F;

        return (r * 0.299F + g * 0.587F + b * 0.114F) * (shade.modifier / 255F);
    }
}
