package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.chariot.ChariotBlade;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class BladeLaunchSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "blade_launch");
    public static final BladeLaunchSkill INSTANCE = new BladeLaunchSkill();

    public static final int BLADE_LEAVES_TICKS = 18;
    private static final double AIM_REACH = 32.0;
    private static final double MUZZLE_AHEAD = 0.9;
    private static final double MUZZLE_HEIGHT = 0.62;

    private static final int COOLDOWN_TICKS = 240;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private BladeLaunchSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.blade_launch";
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
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        StandEntity stand = Chariot.stand(player);
        return stand != null && stand.bladeOut() ? Chariot.NO_RAPIER : Chariot.rapierless(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (!Chariot.is(stand) || stand.bladeOut()) {
            return false;
        }
        stand.triggerStrikeForward("blade_launch");
        StandBeat.at(BLADE_LEAVES_TICKS, player, () -> launch(player));
        return true;
    }

    private static void launch(ServerPlayer player) {
        StandEntity stand = Chariot.stand(player);
        if (stand == null || stand.bladeOut()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 muzzle = stand.position().add(0.0, stand.getBbHeight() * MUZZLE_HEIGHT, 0.0).add(flat.scale(MUZZLE_AHEAD));

        LivingEntity target = StandAim.lookTarget(player, AIM_REACH);
        Vec3 heading = target != null
                ? target.position().add(0.0, target.getBbHeight() * 0.6, 0.0).subtract(muzzle)
                : look;

        JojohaPlayerData data = PlayerDataAccess.get(player);
        float damage = Math.min(10F, StandTuning.damage("blade_launch", ChariotBlade.DAMAGE) * data.stand.powerScale());
        ChariotBlade blade = ChariotBlade.fire(player, stand, muzzle, heading, damage);
        stand.setBladeOut(true);
        Chariot.launched(player, blade);

        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS, 1.0F, 1.4F);
        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.7F, 1.6F);
    }
}
