package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;
import org.gumel.jojoha.stand.hierophant.HierophantClips;
import org.gumel.jojoha.stand.hierophant.HierophantAim;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class EmeraldSplashShotgunSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "emerald_splash_shotgun_blast");
    public static final EmeraldSplashShotgunSkill INSTANCE = new EmeraldSplashShotgunSkill();

    private static final int SHARDS = 3;
    private static final float DAMAGE = 6F;
    private static final float SPEED = 2.8F;
    private static final double SPREAD = 4.0;
    private static final double REACH = 32.0;

    private static final int RELEASE_TICKS = HierophantClips.SHOTGUN_RELEASE_TICKS;

    private static final int COOLDOWN_TICKS = 110;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private EmeraldSplashShotgunSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.emerald_splash_shotgun_blast";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return false;
        }
        float damage = Math.min(10F, StandTuning.damage("emerald_splash_shotgun_blast", DAMAGE) * data.stand.powerScale());
        stand.triggerStrikeForward(HierophantClips.SHOTGUN);
        ServerLevel level = player.serverLevel();
        level.playSound(null, stand.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.8F, 1.6F);

        StandBeat.at(RELEASE_TICKS, player, () -> {
            Vec3 from = HierophantAim.hands(player, stand);
            Vec3 heading = HierophantAim.crosshair(player, REACH).subtract(from).normalize();
            int shards = SHARDS + Relics.extraEmeralds(player);
            for (int i = 0; i < shards; i++) {
                double off = (i - (shards - 1) / 2.0) * SPREAD * Math.PI / 180.0;
                EmeraldSplash.fire(level, player, from, HierophantAim.turn(heading, new Vec3(0, 1, 0), off),
                        SPEED, 0.3F, damage, false).wounding();
            }
            EmeraldSplash.muzzle(level, player, from, heading, shards);
            level.playSound(null, stand.blockPosition(), ModSounds.HG_EMERALD_SPLASH.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, stand.blockPosition(), ModSounds.HG_SPLASH.get(), SoundSource.PLAYERS,
                    1.0F, 0.9F);
        });
        return true;
    }
}
