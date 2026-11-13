package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.fortune.Trickster;
import org.gumel.jojoha.hamon.moves.BottleCapShotMove;
import org.gumel.jojoha.hamon.moves.BubbleCutterMove;
import org.gumel.jojoha.hamon.moves.BubbleLauncherMove;
import org.gumel.jojoha.hamon.moves.ChannelingMove;
import org.gumel.jojoha.hamon.moves.ClackerVolleyMove;
import org.gumel.jojoha.hamon.moves.ForcedRippleBreathingMove;
import org.gumel.jojoha.hamon.moves.HamonOverdriveMove;
import org.gumel.jojoha.hamon.moves.NextLineMove;
import org.gumel.jojoha.hamon.moves.RippleBarrierMove;
import org.gumel.jojoha.hamon.moves.RipplePulseMove;
import org.gumel.jojoha.hamon.moves.RippleRebukeMove;
import org.gumel.jojoha.hamon.moves.RippleWalkMove;
import org.gumel.jojoha.hamon.moves.ScarletOverdriveMove;
import org.gumel.jojoha.hamon.moves.SnakeMufflerMove;
import org.gumel.jojoha.hamon.moves.SunlightYellowOverdriveMove;
import org.gumel.jojoha.hamon.moves.ThunderCrossSplitAttackMove;
import org.gumel.jojoha.hamon.moves.TurquoiseBlueOverdriveMove;
import org.gumel.jojoha.hamon.moves.YouthfulBreathMove;
import org.gumel.jojoha.hamon.moves.ZoomPunchMove;
import org.gumel.jojoha.skill.CastDing;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.skill.StandSkills;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class HamonMoves {
    private static final Map<ResourceLocation, HamonMove> MOVES = new LinkedHashMap<>();

    static {
        register(RipplePulseMove.INSTANCE);
        register(ZoomPunchMove.INSTANCE);
        register(HamonOverdriveMove.INSTANCE);
        register(RippleBarrierMove.INSTANCE);
        register(SunlightYellowOverdriveMove.INSTANCE);
        register(ChannelingMove.INSTANCE);
        register(ClackerVolleyMove.INSTANCE);
        register(ForcedRippleBreathingMove.INSTANCE);
        register(BubbleCutterMove.INSTANCE);
        register(BubbleLauncherMove.INSTANCE);
        register(SnakeMufflerMove.INSTANCE);
        register(RippleWalkMove.INSTANCE);
        register(TurquoiseBlueOverdriveMove.INSTANCE);
        register(ScarletOverdriveMove.INSTANCE);
        register(ThunderCrossSplitAttackMove.INSTANCE);
        register(RippleRebukeMove.INSTANCE);
        register(NextLineMove.INSTANCE);
        register(YouthfulBreathMove.INSTANCE);
        register(BottleCapShotMove.INSTANCE);
    }

    private HamonMoves() {
    }

    private static void register(HamonMove move) {
        MOVES.put(move.id(), move);
    }

    public static Collection<HamonMove> all() {
        return MOVES.values();
    }

    public static HamonMove byId(ResourceLocation id) {
        return MOVES.get(id);
    }

    public static void handleUseRequest(ServerPlayer player, ResourceLocation moveId) {
        handleUseRequest(player, moveId, 0);
    }

    public static void handleUseRequest(ServerPlayer player, ResourceLocation moveId, int charge) {
        HamonMove move = byId(moveId);
        if (move == null) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.discipline != Discipline.HAMON) {
            return;
        }

        if (move.isSustained()
                && (charge == StandSkills.RELEASE
                        || move.isSustainActive(player))) {
            move.release(player, data);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        boolean known = data.getUnlockedHamonPaths().stream()
                .map(HamonPaths::byId)
                .filter(Objects::nonNull)
                .anyMatch(path -> path.moveIds().contains(moveId));
        if (!known) {
            return;
        }

        if (!SkillTrees.skillUnlocked(data, moveId)) {
            return;
        }

        if (Fate.barredFromHamon(data)) {
            HamonBacklash.burst(player);
            return;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(moveId, now)) {
            return;
        }
        if (data.specEnergy < move.energyCost()) {
            return;
        }

        if (!move.activate(player, data, Math.max(0, charge))) {
            return;
        }
        CastDing.play(player);
        NextLineMove.cast(player, moveId);
        Trickster.cast(player, moveId);

        data.setMoveCooldown(moveId, now, move.cooldownTicks());
        data.specEnergy -= move.energyCost();
        HamonMastery.spent(player, data, move.energyCost());

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }
}
