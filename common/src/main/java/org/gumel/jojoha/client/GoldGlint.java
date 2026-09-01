package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * The enchantment glint, in gold, for meteorite gear.
 *
 * <h2>Why this is a render type rather than a texture swap</h2>
 *
 * <p>The glint's colour lives in the sheet - vanilla's measures #592A8F at 0.709 saturation - so a
 * gold glint needs a gold sheet, and a sheet is chosen by the render type. Swapping the bound
 * texture around the draw call would not work: item geometry is batched and flushed later, by which
 * time whatever flag said "this one is gold" has long been cleared. A distinct render type is the
 * only thing that survives batching, because the vertices are sorted into its bucket at the moment
 * they are written and drawn with its texture whenever that bucket is finally emptied.
 *
 * <h2>Why it extends RenderType by hand</h2>
 *
 * <p>Vanilla builds these through {@code RenderType.create}, which is package-private and returns a
 * package-private type - unreachable from here, and not worth an access widener on two loaders.
 * But {@code RenderType}'s constructor is public and it has no abstract methods, so a subclass can
 * simply be written; and because {@code RenderType extends RenderStateShard}, a subclass can also
 * reach the protected shards that make a glint a glint. Both were checked rather than assumed.
 *
 * <p>The shards and their order are vanilla's own for {@code glint()} and {@code entityGlintDirect()}
 * - shader, texture, colour-only write mask, no culling, equal-depth test, additive transparency,
 * and the scrolling texture matrix that makes it move. Clearing runs in reverse, which is what a
 * composite state does and what leaving the GL state as we found it requires.
 */
public final class GoldGlint extends RenderType {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/misc/enchanted_glint_item_gold.png");

    private static final TextureStateShard SHEET = new TextureStateShard(TEXTURE, true, false);

    /** The flat glint, for an item drawn in a GUI slot. */
    public static final RenderType ITEM = new GoldGlint(
            "jojoha_gold_glint",
            () -> {
                RENDERTYPE_GLINT_SHADER.setupRenderState();
                SHEET.setupRenderState();
                COLOR_WRITE.setupRenderState();
                NO_CULL.setupRenderState();
                EQUAL_DEPTH_TEST.setupRenderState();
                GLINT_TRANSPARENCY.setupRenderState();
                GLINT_TEXTURING.setupRenderState();
            },
            () -> {
                GLINT_TEXTURING.clearRenderState();
                GLINT_TRANSPARENCY.clearRenderState();
                EQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                COLOR_WRITE.clearRenderState();
                SHEET.clearRenderState();
                RENDERTYPE_GLINT_SHADER.clearRenderState();
            });

    /** The glint on an item held in the world, drawn straight rather than buffered. */
    public static final RenderType DIRECT = new GoldGlint(
            "jojoha_gold_glint_direct",
            () -> {
                RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER.setupRenderState();
                SHEET.setupRenderState();
                COLOR_WRITE.setupRenderState();
                NO_CULL.setupRenderState();
                EQUAL_DEPTH_TEST.setupRenderState();
                GLINT_TRANSPARENCY.setupRenderState();
                ENTITY_GLINT_TEXTURING.setupRenderState();
            },
            () -> {
                ENTITY_GLINT_TEXTURING.clearRenderState();
                GLINT_TRANSPARENCY.clearRenderState();
                EQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                COLOR_WRITE.clearRenderState();
                SHEET.clearRenderState();
                RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER.clearRenderState();
            });

    private GoldGlint(String name, Runnable setup, Runnable clear) {
        // The buffer size, format and mode are vanilla's for a glint pass. Neither flag applies: a
        // glint is not part of block crumbling, and it does not need upload-time sorting because it
        // draws at equal depth over geometry that has already been sorted.
        super(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
                setup, clear);
    }
}
