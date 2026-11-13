package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.Palette;

public final class PaletteSwap {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[Palette.values().length];

    static {
        for (Palette each : Palette.values()) {
            TEXTURES[each.ordinal()] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/fx/palettes/" + each.file() + ".png");
        }
    }

    private static final long BEAT_MS = 400L;
    private static final long PACK_FADE_MS = 300L;
    private static final float STING_VOLUME = 0.55F;

    private static Palette palette;
    private static long until;

    private static void sting(Palette struck) {
        SoundEvent sound = switch (struck) {
            case IMPACT -> SoundEvents.ANVIL_LAND;
            case BREAK -> SoundEvents.GLASS_BREAK;
            case CLASH -> SoundEvents.TRIDENT_THUNDER.value();
            case AWAKEN -> SoundEvents.BEACON_ACTIVATE;
            case DREAD -> SoundEvents.WARDEN_HEARTBEAT;
            case PIERCE -> SoundEvents.TRIDENT_HIT;
        };
        float pitch = switch (struck) {
            case IMPACT -> 0.6F;
            case BREAK -> 0.7F;
            case CLASH -> 1.7F;
            case AWAKEN -> 1.4F;
            case DREAD -> 0.8F;
            case PIERCE -> 0.6F;
        };
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, STING_VOLUME));
    }

    private static final ScreenPass PASS = new ScreenPass();

    private PaletteSwap() {
    }

    public static void beat(Palette asked) {
        if (ShaderPacks.active()) {
            return;
        }
        begin(asked, BEAT_MS, true);
    }

    public static void hold(Palette asked, int ticks) {
        begin(asked, ticks * 50L, false);
    }

    private static void begin(Palette asked, long lastsMs, boolean snap) {
        if (!JojohaClientConfig.PALETTE_SWAPS.on()) {
            return;
        }
        long now = Util.getMillis();
        boolean showing = palette != null && now < until;
        if (showing && palette == asked) {
            until = Math.max(until, now + lastsMs);
            return;
        }
        if (showing && !snap) {
            return;
        }
        palette = asked;
        until = now + lastsMs;
        sting(asked);
    }

    public static void clear() {
        palette = null;
    }

    private static float strength() {
        if (palette == null || Minecraft.getInstance().level == null) {
            return 0F;
        }
        long over = Util.getMillis() - until;
        if (over < 0L) {
            return JojohaClientConfig.PALETTE_STRENGTH.value();
        }
        if (over >= PACK_FADE_MS || !ShaderPacks.active()) {
            palette = null;
            return 0F;
        }
        float left = 1F - (float) over / PACK_FADE_MS;
        return JojohaClientConfig.PALETTE_STRENGTH.value() * left * left;
    }

    public static void render() {
        ShaderInstance shader = JojohaShaders.palette();
        if (shader == null) {
            return;
        }

        float strength = strength();
        if (strength <= 0.001F || palette == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.setSampler("PaletteSampler", minecraft.getTextureManager().getTexture(TEXTURES[palette.ordinal()]).getId());
        shader.safeGetUniform("Swap").set(strength, 0F, 0F, 0F);

        PASS.draw(shader);
    }
}
