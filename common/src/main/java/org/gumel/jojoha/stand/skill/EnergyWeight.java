package org.gumel.jojoha.stand.skill;

/**
 * What a move costs to throw, as one of a handful of named weights.
 *
 * <p>The costs used to be eight unrelated numbers, one per move file, each picked when that move was
 * written and never compared against the others. That is how a barrage ended up at 14 and an
 * uppercut at 9 - not because a flurry is one and a half uppercuts, but because nobody was ever
 * looking at both at once. Naming the weights forces the comparison: a move does not get a number,
 * it gets a class, and if two moves belong in the same class they cost the same.
 *
 * <p>The scale is set against a hundred-point pool, which is what a Stand starts with:
 *
 * <dl>
 *   <dt>{@link #TRIVIAL}</dt>
 *   <dd>Not really a cost at all, and deliberately so. For the Stand doing something a pair of
 *   hands does - turning a valve, pouring a bucket, throwing a pearl. A hundred of them on a full
 *   bar, which is to say the bar is not what limits it; what limits it is that you have to have a
 *   Stand out to do it. Charging utility like combat would turn every convenience into a decision
 *   about whether the Stand can afford to be helpful, which is the wrong question to make a player
 *   ask twenty times an hour.</dd>
 *
 *   <dt>{@link #UPKEEP}</dt>
 *   <dd>Barely a cost. For things held rather than thrown, where the pressure is meant to come from
 *   the drain while it is running, not from the press that started it.</dd>
 *
 *   <dt>{@link #LIGHT}</dt>
 *   <dd>Movement and single blows. Twenty of them on a full bar - cheap enough to use as punctuation
 *   rather than as a decision.</dd>
 *
 *   <dt>{@link #STANDARD}</dt>
 *   <dd>The ordinary offensive moves, the flurries and the reaching attacks. A dozen on a full bar,
 *   which is the rhythm of a fight rather than a budget for one.</dd>
 *
 *   <dt>{@link #HEAVY}</dt>
 *   <dd>Moves that reshape the fight rather than damage it - a lungful of air that drags a field of
 *   mobs into one pile. Seven on a full bar.</dd>
 *
 *   <dt>{@link #ULTIMATE}</dt>
 *   <dd>Stopping time, and nothing else. Close to half the bar for a full hold, and the tap share
 *   still costs more than any other move in the kit - the point being that you feel it either way,
 *   and that throwing one is a thing you plan a fight around rather than something you do twice.</dd>
 * </dl>
 */
public enum EnergyWeight {
    TRIVIAL(1F),
    UPKEEP(3F),
    LIGHT(5F),
    STANDARD(8F),
    HEAVY(14F),
    ULTIMATE(45F);

    private final float cost;

    EnergyWeight(float cost) {
        this.cost = cost;
    }

    /** Stand energy spent on a successful cast. */
    public float cost() {
        return cost;
    }
}
