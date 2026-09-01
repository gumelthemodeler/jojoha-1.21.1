package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Turning a fresh map into a developed photograph.
 *
 * <h2>The greyscale is baked, not tinted</h2>
 *
 * <p>The obvious way to make a map monochrome is to draw it through a shader or a grey vertex
 * colour, and both are worse than they look. A vertex colour only darkens - there is no shade of
 * grey you can multiply red by to get grey - and a shader means the picture is only monochrome on
 * the screen of whoever has the mod's render path running, so the same photograph in an item frame
 * or on another player's screen is in colour.
 *
 * <p>So the colours are rewritten in the saved data itself, once, when the picture develops. After
 * that it simply is a black and white map: in the hand, in a frame, on a wall, for everyone, with no
 * rendering involved at all. It also survives the mod being removed, which is the sort of thing that
 * separates a change to the world from a change to the view of it.
 *
 * <h2>How the grey is chosen</h2>
 *
 * <p>Every map pixel is a palette index and a brightness. Vanilla's palette already carries four
 * neutral entries - black, grey, light grey and snow - and each of those has four brightnesses, so
 * there are sixteen greys available without inventing anything. Each source colour is converted to
 * its real RGB, reduced to luminance, and matched to whichever of those sixteen is closest.
 *
 * <p>Luminance rather than a flat channel average, for the usual reason: the average makes a
 * saturated red and a saturated blue the same grey, and a photograph of a lava field would come out
 * looking like a photograph of the sea.
 */
public final class PhotographDevelop {
    /** The neutral entries in vanilla's map palette, darkest first. */
    private static final MapColor[] NEUTRALS = {
            MapColor.COLOR_BLACK, MapColor.COLOR_GRAY, MapColor.COLOR_LIGHT_GRAY, MapColor.SNOW};

    private static final MapColor.Brightness[] SHADES = {
            MapColor.Brightness.LOWEST, MapColor.Brightness.LOW,
            MapColor.Brightness.NORMAL, MapColor.Brightness.HIGH};

    /** The sixteen greys, and what each of them is worth, built once. */
    private static byte[] rampBytes;
    private static float[] rampLuma;

    private PhotographDevelop() {
    }

    /**
     * Develops the picture: monochrome, and marked with where it was taken of.
     *
     * @param found where the structure is
     */
    public static void develop(ServerLevel level, ItemStack map, BlockPos found) {
        // The X. This is the single thing that makes an explorer map readable, and a photograph
        // without it is a grey smear the player has to interpret. Vanilla puts one on every treasure
        // map for exactly this reason.
        MapItemSavedData.addTargetDecoration(map, found, "jojoha_subject", MapDecorationTypes.RED_X);

        MapId id = map.get(net.minecraft.core.component.DataComponents.MAP_ID);
        MapItemSavedData data = id == null ? null : level.getMapData(id);
        if (data == null) {
            return;
        }

        greyscale(data);
    }

    /** Rewrites every pixel of the saved map to its nearest neutral. */
    private static void greyscale(MapItemSavedData data) {
        buildRamp();

        for (int i = 0; i < data.colors.length; i++) {
            byte packed = data.colors[i];
            if (packed == 0) {
                // Nothing drawn here. Left alone so unexplored map stays unexplored rather than
                // becoming black, which would read as a photograph of a cave.
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

        // Saved, and nothing else needed.
        //
        // There is no call here to mark the pixels dirty for sending, because the private method
        // that would do it cannot be reached and does not need to be: this runs before the picture
        // has ever been held, and a player picking a map up for the first time is sent the whole
        // thing rather than a difference. The greyscale is simply what the map has always said.
        data.setDirty();
    }

    /** The closest of the sixteen greys to a given brightness. */
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

    /**
     * How bright a palette entry is at a given shade, from nought to one.
     *
     * <p>Read off {@code col} and the shade's own multiplier rather than through
     * {@code calculateRGBColor}. That method hands back a packed colour in whichever order the map
     * texture wants, and guessing wrong between ARGB and ABGR swaps red with blue - which would not
     * throw, would not look obviously broken, and would quietly print a photograph of a lava field
     * that looks like a photograph of the sea. {@code col} is documented as plain RGB and the
     * multiplier is a plain scale, so there is nothing to get backwards.
     *
     * <p>Rec. 601 weights, for the usual reason: a flat channel average makes saturated red and
     * saturated blue the same grey.
     */
    private static float luma(MapColor colour, MapColor.Brightness shade) {
        float r = ((colour.col >> 16) & 0xFF) / 255F;
        float g = ((colour.col >> 8) & 0xFF) / 255F;
        float b = (colour.col & 0xFF) / 255F;

        return (r * 0.299F + g * 0.587F + b * 0.114F) * (shade.modifier / 255F);
    }
}
