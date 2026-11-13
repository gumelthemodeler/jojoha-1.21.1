package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.HierophantClips;
import org.gumel.jojoha.stand.hierophant.HierophantTraps;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class EmeraldRadiusSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "twenty_meter_emerald_splash");
    public static final EmeraldRadiusSkill INSTANCE = new EmeraldRadiusSkill();

    public static final double RADIUS = 20.0;
    private static final int LASTS_TICKS = 20 * 20;

    private static final int COOLDOWN_TICKS = 1200;
    private static final float ENERGY_COST = EnergyWeight.ULTIMATE.cost();

    private EmeraldRadiusSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.twenty_meter_emerald_splash";
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
        ServerLevel level = player.serverLevel();
        Vec3 centre = StandAim.feet(player).add(0.0, 1.0, 0.0);
        stand.triggerStrikeForward(HierophantClips.RADIUS);
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.5F);
        StandBeat.at(HierophantClips.RADIUS_CAST_TICKS, player, () -> {
            HierophantTraps.field(level, player, centre, RADIUS, LASTS_TICKS);
            level.playSound(null, BlockPos.containing(centre), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        });
        return true;
    }
}
