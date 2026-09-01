package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.stand.skill.moves.LassoOfThornsSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a thorn vine turning into a lead when it lets go.
 *
 * <p>Vanilla's leash assumes it came from a lead item, so releasing gives the item back. That is
 * right for the item and wrong for this move, which never took one - and the leash breaks on its own
 * whenever the mob wanders past ten blocks, so a lasso used normally quietly prints leads. Free
 * items out of a Stand ability is the sort of thing that ends up on a duplication list.
 *
 * <h2>Which leashes are ours</h2>
 *
 * <p>A tag on the mob, set when the vine goes round it. Tags are not synced to clients, which would
 * be a problem for anything visual, and is not one here: dropping happens entirely on the server, so
 * the only side that has to know is the side that already knows.
 *
 * <p>The tag is cleared as the vine comes off, so a mob later caught with a real lead behaves like
 * any other mob.
 */
@Mixin(Mob.class)
public abstract class LassoNoLeadMixin {
    @Inject(method = "dropLeash(ZZ)V", at = @At("HEAD"), cancellable = true)
    private void jojoha$noLead(boolean broadcast, boolean dropItem, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        if (!dropItem || !self.getTags().contains(LassoOfThornsSkill.LASSO_TAG)) {
            return;
        }

        // Same call, minus the item. Re-entering this injection is harmless: the guard above is on
        // dropItem, and it is false the second time through.
        self.removeTag(LassoOfThornsSkill.LASSO_TAG);
        self.dropLeash(broadcast, false);
        ci.cancel();
    }
}
