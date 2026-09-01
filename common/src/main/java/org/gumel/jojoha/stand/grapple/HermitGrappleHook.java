package org.gumel.jojoha.stand.grapple;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

/**
 * The thorned vine Hermit Purple throws, and the thing the swing hangs off.
 *
 * <p>A projectile until it lands and an anchor afterwards, which is why it is one entity rather than
 * two. The alternative - a flying hook that despawns and spawns a separate anchor where it stopped -
 * puts a frame between the two in which the rope has nowhere to be attached, and a rope that blinks
 * on arrival is the one thing a grapple cannot do.
 *
 * <h2>Why it extends Entity and not a projectile</h2>
 *
 * <p>{@code Projectile}'s constructor is not public outside its own package, so the choice was
 * {@code AbstractArrow} or nothing - and {@code AbstractArrow} is a poor fit on its own merits. It
 * arrives with pickup rules, damage, a shake animation and, worst of all, the habit of burying
 * itself a little way into whatever it hits. That last one is not cosmetic here: the rope is
 * measured to the hook, so a hook that sinks into the wall shortens the rope every time it lands,
 * and on a swing that is a visible jolt at the moment of attachment.
 *
 * <p>Moving and hit-testing by hand is about fifteen lines, and the hook then parks exactly on the
 * face it struck. {@code ProjectileUtil} does the sweep either way - it takes any entity, not just
 * a {@code Projectile}.
 */
public class HermitGrappleHook extends Entity {
    /** How fast it flies, in blocks per tick. Its reach is GrappleAim's - see that. */
    public static final double SPEED = 2.6;
    public static final double MAX_RANGE = GrappleAim.RANGE;

    /**
     * Attached or still travelling, published to the client.
     *
     * <p>The client needs this rather than being able to work it out. It draws the rope every frame
     * and the rope hangs differently depending on the answer - taut to a hook that has bitten, and
     * trailing behind one still in the air.
     */
    private static final EntityDataAccessor<Boolean> DATA_ATTACHED =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);

    /**
     * Who threw it, as an entity id.
     *
     * <p>Synced rather than resolved from an owner UUID the way vanilla projectiles do it. The rope
     * has to be drawn on the very first frame this entity exists, and a UUID lookup on the client
     * can miss for a tick or two while the owner is still being tracked - which is one or two frames
     * of a hook flying through the air with no rope behind it.
     */
    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.INT);

    /** What one tick on the vine costs its owner in Stand energy. */
    private static final float HOLD_COST = 0.16F;

    /**
     * Whether this vine hauls instead of holding, and which arm it came off.
     *
     * <p>Both are synced because both are the client's business: the zip is driven client-side like
     * the swing is, and the arm is the only thing telling the renderer to draw two vines apart
     * rather than one on top of another.
     */
    private static final EntityDataAccessor<Boolean> DATA_ZIP =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LEFT =
            SynchedEntityData.defineId(HermitGrappleHook.class, EntityDataSerializers.BOOLEAN);

    /** How long a zip may run before it gives up, in ticks. */
    private static final int ZIP_TIMEOUT = 60;

    private int zipTicks;

    /** Where it started, so the range cut-off is measured rather than counted in ticks. */
    private Vec3 origin = Vec3.ZERO;

    /**
     * Where it is going, settled before it was thrown.
     *
     * <p>The hook no longer finds its own target by flying until something stops it. The anchor is
     * chosen at the moment of the throw - by the same code that drew the mark on the block, so the
     * two cannot disagree - and the flight is the vine travelling to a decision already made.
     *
     * <p>Which is what makes it unable to miss. There is no sweep to fail, no thin ledge to slip
     * past between two ticks at two and a half blocks each, and no fast-mover tunnelling. It arrives
     * because arriving is the only thing it is doing.
     *
     * <p>Server-side only. The client is told the hook has attached and where it is; it never needs
     * to know where the hook was going before it got there.
     */
    private Vec3 target;

    public HermitGrappleHook(EntityType<? extends HermitGrappleHook> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public HermitGrappleHook(Level level, Player thrower, Vec3 anchor) {
        this(ModRegistries.GRAPPLE_HOOK.get(), level);

        // From the hand rather than the eyes. Starting at the eyes puts the rope through the middle
        // of the screen in first person and out of the side of the head in third.
        Vec3 from = thrower.getEyePosition().subtract(0, 0.28, 0);
        this.setPos(from.x, from.y, from.z);
        this.origin = from;
        this.target = anchor;
        this.entityData.set(DATA_OWNER, thrower.getId());

        Vec3 toward = anchor.subtract(from);
        double distance = toward.length();
        this.setDeltaMovement(distance < 1.0E-4
                ? thrower.getLookAngle().scale(SPEED)
                : toward.scale(SPEED / distance));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ATTACHED, false);
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_ZIP, false);
        builder.define(DATA_LEFT, false);
    }

    /** One of the pair Thorn Zip throws - see ThornZipSkill. */
    public static HermitGrappleHook zip(Level level, Player thrower, Vec3 anchor, boolean leftArm) {
        HermitGrappleHook hook = new HermitGrappleHook(level, thrower, anchor);
        hook.entityData.set(DATA_ZIP, true);
        hook.entityData.set(DATA_LEFT, leftArm);
        return hook;
    }

    public boolean isZip() {
        return this.entityData.get(DATA_ZIP);
    }

    public boolean isLeftArm() {
        return this.entityData.get(DATA_LEFT);
    }

    public boolean isAttached() {
        return this.entityData.get(DATA_ATTACHED);
    }

    /**
     * Culled as the whole vine, not as the hook.
     *
     * <p>This entity is three tenths of a block across, and the rope is drawn from its renderer -
     * so the game was deciding whether to draw thirty blocks of vine by asking whether one small
     * point at the far end of it was on screen. Look up while swinging and the hook goes behind
     * your own head, the renderer is skipped, and the entire rope disappears with it. That is the
     * random vanishing: not random at all, just a frustum test against the wrong shape.
     *
     * <p>Widened to enclose both ends, which is the shape actually being drawn. Culling still
     * works - the vine goes away when the whole span is off screen, which is when it should.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        Player owner = holder();
        return owner == null
                ? super.getBoundingBoxForCulling()
                : super.getBoundingBoxForCulling().minmax(owner.getBoundingBox());
    }

    /**
     * This player's vine, on either side.
     *
     * <p>Searched rather than remembered, because the two sides would have to remember it
     * separately and a remembered hook is exactly the thing that goes stale. The entity is synced,
     * so both sides are looking at the same fact.
     *
     * <p>Bounded by the vine's own reach so it is a small search over nearby entities rather than a
     * walk of everything loaded - and a hook further away than that has already discarded itself.
     */
    /**
     * Cuts every vine this player has out, whatever it was doing.
     *
     * <p>For the Stand going away. A hook is Hermit Purple reaching for something, and when the
     * Stand is dismissed - by the key, by running the pool dry, by a command - the thing doing the
     * holding no longer exists. Left alone the hook carried on pulling, and the move that would have
     * released it was no longer on the bar to press, so the player was held by a Stand they did not
     * have.
     *
     * <p>Both kinds go. A zip mid-flight is the same reach by the same Stand, and finishing one
     * after its owner had vanished would be the same wrong answer with a different animation.
     */
    public static void releaseAll(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        for (HermitGrappleHook hook : player.level().getEntitiesOfClass(
                HermitGrappleHook.class,
                player.getBoundingBox().inflate(MAX_RANGE * 2.5),
                hook -> hook.holder() == player)) {
            hook.discard();
        }
    }

    public static HermitGrappleHook findFor(Player player) {
        return find(player, false);
    }

    /**
     * This player's <em>grapple</em> vine, ignoring anything a zip threw.
     *
     * <p>The distinction is not cosmetic, and leaving it out cost the zip entirely.
     *
     * <p>Both moves throw the same entity, so a search that only asks "does this player have a vine
     * out" answers yes the instant a zip leaves the arms. The grapple is a held move, and the input
     * layer decides whether to send its release by asking exactly that question - so throwing a zip
     * made the grapple believe it had become active without the key ever going down, and since the
     * key was already up, the very next client tick sent its release. That release discards every
     * hook the player owns. The zip's pair were deleted a tick or two into their flight, which is
     * why they flashed, never arrived, and never pulled.
     *
     * <p>So the grapple asks only about grapple vines, and lets a zip get on with it.
     */
    public static HermitGrappleHook findSwing(Player player) {
        return find(player, true);
    }

    private static HermitGrappleHook find(Player player, boolean swingOnly) {
        java.util.List<HermitGrappleHook> found = player.level().getEntitiesOfClass(
                HermitGrappleHook.class,
                player.getBoundingBox().inflate(MAX_RANGE * 2.5),
                hook -> hook.holder() == player && (!swingOnly || !hook.isZip()));

        return found.isEmpty() ? null : found.get(0);
    }

    /** The player this belongs to, or null if they have gone. */
    public Player holder() {
        int id = this.entityData.get(DATA_OWNER);
        return id >= 0 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    @Override
    public void tick() {
        super.tick();

        // The server owns this entity outright, and the client owns none of it.
        //
        // Everything below decides where the hook goes, whether it has arrived, whether its owner
        // still exists and what the ride costs - all of it from state the client does not have.
        // The target in particular is never synced: it is settled at the throw and the flight is
        // just travel toward it, so the client's copy is null on every tick of every hook.
        //
        // Which meant the client hit the null-target guard below on the very first tick and threw
        // its own copy of the hook away. The server kept the real one, so nothing came back to
        // replace it - the vine appeared for a frame and vanished, and holding the key had nothing
        // to do with it. The client's job here is to be told where the hook is and draw the rope
        // to it, and that is all.
        if (this.level().isClientSide()) {
            return;
        }

        // Checked on every tick of both states, and it used to be checked on neither once attached.
        //
        // That was a hook that could never die. The attached branch below returned before any of
        // this, so a vine whose release was missed - and one was missed every time, while the move
        // still carried a cooldown that ate its own release packet - stayed nailed to the wall for
        // the rest of the session. Nothing could clear it and nothing could see it.
        //
        // The reason that stops you grappling rather than merely littering the world: a press with
        // a hook already out is read as the release, so every throw was spent letting go of a vine
        // from ten minutes ago. Press, discard the ghost, nothing happens. Press again, discard the
        // next one. The move never got as far as throwing.
        Player owner = holder();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            this.discard();
            return;
        }

        // And a vine cannot hold on from further away than it can reach. Generous, because a swing
        // legitimately stretches the span - this is the safety net for teleports and falls, not the
        // rope constraint, which lives on the client.
        if (this.position().distanceTo(owner.position()) > MAX_RANGE * 2.5) {
            this.discard();
            return;
        }

        if (this.isAttached()) {
            // Nailed down. Not even gravity - an anchor that sags is a rope that lengthens.
            this.setDeltaMovement(Vec3.ZERO);

            // A zip ends itself. It is not a hold and there is no release to wait for, so the vine
            // lets go once the owner has arrived - or once it is plain they are not going to, which
            // is what the timeout is for: a zip into a doorway can end up wedged short of its
            // anchor with nowhere left to travel.
            if (isZip()) {
                boolean arrived = owner.getEyePosition().distanceTo(this.position())
                        <= GrappleZip.ARRIVE;

                if (arrived || ++zipTicks > ZIP_TIMEOUT) {
                    // However it ended. The stall detector keeps a note of where the flier was
                    // last tick, and an entry left behind would tell the next zip it had already
                    // stopped making progress before it had started.
                    GrappleZip.forget(owner);
                    this.discard();
                }
                return;
            }

            drain(owner);
            return;
        }

        if (this.origin.equals(Vec3.ZERO)) {
            this.origin = this.position();
        }

        // Nowhere to be. Only reachable on a hook that survived a reload, which cannot happen while
        // this entity refuses to be saved - kept as the honest guard rather than as a claim.
        if (this.target == null) {
            this.discard();
            return;
        }

        Vec3 remaining = this.target.subtract(this.position());
        double left = remaining.length();

        // Arrived, or would overshoot this tick. Placed exactly on the anchor either way, because
        // the rope is measured to the hook and a hook a fraction past its mark is a rope a fraction
        // too long on every swing that follows.
        if (left <= SPEED) {
            this.setPos(this.target.x, this.target.y, this.target.z);
            attach();
            return;
        }

        // Straight down the line to it. No droop: the vine is being thrown at something specific
        // rather than lobbed, and an arc would only be a way of arriving somewhere else.
        Vec3 step = remaining.scale(SPEED / left);
        this.setDeltaMovement(step);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
    }

    /**
     * Entities are passed through on purpose.
     *
     * <p>A hook that catches a mob is a different move - it drags them to you, or you to them, and
     * either way the rope stops being something you can hang from, because the far end walks off.
     * The swing needs a fixed point, so for now the vine goes past anything alive and takes the wall
     * behind it.
     */
    private boolean canHit(Entity entity) {
        return false;
    }

    /**
     * What the ride costs, charged by the tick.
     *
     * <p>Server only, and only while it is actually holding somebody up. Running out is a release
     * rather than a refusal - the vine simply lets go, which is the one behaviour that can never
     * strand a player on a rope they cannot drop.
     */
    private void drain(Player owner) {
        if (!(this.level() instanceof ServerLevel) || !(owner instanceof ServerPlayer holder)) {
            return;
        }

        org.gumel.jojoha.data.JojohaPlayerData data =
                org.gumel.jojoha.data.PlayerDataAccess.get(holder);
        if (data == null) {
            return;
        }

        if (data.standEnergy < HOLD_COST) {
            this.discard();
            return;
        }

        data.standEnergy -= HOLD_COST;

        // Written back and pushed the same way every other energy spend does it, but synced on a
        // multiple of ticks rather than every one - the bar does not need twenty updates a second,
        // and the sync writes a log line each time it runs.
        org.gumel.jojoha.data.PlayerDataAccess.set(holder, data);
        if (this.tickCount % 5 == 0) {
            org.gumel.jojoha.data.PlayerDataAccess.sync(holder);
        }
    }

    /**
     * Every way this entity ends, in one place.
     *
     * <p>The grace is granted here rather than at each discard because there are six of them - the
     * owner dying, the range, the timeout, the arrival, the release, and the world unloading - and
     * an exemption that only covers five is an exemption that fails on whichever one the player hits
     * first. Overriding removal catches all of them by construction, including any added later.
     */
    @Override
    public void remove(RemovalReason reason) {
        Player owner = holder();
        if (owner != null && !this.level().isClientSide()) {
            GrappleGrace.grant(owner);
        }
        super.remove(reason);
    }

    /** Bites in, wherever it has got to. */
    private void attach() {
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_ATTACHED, true);

        if (this.level() instanceof ServerLevel server) {
            server.playSound(null, this.blockPosition(), SoundEvents.WEEPING_VINES_PLACE,
                    SoundSource.PLAYERS, 0.8F, 0.75F);
        }
    }

    /**
     * Never saved, and never restored.
     *
     * <p>This entity only means anything while somebody is holding the other end of it. An anchor
     * brought back on world load with no rope and no owner is litter that has to be cleaned up by
     * hand, so it simply does not persist.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** Drawn well past the point where its own hit box would have been culled. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
