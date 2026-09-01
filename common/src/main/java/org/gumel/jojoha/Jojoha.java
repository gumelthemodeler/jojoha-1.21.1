package org.gumel.jojoha;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.command.JojohaCommands;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.TimeStopCast;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;

public final class Jojoha {
    public static final String MOD_ID = "jojoha";

    /**
     * Longer than any cooldown in the mod, and the mark of one written by another world's clock.
     *
     * <p>Twenty minutes. The longest thing here is the time stop's exhaustion lockout at about a
     * minute and a half, so there is an order of magnitude between a real cooldown and a suspicious
     * one - which is what makes this safe to apply without knowing anything about the move.
     */
    private static final long FOREIGN_COOLDOWN_TICKS = 24000L;

    public static void init() {
        org.gumel.jojoha.stand.grapple.GrappleGrace.init();
        org.gumel.jojoha.stand.skill.moves.TwistingGutPunchSkill.init();
        org.gumel.jojoha.stand.skill.moves.VineHaul.init();
        ModRegistries.init();
        HamonPaths.bootstrap();
        StandTypes.bootstrap();
        NetworkHandler.init();
        JojohaCommands.init();
        EnergySystem.init();
        org.gumel.jojoha.combat.VampireTraits.init();
        StandArrowRitual.init();
        org.gumel.jojoha.item.StoneMaskRitual.init();
        org.gumel.jojoha.item.MaskBlood.init();
        TimeStopSystem.init();
        TimeStopCast.init();
        StandSkills.init();
        StandPassives.init();

        // The client is told who it is on the way in, and again after dying.
        //
        // Keeping the data through a death is only half the job: a respawn builds a new player
        // entity and the client's cache is not rebuilt with it, so without this the server would
        // hold a fully-statted player while their own screen showed an empty bar, no spec and no
        // Stand - which looks exactly like the data having been lost, and is the half of the bug
        // that copyOnDeath cannot reach. The join case is the same thing for a fresh connection.
        org.gumel.jojoha.stand.StandUtilityWork.init();
        org.gumel.jojoha.stand.skill.StandBeat.init();

        PlayerEvent.PLAYER_JOIN.register(player -> {
            // A Stand never survives the gap between sessions, but the flag that says one is out
            // does: standSummoned is in the codec and the entity uuid that would find it is not.
            // A clean quit clears the flag; a crash or a killed process does not, and the player
            // comes back with the game convinced their Stand is out. Nothing is drawn, energy
            // drains for a Stand that is not there, and everything that needs to find the entity -
            // item delegation, moves, the guard - fails without saying why.
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.standSummoned = false;
            data.summonedStandEntityUuid = null;
            data.standGuarding = false;

            // And the one that was missing, which cost a player their legs. Piloting is saved with
            // the rest of the session but describes a camera attached to an entity that no longer
            // exists, so a player who quit mid-flight came back still flying: the client suppresses
            // the body's own movement while piloting - see KeyboardInputMixin - and hands it to a
            // Stand there is nothing left to hand it to. Nothing moved, and nothing said why.
            data.standPiloting = false;
            org.gumel.jojoha.stand.StandUtilityWork.forget(player.getUUID());

            // Cooldowns are saved as absolute game times, and a game time only means anything in
            // the world that wrote it. Carried into a different world - a different save, a server
            // after singleplayer - the same number is either long past or centuries away, and the
            // second of those locks a move out forever.
            //
            // Nothing here needs to know which case it is. A cooldown that has already expired is
            // finished, and one further off than any cooldown in the mod could legitimately be was
            // written by a clock this world does not share.
            long now = player.level().getGameTime();
            data.moveCooldowns.entrySet().removeIf(entry -> {
                long left = entry.getValue() - now;
                return left <= 0L || left > FOREIGN_COOLDOWN_TICKS;
            });

            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            // Whatever was out belongs to the body that died. Cleared before the sync so the client
            // is not told it still has a Stand standing somewhere in the world it just left.
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.standSummoned = false;
            data.summonedStandEntityUuid = null;
            data.standGuarding = false;
            data.standPiloting = false;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        });

        // Clean up a summoned Stand if its owner disconnects, rather than leaving it orphaned.
        PlayerEvent.PLAYER_QUIT.register(player -> {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (data.standSummoned) {
                // Immediate, not the usual fade - the owner is leaving, so there's nobody left
                // for it to dissolve in front of.
                StandSummonHandler.dismissImmediately(player, data);
                PlayerDataAccess.set(player, data);
            }
        });

        // Stand blocking (absorbs a hit, restores energy, tracks toward a guard break) and
        // non-stand-form damage (also restores energy) - see the design doc's Stand Balance.
        // Datapack-tunable numbers. Registered against SERVER_DATA so a pack can change them
        // and /reload picks it up without a restart.
        dev.architectury.registry.ReloadListenerRegistry.register(
                net.minecraft.server.packs.PackType.SERVER_DATA,
                new org.gumel.jojoha.data.StandTuning(),
                org.gumel.jojoha.data.StandTuning.ID);

        EntityEvent.LIVING_HURT.register(StandCombatHandler::handleLivingHurt);
        org.gumel.jojoha.data.StatProgression.init();

        // A player can't melee-attack their own Stand. Other players'/mobs' attacks on it are
        // untouched - this only blocks the owner hitting the thing following them around.
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            if (entity instanceof StandEntity stand && stand.getOwner() == player) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }
}
