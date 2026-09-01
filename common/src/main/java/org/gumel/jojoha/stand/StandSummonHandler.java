package org.gumel.jojoha.stand;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.slf4j.Logger;

/** Server-side summon/dismiss logic behind the Summon/Dismiss Stand keybind. */
public final class StandSummonHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StandSummonHandler() {
    }

    public static void handleToggleRequest(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            LOGGER.info("[jojoha] {} pressed summon but has no Stand - ignoring", player.getGameProfile().getName());
            return;
        }

        TrustTier tier = data.stand.trust();

        if (data.standSummoned) {
            // Withdrawing is always the user's call, at every Trust Tier. The tiers limit how
            // much Stand you get and how long it can hold form on its own - not whether you're
            // allowed to put it away.
            LOGGER.info("[jojoha] {} ending Stand cast", player.getGameProfile().getName());
            dismiss(player, data);
        } else {
            // A DORMANT Stand still answers the cast - it just never takes form, so it costs no
            // energy to hold and there's nothing for an empty pool to cut short.
            if (tier.manifestsEntity() && data.standEnergy <= 0) {
                LOGGER.info("[jojoha] {} pressed summon but standEnergy is 0 - ignoring", player.getGameProfile().getName());
                return;
            }
            LOGGER.info("[jojoha] {} casting Stand {} at trust tier {}",
                    player.getGameProfile().getName(), data.stand.standId(), tier);
            summon(player, data);
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /**
     * Raises the cast. At DORMANT that's all it is - the aura answers (see the standSummoned gate
     * in {@code EnergySystem}/{@code LocalStandAuraEffect}) but no entity is ever spawned, so
     * {@code summonedStandEntityUuid} stays null and every downstream lookup simply finds nothing.
     */
    private static void summon(ServerPlayer player, JojohaPlayerData data) {
        data.standSummoned = true;
        data.summonedStandEntityUuid = null;

        // First arg is the player to SKIP, not the player to play for: the caster is excluded so
        // their client can play a volume-ramped copy instead (StandSummonSound), which a
        // server-side sound can't do. Everyone else hears it plainly, as before.
        player.serverLevel().playSound(player, player.blockPosition(), ModSounds.STAND_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!data.stand.trust().manifestsEntity()) {
            return;
        }

        StandEntity stand = new StandEntity(ModRegistries.STAND.get(), player.serverLevel());
        stand.setOwner(player);
        stand.setStandType(data.stand.standId());
        stand.setTrustTier(data.stand.trust());
        stand.setSkin(data.stand.skin());
        // Carries the stance over, so a Stand summoned while set to DEFENDING comes out braced
        // rather than starting passive and only switching on the next toggle.
        stand.setMode(data.standMode);
        stand.moveTo(player.getX(), player.getY() + 0.5, player.getZ(), player.getYRot(), 0F);
        player.serverLevel().addFreshEntity(stand);
        // The "spawn" animation trigger fires from the entity's own first tick, not here - see
        // StandEntity.tick()'s comment for why triggering it immediately on spawn is unreliable.

        data.summonedStandEntityUuid = stand.getUUID();

        // The Stand announcing itself, if it is the sort that does.
        //
        // Separate from the summon whoosh, which is the manifestation; this is the name being
        // called with it - and it is the Stand's own name, so it comes off the Stand rather than
        // being written here. Star Platinum's cry used to play for every Stand in the game, which
        // was invisible while there was only one and became a shout of somebody else's name the
        // moment there were two.
        java.util.function.Supplier<net.minecraft.sounds.SoundEvent> voice =
                org.gumel.jojoha.stand.StandTypes.byIdOrDefault(data.stand.standId()).voice();
        if (voice != null && voice.get() != null) {
            player.serverLevel().playSound(null, player.blockPosition(), voice.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * Raises the cast whether or not the player asked for it.
     *
     * <p>For effects that need the Stand present to happen to it - the skin arrow being the first,
     * which has to have something to turn white and blow apart. Does nothing if it is already out,
     * so a caller can say "be summoned" rather than having to find out first.
     *
     * <p>Deliberately not routed through {@link #handleToggleRequest}: that reads a keypress and
     * would put the Stand <em>away</em> if it happened to be out already, which is the exact
     * opposite of what every caller of this wants.
     */
    public static void forceSummon(ServerPlayer player, JojohaPlayerData data) {
        if (!data.stand.isPresent()) {
            return;
        }

        // Asks whether a Stand is actually standing there, not whether a flag says one is. Those
        // two can disagree: standSummoned is written to disk but summonedStandEntityUuid is not,
        // so a session that ended without a clean quit - a crash, a force-kill - comes back with
        // the flag set and no entity and no way to find one. Trusting the flag there meant this
        // returned without summoning anything and every caller silently did nothing.
        if (data.standSummoned && findStand(player, data) != null) {
            return;
        }

        // Cleared first, because summon() is what sets them and it would otherwise be building a
        // second Stand on top of a half-recorded first.
        data.standSummoned = false;
        data.summonedStandEntityUuid = null;
        summon(player, data);
    }

    /** The player's currently-summoned Stand entity, or null if there isn't one. */
    public static StandEntity findStand(ServerPlayer player, JojohaPlayerData data) {
        if (data.summonedStandEntityUuid == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
        return entity instanceof StandEntity stand ? stand : null;
    }

    /**
     * Ends the cast: the Stand fades out and removes itself rather than vanishing on the spot
     * (see {@link StandEntity#beginDismissal}). The player's own state is cleared immediately, so
     * the still-dissolving entity is already disowned - re-casting mid-fade gives a fresh Stand
     * and leaves the old one to finish dissolving on its own.
     *
     * <p>Also used when Stand energy runs out or an EMERGING window expires (EnergySystem).
     */
    public static void dismiss(ServerPlayer player, JojohaPlayerData data) {
        // The vines go with the Stand that was holding them - see HermitGrappleHook.releaseAll.
        org.gumel.jojoha.stand.grapple.HermitGrappleHook.releaseAll(player);

        StandEntity stand = findStand(player, data);
        if (stand != null) {
            stand.beginDismissal();
        }

        if (data.standSummoned) {
            // Pitched down from the summon cue so the pair reads as the same sound answering and
            // then releasing. Unlike the summon, this plays for everyone including the caster
            // (null = exclude nobody): the fade-in trick the summon needs makes no sense for a
            // sound marking something ending, so there's no local copy to avoid doubling up with.
            player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_DISMISS.get(),
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        data.standSummoned = false;
        data.summonedStandEntityUuid = null;
    }

    /**
     * The cast breaking rather than being released - too much punishment absorbed, or the energy
     * gone.
     *
     * <p>Sounds different from an ordinary withdrawal on purpose. Both are the summon cue pitched
     * down, but a collapse drops it much further: putting a Stand away and having it forced out of
     * you should not be the same noise, and pitch alone carries that without a second recording.
     */
    public static void collapse(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        collapse(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /**
     * The same, for callers that are already holding the player's data.
     *
     * <p>Split the way {@link #dismiss} is, and for the same reason: a caller mid-update has a live
     * copy it intends to write back, and a version that fetched and saved its own would be undone
     * by that write a moment later.
     */
    public static void collapse(ServerPlayer player, JojohaPlayerData data) {
        if (!data.standSummoned) {
            return;
        }

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.STAND_DISMISS.get(),
                SoundSource.PLAYERS, 1.0F, COLLAPSE_PITCH);

        // Energy is emptied as well as the Stand withdrawn, so a collapse cannot be shrugged off by
        // re-casting immediately - it has to be regenerated like any other spent cast.
        data.standEnergy = 0F;
        dismiss(player, data);
    }

    /** Well below the withdrawal's own 0.8, so the two are never mistaken for each other. */
    private static final float COLLAPSE_PITCH = 0.45F;

    /**
     * Removes the Stand outright, skipping the fade. Used when there'll be nobody around to watch
     * it finish - the owner disconnecting - since a fading entity left behind would just linger
     * with a null owner until its next tick discarded it anyway.
     */
    public static void dismissImmediately(ServerPlayer player, JojohaPlayerData data) {
        // The vines go with the Stand that was holding them - see HermitGrappleHook.releaseAll.
        org.gumel.jojoha.stand.grapple.HermitGrappleHook.releaseAll(player);

        if (data.summonedStandEntityUuid != null) {
            Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
            if (entity != null) {
                entity.discard();
            }
        }
        data.standSummoned = false;
        data.summonedStandEntityUuid = null;
    }
}
