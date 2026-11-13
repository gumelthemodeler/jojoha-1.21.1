package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.supporter.SupporterAuras;

public final class StandAuraParticle extends TextureSheetParticle {
    private static final double OWNER_SEARCH_RADIUS = 2.0;

    private static final double RISE_PER_TICK = 0.055;
    private static final double TAPER_PER_TICK = 0.93;

    private static final float PIXEL_SIZE = 0.5F;
    private static final int FADE_TICKS = 4;

    private final SpriteSet sprites;
    private final Player owner;

    private StandShadowForm shadow;

    private boolean dark;

    private double localX;
    private double localY;
    private double localZ;

    private StandAuraParticle(ClientLevel level, double x, double y, double z,
                              double red, double green, double blue, SpriteSet sprites,
                              boolean hamon) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;

        this.owner = level.getNearestPlayer(x, y, z, OWNER_SEARCH_RADIUS, false);
        if (this.owner != null) {
            this.localX = x - owner.getX();
            this.localY = y - owner.getY();
            this.localZ = z - owner.getZ();
        }

        this.quadSize = PIXEL_SIZE;
        this.lifetime = 10 + this.random.nextInt(7);

        if (hamon) {
            this.setColor(HamonFx.RED,
                    HamonFx.GREEN,
                    HamonFx.BLUE);
        } else {
            tint(red, green, blue);

            this.shadow = shadowOf(this.owner);
            if (this.shadow != null) {
                shadeByHeight();
                this.dark = this.shadow.dark();
            } else {
                this.dark = StandShadowForm.dark(
                        (Math.round(this.rCol * 255F) << 16) | (Math.round(this.gCol * 255F) << 8)
                                | Math.round(this.bCol * 255F));
            }
        }
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        if (owner == null || !owner.isAlive() || this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        double previousLocalX = localX;
        double previousLocalY = localY;
        double previousLocalZ = localZ;

        localY += RISE_PER_TICK;
        localX *= TAPER_PER_TICK;
        localZ *= TAPER_PER_TICK;

        setPos(owner.getX() + localX, owner.getY() + localY, owner.getZ() + localZ);
        if (shadow != null) {
            shadeByHeight();
        }

        this.xo = owner.xo + previousLocalX;
        this.yo = owner.yo + previousLocalY;
        this.zo = owner.zo + previousLocalZ;

        this.alpha = Math.min(1F, (this.lifetime - this.age) / (float) FADE_TICKS);
        setSpriteFromAge(sprites);
    }

    private void tint(double red, double green, double blue) {
        if (red > 0.0 || green > 0.0 || blue > 0.0) {
            this.setColor((float) red, (float) green, (float) blue);
            return;
        }

        StandEntity out =
                this.owner == null ? null : StandEntity.findFor(this.owner);
        if (out != null) {
            int rgb = StandShadowForm.auraColourFor(out);
            this.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
            return;
        }

        StandType stand = StandTypes.byIdOrDefault(ClientPlayerDataCache.data.stand.isPresent()
                ? ClientPlayerDataCache.data.stand.standId()
                : null);
        int skin = ClientPlayerDataCache.data.stand.skin();
        int rgb = SupporterAuras.ownTint(stand.auraColorFor(skin));
        this.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
    }

    private static StandShadowForm shadowOf(Player owner) {
        if (owner == null) {
            return null;
        }
        StandEntity stand = StandEntity.findFor(owner);
        if (stand != null) {
            StandShadowForm form = StandShadowForm.of(stand);
            return form != null || SupporterAuras.chosen(owner.getUUID()) ? form
                    : StandShadowForm.gradientFor(stand.getStandType().id(), stand.getSkin(),
                            stand.getTrustTier());
        }
        if (owner == Minecraft.getInstance().player && ClientPlayerDataCache.data.stand.isPresent()) {
            StandShadowForm form = StandShadowForm.of(
                    ClientPlayerDataCache.data.stand.standId(), ClientPlayerDataCache.data.stand.skin(),
                    ClientPlayerDataCache.data.stand.trust());
            return form != null || SupporterAuras.chosen(owner.getUUID()) ? form
                    : StandShadowForm.gradientFor(ClientPlayerDataCache.data.stand.standId(),
                            ClientPlayerDataCache.data.stand.skin(), ClientPlayerDataCache.data.stand.trust());
        }
        return null;
    }

    private void shadeByHeight() {
        int rgb = shadow.colourAt((float) (localY / Math.max(0.5, owner.getBbHeight())));
        this.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (owner == minecraft.player && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }
        super.render(buffer, camera, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return dark ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : EmissiveParticleRenderType.INSTANCE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new StandAuraParticle(level, x, y, z, dx, dy, dz, sprites, false);
        }
    }

    public static final class HamonProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public HamonProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                        double dx, double dy, double dz) {
            return new StandAuraParticle(level, x, y, z, dx, dy, dz, sprites, true);
        }
    }
}
