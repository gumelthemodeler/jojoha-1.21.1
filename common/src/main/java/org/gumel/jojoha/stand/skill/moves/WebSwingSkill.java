package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.grapple.GrappleAim;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class WebSwingSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "web_swing");
    public static final WebSwingSkill INSTANCE = new WebSwingSkill();

    private WebSwingSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.web_swing";
    }

    @Override
    public boolean movesTheUsersBody() {
        return true;
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(Player player) {
        HermitGrappleHook swing = HermitGrappleHook.findSwing(player);
        return swing != null && swing.isTendril();
    }

    @Override
    public float energyCost() {
        return 0F;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();

        HermitGrappleHook existing = HermitGrappleHook.findSwing(player);
        if (existing != null) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof HermitGrappleHook other && other.holder() == player && !other.isZip()) {
                    other.discard();
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS,
                    0.5F, 1.4F);
            return true;
        }

        BlockHitResult anchor = GrappleAim.find(player);
        if (anchor == null) {
            return false;
        }
        level.addFreshEntity(HermitGrappleHook.tendril(level, player, anchor.getLocation()));
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.6F, 1.3F);
        MovementFx.hold(player, MovementFx.THORN_ZIP_START);
        return true;
    }
}
