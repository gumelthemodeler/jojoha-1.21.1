package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.grapple.GrappleAim;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class ThornZipSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thorn_zip");

    public static final ThornZipSkill INSTANCE = new ThornZipSkill();

    private static final int COOLDOWN_TICKS = 26;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private ThornZipSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.thorn_zip";
    }

    @Override
    public boolean movesTheUsersBody() {
        return true;
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
        ServerLevel level = player.serverLevel();

        if (HermitGrappleHook.findFor(player) != null) {
            return false;
        }

        BlockHitResult anchor = GrappleAim.find(player);
        if (anchor == null) {
            return false;
        }

        Vec3 at = GrappleAim.perch(level, anchor);

        level.addFreshEntity(HermitGrappleHook.zip(level, player, at, false));
        level.addFreshEntity(HermitGrappleHook.zip(level, player, at, true));

        level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.7F, 1.5F);

        MovementFx.hold(player, MovementFx.WEB_ZIP_START);
        return true;
    }
}
