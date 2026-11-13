package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.grapple.GrappleAim;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class HermitGrappleSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hermit_grapple");

    public static final HermitGrappleSkill INSTANCE = new HermitGrappleSkill();

    private static final int COOLDOWN_TICKS = 0;
    private static final float ENERGY_COST = 0F;

    private HermitGrappleSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.hermit_grapple";
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
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(Player player) {
        return HermitGrappleHook.findSwing(player) != null;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();

        HermitGrappleHook existing = HermitGrappleHook.findSwing(player);
        if (existing != null) {
            for (Entity entity : player.serverLevel().getAllEntities()) {
                if (entity instanceof HermitGrappleHook other
                        && other.holder() == player && !other.isZip()) {
                    other.discard();
                }
            }
            level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                    SoundSource.PLAYERS, 0.4F, 1.6F);
            return true;
        }

        BlockHitResult anchor =
                GrappleAim.find(player);
        if (anchor == null) {
            return false;
        }

        HermitGrappleHook hook = new HermitGrappleHook(level, player, anchor.getLocation());
        level.addFreshEntity(hook);

        level.playSound(null, player.blockPosition(), ModSounds.STAND_HIT.get(),
                SoundSource.PLAYERS, 0.55F, 1.25F);

        MovementFx.hold(player, MovementFx.THORN_ZIP_START);
        return true;
    }
}
