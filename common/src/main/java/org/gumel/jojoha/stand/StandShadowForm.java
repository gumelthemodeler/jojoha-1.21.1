package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.StandData;
import org.gumel.jojoha.supporter.SkinCatalog;
import org.gumel.jojoha.supporter.SupporterAuras;

import java.util.Map;
import java.util.UUID;

public record StandShadowForm(ResourceLocation model, ResourceLocation texture,
                              int topColour, int bottomColour) {
    private static final Map<ResourceLocation, StandShadowForm> FORMS = Map.of(
            StandTypes.THE_WORLD_ID, new StandShadowForm(
                    asset("geo/shadow_the_world.geo.json"),
                    asset("textures/entity/stand/the_world/shadow_theworld.png"),
                    0x9BE3FF, 0x8B008B));

    public static StandShadowForm of(StandEntity stand) {
        return of(stand.getStandType().id(), stand.getSkin(), stand.getTrustTier());
    }

    public static StandShadowForm of(ResourceLocation standId, int skin, TrustTier tier) {
        StandShadowForm form = standId == null ? null : FORMS.get(standId);
        if (form == null) {
            return null;
        }
        return tier == TrustTier.EMERGING || isShadowSkin(standId, skin, form) ? form : null;
    }

    private static boolean isShadowSkin(ResourceLocation standId, int skin, StandShadowForm form) {
        StandType type = StandTypes.byIdOrDefault(standId);
        return type.id().equals(standId) && type.textureFor(skin).equals(form.texture());
    }

    public int colourAt(float height) {
        float t = Mth.clamp(height, 0F, 1F);
        t = t * t * (3F - 2F * t);
        int red = Math.round(Mth.lerp(t, (bottomColour >> 16) & 0xFF, (topColour >> 16) & 0xFF));
        int green = Math.round(Mth.lerp(t, (bottomColour >> 8) & 0xFF, (topColour >> 8) & 0xFF));
        int blue = Math.round(Mth.lerp(t, bottomColour & 0xFF, topColour & 0xFF));
        return (red << 16) | (green << 8) | blue;
    }

    public static StandShadowForm gradientFor(ResourceLocation standId, int skin, TrustTier tier) {
        StandShadowForm form = of(standId, skin, tier);
        if (form != null) {
            return form;
        }
        SkinCatalog.Entry remote = SkinCatalog.get(skin);
        if (remote == null || remote.aura() == SkinCatalog.NO_AURA
                || remote.aura2() == SkinCatalog.NO_AURA) {
            return null;
        }
        return new StandShadowForm(null, null, remote.aura2(), remote.aura());
    }

    public boolean dark() {
        return brightness(topColour) < DARK || brightness(bottomColour) < DARK;
    }

    public static boolean dark(int rgb) {
        return brightness(rgb) < DARK;
    }

    private static final int DARK = 70;

    private static int brightness(int rgb) {
        return Math.max((rgb >> 16) & 0xFF, Math.max((rgb >> 8) & 0xFF, rgb & 0xFF));
    }

    public static int auraColourFor(StandType type, int skin, TrustTier tier) {
        StandShadowForm shadow = of(type.id(), skin, tier);
        return shadow != null ? shadow.auraColour() : type.auraColorFor(skin);
    }

    public static int auraColourFor(StandEntity stand) {
        StandType type = stand.getStandType();
        StandShadowForm shadow = of(type.id(), stand.getSkin(), stand.getTrustTier());
        return shadow != null ? shadow.auraColour()
                : SupporterAuras.tint(stand.getOwnerUuid(), type.auraColorFor(stand.getSkin()));
    }

    public static int auraColourFor(StandData stand) {
        StandType type = StandTypes.byIdOrDefault(stand.standId());
        StandShadowForm shadow = of(type.id(), stand.skin(), stand.trust());
        return shadow != null ? shadow.auraColour()
                : SupporterAuras.ownTint(type.auraColorFor(stand.skin()));
    }

    public static int auraColourFor(StandData stand, UUID owner) {
        StandType type = StandTypes.byIdOrDefault(stand.standId());
        StandShadowForm shadow = of(type.id(), stand.skin(), stand.trust());
        return shadow != null ? shadow.auraColour()
                : SupporterAuras.tint(owner, type.auraColorFor(stand.skin()));
    }

    public int auraColour() {
        return colourAt(0.5F);
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
