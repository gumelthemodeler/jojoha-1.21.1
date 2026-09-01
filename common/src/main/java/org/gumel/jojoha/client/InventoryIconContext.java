package org.gumel.jojoha.client;

/**
 * Whether an item is being drawn into a slot right now.
 *
 * <p>Exists because an item's model is chosen before anything knows what it is being drawn for.
 * Minecraft resolves a stack to a model through {@code ItemOverrides}, and an override is a
 * predicate over the <em>stack</em> - what it is, how damaged it is, whether it is being pulled -
 * never over where the result is about to appear. So a model has no way to ask "am I in an
 * inventory", which is exactly the question that decides whether these items want their flat icon
 * or their authored geometry.
 *
 * <p>This carries the answer across that gap: the renderer knows the display context, sets it here
 * on the way in, and the override predicate reads it a moment later during model resolution. A flag
 * rather than an argument because the call between the two is vanilla's and cannot be given one.
 *
 * <p>Saved and restored rather than set and cleared. Item rendering nests - an item frame inside a
 * world drawn behind an open inventory screen - and a nested draw that reset the flag on the way out
 * would leave the outer one lying about where it was.
 */
public final class InventoryIconContext {
    /**
     * Render-thread only, so a plain field is enough.
     *
     * <p>Everything that reads or writes this runs inside one item draw on the render thread, and
     * the pairs are balanced by the mixin that sets them.
     */
    private static boolean inSlot;

    private InventoryIconContext() {
    }

    /** @return the previous value, to hand back to {@link #restore(boolean)}. */
    public static boolean push(boolean nowInSlot) {
        boolean previous = inSlot;
        inSlot = nowInSlot;
        return previous;
    }

    public static void restore(boolean previous) {
        inSlot = previous;
    }

    /** Read by the model override predicate - see ModItemProperties. */
    public static boolean inSlot() {
        return inSlot;
    }
}
