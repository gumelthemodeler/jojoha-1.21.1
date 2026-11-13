package org.gumel.jojoha.vampire;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.fortune.Trickster;
import org.gumel.jojoha.hamon.moves.NextLineMove;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.pillar.PillarMoves;
import org.gumel.jojoha.skill.CastDing;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.ultimate.AdaptMove;
import org.gumel.jojoha.ultimate.AdaptedAbilityMove;
import org.gumel.jojoha.ultimate.AdaptiveFlightMove;
import org.gumel.jojoha.ultimate.UltimateStrengthMove;
import org.gumel.jojoha.vampire.moves.AmalgamateMove;
import org.gumel.jojoha.vampire.moves.BlitzMove;
import org.gumel.jojoha.vampire.moves.BloodBoilMove;
import org.gumel.jojoha.vampire.moves.DrainMove;
import org.gumel.jojoha.vampire.moves.FleshBudMove;
import org.gumel.jojoha.vampire.moves.RegenerationMove;
import org.gumel.jojoha.vampire.moves.SpaceRipperMove;
import org.gumel.jojoha.vampire.moves.SubzeroMove;
import org.gumel.jojoha.vampire.moves.VampiricDriveMove;
import org.gumel.jojoha.vampire.moves.VaporousFreezeMove;
import org.gumel.jojoha.vampire.moves.WallCrawlMove;
import org.gumel.jojoha.vampire.moves.ZombifyMove;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VampireMoves {

    private static final Map<ResourceLocation, VampireMove> MOVES = new LinkedHashMap<>();

    static {
        register(VampiricDriveMove.INSTANCE);
        register(BlitzMove.INSTANCE);
        register(DrainMove.INSTANCE);
        register(SubzeroMove.INSTANCE);
        register(SpaceRipperMove.INSTANCE);
        register(FleshBudMove.INSTANCE);
        register(ZombifyMove.INSTANCE);
        register(AmalgamateMove.INSTANCE);
        register(WallCrawlMove.INSTANCE);
        register(VaporousFreezeMove.INSTANCE);
        register(RegenerationMove.INSTANCE);
        register(BloodBoilMove.INSTANCE);
        for (VampireMove move : PillarMoves.ALL) {
            register(move);
        }
        register(UltimateStrengthMove.INSTANCE);
        register(AdaptiveFlightMove.INSTANCE);
        register(AdaptMove.INSTANCE);
        register(AdaptedAbilityMove.INSTANCE);
        register(AdaptedAbilityMove.SECOND);
        register(AdaptedAbilityMove.THIRD);
    }

    private VampireMoves() {
    }

    private static void register(VampireMove move) {
        MOVES.put(move.id(), move);
    }

    public static Collection<VampireMove> all() {
        return MOVES.values();
    }

    public static VampireMove byId(ResourceLocation id) {
        return MOVES.get(id);
    }

    public static boolean unlocked(JojohaPlayerData data, VampireMove move) {
        if (move.pillar()) {
            return Fate.pillar(data);
        }
        if ((data.vampireStage == VampireStage.ULTIMATE_LIFEFORM) != (move.minimumStage() == VampireStage.ULTIMATE_LIFEFORM)) {
            return false;
        }
        return data.vampireStage.ordinal() >= move.minimumStage().ordinal();
    }

    public static boolean handleUseRequest(ServerPlayer player, ResourceLocation moveId) {
        return handleUseRequest(player, moveId, 0);
    }

    public static boolean handleUseRequest(ServerPlayer player, ResourceLocation moveId,
                                           int charge) {
        VampireMove move = byId(moveId);
        if (move == null) {
            return false;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.vampireStage == VampireStage.NONE) {
            return true;
        }

        if (move.isSustained()
                && (charge == StandSkills.RELEASE
                        || move.isSustainActive(player))) {
            move.release(player, data);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return true;
        }
        if (!unlocked(data, move)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.vampire.too_young"), true);
            return true;
        }

        if (!SkillTrees.skillUnlocked(data, moveId)) {
            return true;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(moveId, now)) {
            return true;
        }
        float cost = move.energyCost(data);
        if (data.specEnergy < cost) {
            return true;
        }

        String unavailable = PillarMen.petrified(player)
                ? "message.jojoha.pillar.petrified"
                : Headless.is(player) && move != ZombifyMove.INSTANCE
                ? "message.jojoha.vampire.headless" : move.unavailable(player, data);
        if (unavailable != null) {
            player.displayClientMessage(Component.translatable(unavailable), true);
            return true;
        }

        data.setMoveCooldown(moveId, now, move.cooldownTicks(data));
        data.specEnergy -= cost;

        move.activate(player, data);
        CastDing.play(player);
        NextLineMove.cast(player, moveId);
        Trickster.cast(player, moveId);

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }
}
