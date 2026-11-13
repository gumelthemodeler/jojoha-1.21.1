package org.gumel.jojoha.vampire.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.family.FamilyLine;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.Headless;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMinions;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;

public final class ZombifyMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "zombify");
    public static final ZombifyMove INSTANCE = new ZombifyMove();

    private static final double REACH = 8.0;

    private static final int COOLDOWN_TICKS = 240;
    private static final float ENERGY_COST = 30F;

    private ZombifyMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
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
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();

        if (FamilyLine.takeOver(player, REACH)) {
            Headless.regrow(player, null);
            return;
        }

        if (VampireMinions.minionsOf(player).size() >= VampireMinions.BODY_CAP) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.vampire.minions_full",
                            VampireMinions.BODY_CAP), true);
            return;
        }

        Vec3 corpse = VampireMinions.nearestCorpse(player, REACH);
        if (corpse == null) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.vampire.no_corpse"), true);
            return;
        }

        VampireMinions.consumeCorpse(corpse);
        MovementFx.pose(player, VampirePoses.ZOMBIFY);
        VampireFx.ring(level, corpse.x, corpse.y + 0.15, corpse.z, 1.2, 1);
        StandBeat.after(VampirePoses.ZOMBIFY_RAISE_TICKS, player, player, (caster, self) -> rise(caster, corpse));
    }

    private static void rise(ServerPlayer player, Vec3 corpse) {
        ServerLevel level = player.serverLevel();
        if (!player.isAlive() || VampireMinions.minionsOf(player).size() >= VampireMinions.BODY_CAP) {
            return;
        }

        Zombie raised = VampireMinions.raise(player, corpse, VampireMinions.Tier.SERVANT);
        if (raised == null) {
            return;
        }

        for (double up = 0.0; up <= 2.0; up += 0.35) {
            VampireFx.blood(level, corpse.x, corpse.y + up, corpse.z, 6, 0.32, 0.03);
        }
        VampireFx.ring(level, corpse.x, corpse.y + 0.15, corpse.z, 1.8, 2);
        VampireFx.ring(level, corpse.x, corpse.y + 1.0, corpse.z, 1.2, 1);
        VampireFx.aura(level, corpse.x, corpse.y + 1.0, corpse.z, 16);
        level.playSound(null, raised.blockPosition(), ModSounds.VAMPIRE_REANIMATE.get(),
                SoundSource.PLAYERS, 0.8F, 0.6F);
    }
}
