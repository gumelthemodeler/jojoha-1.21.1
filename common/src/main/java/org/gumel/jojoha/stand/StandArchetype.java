package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.passive.EnhancedReflexesPassive;
import org.gumel.jojoha.stand.passive.UnwaveringPassive;

import java.util.List;
import java.util.Locale;

/**
 * What kind of fight a Stand is built for.
 *
 * <p>Every Stand gets four passives: two from here, which it shares with every other Stand of its
 * kind, and two of its own. The split is the point. A Stand's identity is supposed to be its own two
 * - Star Platinum's reach at point blank, its eye for what is worth opening - and if every Stand
 * carried four unique passives then nothing would be shared, every new Stand would be a fresh
 * balance problem, and there would be no such thing as knowing roughly what you are facing.
 *
 * <p>This is deliberately not {@link StandRange}. Range answers "how far can it reach", which
 * decides which generic moves it is handed; this answers "what does it do with the reach", which
 * decides how it fights. A long-armed Stand that exists to rearrange the ground is CONTROL at
 * LONG range, and collapsing the two would make that Stand impossible to describe.
 *
 * <h2>The three empty ones</h2>
 *
 * <p>Only BRAWLER has passives, because only BRAWLER has a Stand. The other three are declared
 * because the taxonomy is the design and writing it down is most of the value - a new Stand arrives
 * knowing which shelf it belongs on. Their passives are left empty rather than guessed at: a passive
 * invented to fill a slot is a balance decision made by nobody, and {@link StandType#allPassives()}
 * simply returns the Stand's own two until someone fills them in.
 */
public enum StandArchetype {
    /**
     * Close ranged and defensive, built around combos and melee pressure.
     *
     * <p>Its two passives are the ones every brawler needs to be allowed near an enemy at all:
     * something that keeps it standing when it gets there, and something that keeps it from being
     * punished for the reach.
     */
    BRAWLER(List.of(EnhancedReflexesPassive.ID, UnwaveringPassive.ID)),

    /** Long ranged: zoning, projectiles, and keeping the fight at a distance it chose. */
    RANGED(List.of()),

    /** Changes the ground the fight is fought on rather than dealing with the fighter. */
    CONTROL(List.of()),

    /**
     * Its own rules - information, dimension, and mechanics the other three do not have.
     *
     * <p>Both of these are statements about the kind of Stand rather than about any one of them. A
     * specialist is not built to trade blows, so it neither levels by winning fights nor folds the
     * first time somebody poisons it - and that will be as true of the next specialist as it is of
     * Hermit Purple. Filed here so the next one inherits them instead of copying them.
     */
    SPECIALIST(List.of(org.gumel.jojoha.stand.passive.MadeForThisPassive.ID,
            org.gumel.jojoha.stand.passive.UnorthodoxMethodPassive.ID));

    private final List<ResourceLocation> passives;

    StandArchetype(List<ResourceLocation> passives) {
        this.passives = passives;
    }

    /** The two every Stand of this kind is granted, before its own. */
    public List<ResourceLocation> passives() {
        return passives;
    }

    public String translationKey() {
        return "archetype.jojoha." + name().toLowerCase(Locale.ROOT);
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }
}
