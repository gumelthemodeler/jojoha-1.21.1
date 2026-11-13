package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class PilotSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pilot");
    public static final PilotSkill INSTANCE = new PilotSkill();

    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST = EnergyWeight.UPKEEP.cost();

    private PilotSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.pilot";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    private static final double LAUNCH_DISTANCE = 3.0;

    private static final double WALL_MARGIN = 0.6;

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (data.standPiloting) {
            PilotSystem.stop(player, data);
            return true;
        }

        PilotSystem.begin(player, data, stand);

        Vec3 ahead = player.getLookAngle();
        Vec3 from = new Vec3(player.getX(), player.getEyeY(), player.getZ());
        Vec3 out = from.add(ahead.scale(LAUNCH_DISTANCE));
        HitResult wall = player.level().clip(
                new ClipContext(from, out,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
        Vec3 at = wall.getType() == HitResult.Type.MISS
                ? out
                : wall.getLocation().subtract(ahead.scale(WALL_MARGIN));
        stand.setPos(at.x, at.y, at.z);

        stand.setDeltaMovement(Vec3.ZERO);

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.5F, 1.8F);
        return true;
    }
}
