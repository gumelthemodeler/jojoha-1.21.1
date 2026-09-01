package org.gumel.jojoha.item;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.registry.ModItems;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.gumel.jojoha.registry.ModRegistries;

/**
 * A dagger in flight.
 *
 * <p>Built on {@link AbstractArrow} for the same reason a trident is: everything a thrown weapon has
 * to do is already there and already correct. Sticking in the block it lands in, surviving in the
 * world until somebody walks over it, being picked up back into the inventory it came from,
 * remembering who threw it so that person is not hit by their own weapon, and taking its damage from
 * how fast it was travelling - none of that is dagger-specific, and hand-rolling any of it would be
 * writing a worse version of code that ships with the game.
 *
 * <p>What is dagger-specific is that the stack itself is carried. A thrown dagger is not a generic
 * projectile that happens to drop an item on landing - it <em>is</em> the item, mid-air, with its
 * damage, its enchantments and its remaining durability intact, and what you pick up is the same
 * one you threw. {@link AbstractArrow}'s pickup stack is exactly that, so it is handed the real
 * stack rather than a fresh copy.
 *
 * <p>No loyalty, no riptide, no channeling. Those are a trident's, and a dagger that came back on
 * its own would remove the only cost the throw has - that you are now unarmed until you fetch it.
 */
public class ThrownDagger extends AbstractArrow {
    /**
     * Which dagger this is, for the client to draw.
     *
     * <p>Synced, and it has to be its own field, because the stack {@link AbstractArrow} already
     * carries is <em>not</em> sent to the client. It is a plain private field written to NBT for
     * the world save; the only things that class synchronises are its flags and its pierce level.
     * Vanilla never notices, because a trident is drawn from a fixed model and never asks what item
     * it is - but a dagger is drawn <em>as</em> its item, so on the client every thrown dagger fell
     * through to {@link #getDefaultPickupItem()} and flew as whatever that returned.
     */
    private static final EntityDataAccessor<ItemStack> DATA_DAGGER =
            SynchedEntityData.defineId(ThrownDagger.class, EntityDataSerializers.ITEM_STACK);

    /**
     * How hard a landing dagger is to knock loose, and how long it waits before it can be collected.
     *
     * <p>Inherited behaviour otherwise; this only exists so the numbers have somewhere to be read.
     */
    private static final float DEFAULT_DAMAGE = 4.0F;

    public ThrownDagger(EntityType<? extends ThrownDagger> type, Level level) {
        super(type, level);
    }

    public ThrownDagger(Level level, LivingEntity thrower, ItemStack thrown, float damage) {
        // The stack is passed as both the pickup item and the weapon: it is what lands on the
        // ground and it is also what the hit is credited to, which is what lets enchantments on the
        // dagger apply to the throw the same way they do to a swing.
        super(ModRegistries.THROWN_DAGGER.get(), thrower, level, thrown.copy(), thrown.copy());
        this.setBaseDamage(damage);

        // AbstractArrow defaults this to DISALLOWED, which for a weapon you are meant to go and
        // fetch means the throw destroys it. A trident sets ALLOWED in its own constructor for
        // exactly this reason and so does this.
        this.pickup = Pickup.ALLOWED;
        this.entityData.set(DATA_DAGGER, thrown.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DAGGER, ItemStack.EMPTY);
    }

    /**
     * The dagger to draw, on either side.
     *
     * <p>Falls back to the server's own copy rather than to nothing, so a dagger loaded from a world
     * save - where the synced value has not arrived yet but the NBT one has - still draws.
     */
    public ItemStack displayStack() {
        ItemStack synced = this.entityData.get(DATA_DAGGER);
        return synced.isEmpty() ? this.getPickupItemStackOrigin() : synced;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_DAGGER, this.getPickupItemStackOrigin().copy());
    }

    /**
     * What a dagger with no stack of its own falls back to.
     *
     * <p>Only reached for an instance built without one - a command spawn, or a broken save. It was
     * an iron <em>sword</em>, which was harmless as a fallback and highly visible as a symptom: with
     * the real stack never reaching the client, this was what every thrown dagger was drawn as.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.DAGGER_IRON.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    /**
     * The hit, minus the arrow it would otherwise leave sticking out of them.
     *
     * <p>{@code AbstractArrow.onHitEntity} raises the victim's arrow count, and vanilla draws that
     * count as literal arrow models jutting from the body - which is why testers reported daggers
     * "turning into arrows" once one landed on a player. It is not the dagger that changed; it is
     * the quiver of arrows the base class hung on the target.
     *
     * <p>Put back rather than prevented, because the increment sits in the middle of the base
     * method alongside the damage, the knockback and the enchantment effects, all of which a dagger
     * does want. Taking one number back afterwards is a smaller and safer thing than reimplementing
     * the rest to avoid it.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        int before = result.getEntity() instanceof LivingEntity hit ? hit.getArrowCount() : 0;

        super.onHitEntity(result);

        if (result.getEntity() instanceof LivingEntity hit) {
            hit.setArrowCount(before);
        }

        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    /**
     * Only its thrower may pick it up.
     *
     * <p>The ownership test comes <em>first</em>, and the order is the whole of it. The base
     * implementation does not report whether a pickup would be allowed - under
     * {@link Pickup#ALLOWED} it puts the stack into the player's inventory and returns whether that
     * worked. Asking it before checking ownership therefore hands the dagger to whoever walked into
     * it and only then decides they were not entitled to it, and since the entity is removed on a
     * true return and left alone on a false one, the dagger ends up both in their inventory and
     * still lying on the ground. Short-circuiting the other way round is what stops that.
     */
    @Override
    protected boolean tryPickup(Player player) {
        return this.ownedBy(player) && super.tryPickup(player);
    }

    /** What the hit is credited to, so the dagger's own enchantments count. */
    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    /** The damage a dagger with no tier information falls back to. */
    public static float defaultDamage() {
        return DEFAULT_DAMAGE;
    }
}
