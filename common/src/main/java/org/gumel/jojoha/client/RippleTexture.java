package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.ripple.RippleField;
import org.gumel.jojoha.stand.ripple.RippleMode;
import org.gumel.jojoha.stand.ripple.RippleProfile;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RippleTexture {
    private static final int STEPS_PER_SECOND = 24;

    private static final float HALO_ALPHA = 0.08F;
    private static final float CORE_ALPHA = 0.4F;

    private static final Map<ResourceLocation, NativeImage> STENCILS = new HashMap<>();

    private static final Map<ResourceLocation, Boolean> MISSING = new HashMap<>();

    private static final Map<Key, Sheet> SHEETS = new HashMap<>();

    private RippleTexture() {
    }

    private record Key(ResourceLocation stencil, RippleMode mode, int tint) {
    }

    private static final class Sheet {
        private final ResourceLocation id;
        private final DynamicTexture texture;
        private final float phase;
        private int step = Integer.MIN_VALUE;

        private Sheet(ResourceLocation id, DynamicTexture texture, float phase) {
            this.id = id;
            this.texture = texture;
            this.phase = phase;
        }
    }

    @Nullable
    public static ResourceLocation of(ResourceLocation stencil, RippleProfile field, RippleMode mode,
                                      int tint, double seconds) {
        NativeImage source = stencilImage(stencil);
        if (source == null) {
            return null;
        }

        Sheet sheet = SHEETS.computeIfAbsent(new Key(stencil, mode, tint), key -> build(key, source));
        int step = (int) Math.floor(seconds * STEPS_PER_SECOND);
        if (sheet.step != step) {
            sheet.step = step;
            paint(source, sheet.texture.getPixels(), field, tint, step / (double) STEPS_PER_SECOND,
                    sheet.phase);
            sheet.texture.upload();
        }
        return sheet.id;
    }

    private static Sheet build(Key key, NativeImage source) {
        DynamicTexture texture = new DynamicTexture(source.getWidth(), source.getHeight(), false);
        texture.setFilter(false, false);

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                "ripple/" + key.mode().name().toLowerCase(Locale.ROOT) + "/"
                        + Integer.toHexString(key.tint()) + "/"
                        + key.stencil().getNamespace() + "/"
                        + key.stencil().getPath().replace('/', '_'));
        Minecraft.getInstance().getTextureManager().register(id, texture);

        float phase = Math.abs(key.stencil().toString().hashCode() % 1000) / 1000F * Mth.TWO_PI;
        return new Sheet(id, texture, phase);
    }

    private static void paint(NativeImage stencil, NativeImage out, RippleProfile field, int tint,
                              double seconds, float phase) {
        int width = stencil.getWidth();
        int height = stencil.getHeight();
        int red = (tint >> 16) & 0xFF;
        int green = (tint >> 8) & 0xFF;
        int blue = tint & 0xFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int cover = FastColor.ABGR32.alpha(stencil.getPixelRGBA(x, y));
                if (cover == 0) {
                    out.setPixelRGBA(x, y, 0);
                    continue;
                }

                float halo = RippleField.halo(field, x, y, seconds, phase);
                if (halo <= 0F) {
                    out.setPixelRGBA(x, y, 0);
                    continue;
                }

                float core = RippleField.core(field, x, y, seconds, phase);
                float burn = field.glow() * core;
                int alpha = Math.round(cover * Mth.clamp(HALO_ALPHA * halo + CORE_ALPHA * core, 0F, 1F));
                out.setPixelRGBA(x, y, FastColor.ABGR32.color(alpha,
                        lift(blue, burn), lift(green, burn), lift(red, burn)));
            }
        }
    }

    private static int lift(int channel, float burn) {
        return Mth.clamp(Math.round(channel + (255 - channel) * burn), 0, 255);
    }

    @Nullable
    private static NativeImage stencilImage(ResourceLocation stencil) {
        NativeImage cached = STENCILS.get(stencil);
        if (cached != null) {
            return cached;
        }
        if (MISSING.containsKey(stencil)) {
            return null;
        }

        Resource resource = Minecraft.getInstance().getResourceManager().getResource(stencil).orElse(null);
        if (resource == null) {
            MISSING.put(stencil, true);
            return null;
        }

        try (InputStream stream = resource.open()) {
            NativeImage image = NativeImage.read(stream);
            STENCILS.put(stencil, image);
            return image;
        } catch (Exception failed) {
            MISSING.put(stencil, true);
            return null;
        }
    }

    public static void forget() {
        for (NativeImage image : STENCILS.values()) {
            image.close();
        }
        STENCILS.clear();
        MISSING.clear();
        for (Sheet sheet : SHEETS.values()) {
            Minecraft.getInstance().getTextureManager().release(sheet.id);
        }
        SHEETS.clear();
    }
}
