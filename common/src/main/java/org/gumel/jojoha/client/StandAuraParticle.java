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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;

/**
 * A tongue of Stand aura clinging to its user.
 *
 * <p>Unlike an ordinary particle this does not live in the world - it keeps an offset relative to
 * the player and re-derives its absolute position from them every tick. That is what makes the
 * aura stay welded to the body while they run: a particle that merely starts at the player and
 * then coasts on its own velocity gets left behind the instant they move, which reads as smoke
 * trailing off them rather than as an aura burning on them.
 *
 * <p>The offset itself climbs and narrows over the particle's life, so each mote licks upward and
 * tapers like a flame instead of drifting in a straight line.
 */
public final class StandAuraParticle extends TextureSheetParticle {
    /** How far a mote may be from a player's centre and still be considered theirs. */
    private static final double OWNER_SEARCH_RADIUS = 2.0;

    private static final double RISE_PER_TICK = 0.055;
    /** Under 1, so the column pinches inward as it rises - the taper that makes it read as flame. */
    private static final double TAPER_PER_TICK = 0.93;

    private final SpriteSet sprites;
    private final Player owner;

    private double localX;
    private double localY;
    private double localZ;

    private StandAuraParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.gravity = 0F;
        this.hasPhysics = false;

        // Bound by proximity because a SimpleParticleType carries no payload to name an owner in.
        // Motes are spawned inside their player's own silhouette, so the nearest player is the
        // right one in every case that matters; a miss would only pick a body pressed against them.
        this.owner = level.getNearestPlayer(x, y, z, OWNER_SEARCH_RADIUS, false);
        if (this.owner != null) {
            this.localX = x - owner.getX();
            this.localY = y - owner.getY();
            this.localZ = z - owner.getZ();
        }

        this.quadSize = 0.34F + this.random.nextFloat() * 0.16F;
        this.lifetime = 10 + this.random.nextInt(7);

        // Tinted from the local player's own Stand rather than hardcoded, so the aura and the
        // outline glow always agree on what colour this Stand is.
        StandType stand = StandTypes.byIdOrDefault(ClientPlayerDataCache.data.stand.isPresent()
                ? ClientPlayerDataCache.data.stand.standId()
                : null);
        int skin = ClientPlayerDataCache.data.stand.skin();
        this.setColor(stand.auraRedFor(skin), stand.auraGreenFor(skin), stand.auraBlueFor(skin));
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

        // Previous position is rebuilt from the owner's own previous position, so the particle
        // interpolates between frames on exactly the same curve the player does. Without this it
        // would lerp from where the player used to be, and visibly lag a step behind them.
        this.xo = owner.xo + previousLocalX;
        this.yo = owner.yo + previousLocalY;
        this.zo = owner.zo + previousLocalZ;

        setSpriteFromAge(sprites);
    }

    /**
     * Never drawn on top of the camera in first person.
     *
     * <p>Spawning is already skipped for the local player while in first person, but that alone
     * isn't enough: motes created moments before the camera changed - or belonging to a player the
     * camera later attaches to - would otherwise hang in front of the view for the rest of their
     * lives. Checking at render time closes that off whatever route the particle arrived by.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (owner == minecraft.player && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }
        super.render(buffer, camera, partialTick);
    }

    /** Swells slightly then shrinks away, so motes dissipate rather than blinking out. */
    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0F, 1F);
        return this.quadSize * (1F - progress * progress) * 1.2F;
    }

    /** Additive, so overlapping motes build light instead of stacking into flat colour. */
    @Override
    public ParticleRenderType getRenderType() {
        return EmissiveParticleRenderType.INSTANCE;
    }

    /**
     * Full brightness regardless of world lighting - the same fixed value (240) vanilla's own
     * SoulParticle.EmissiveProvider uses, rather than the default which samples block/sky light
     * at the particle's position and washes the tint out under normal lighting.
     */
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
            return new StandAuraParticle(level, x, y, z, sprites);
        }
    }
}
