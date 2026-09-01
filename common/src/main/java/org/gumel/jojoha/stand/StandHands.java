package org.gumel.jojoha.stand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.skill.EnergyWeight;

/**
 * The Stand as a second pair of hands: it uses the item its user is holding, from where
 * <em>it</em> is standing rather than from where they are.
 *
 * <p>Water goes down at the Stand's feet, a pearl leaves from the Stand's hand, a fire is struck on
 * the block the Stand is reaching for. Nothing about what the item does changes - a bucket is still
 * a bucket - only the point in the world the act happens at.
 *
 * <h2>What the Stand actually changes</h2>
 *
 * <p>The reach, and nothing else about the aim. You point where you want the thing to happen and
 * the Stand goes and does it - out to {@link #REACH} blocks rather than the four and a half your
 * own arms manage.
 *
 * <p>It was built the other way round first: the player was briefly relocated to the Stand's
 * position so that every ray, spawn point and collision check vanilla performs would resolve from
 * there. That is a defensible reading of "the Stand does it", and it worked - but it aimed from the
 * Stand's eyes rather than the player's, which meant the block landed somewhere the crosshair was
 * not pointing, by a margin that grew with however far out the Stand had drifted. Aiming a thing
 * you cannot aim with is not a feature. The origin is the player's eye now, and the Stand's
 * contribution is the distance.
 *
 * <p>Only one number is touched, and only for the length of one synchronous call: the player's
 * block-interaction range. That covers both camps of item at once - the ones that act on a hit
 * result handed to them, and the ones that raycast for themselves inside {@code use}, a bucket
 * being the obvious one - so the two can never disagree about how far away a thing is allowed to
 * be. The restore is in a {@code finally} so a throwing item cannot leave the range raised.
 *
 * <p>The trade this makes, stated plainly: a thrown item now leaves from the player rather than
 * from the Stand, because nothing is relocated any more. A pearl thrown this way goes further than
 * your arm allows but starts where you are standing.
 *
 * <p>Two things are deliberately <em>not</em> delegated:
 *
 * <ul>
 *   <li><b>Items with a use duration</b> - bows, food, shields, tridents, spyglasses. Those call
 *   {@code startUsingItem} and finish on some later tick, by which point the player is back in
 *   their body and the release would happen from there. A half-delegated draw is worse than none,
 *   so they fall through to the player's own hands untouched.</li>
 *   <li><b>The block's own interaction</b> - opening a chest, flipping a lever. This calls
 *   {@code ItemStack.useOn} directly rather than {@code ServerPlayerGameMode.useItemOn}, which
 *   would run the block's interaction first. That is not squeamishness: a container menu checks
 *   every tick that the player is still within four blocks of it, so a chest opened at the Stand's
 *   reach would slam shut on the very next tick. The rule that falls out of it is a clean one - the
 *   Stand uses the item, it does not operate the furniture.</li>
 * </ul>
 */
public final class StandHands {
    /** Shares the cooldown map with the moves, so it needs an id like they have. */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_hands");

    /**
     * How far the Stand can reach from its own eyes, in blocks.
     *
     * <p>How far the Stand will travel to do a job, by range class. Not an arm's length - the Stand
     * goes there - but not unlimited either: a Stand that would cross any distance stops being a
     * Stand and becomes a cursor.
     *
     * <p>Applied to the player's own interaction-range attribute for the length of the call as well
     * as to the trace, because items fall into two camps: those that act on a hit result handed to
     * them, and those that raycast for themselves inside {@code use} - a bucket being the obvious
     * one. Setting both is what stops those two camps disagreeing about how far away a thing is
     * allowed to be. Vanilla would otherwise refuse the placement on distance alone.
     */
    public static final double CLOSE_REACH = 15.0;
    public static final double LONG_REACH = 30.0;

    /**
     * How far this player's Stand will go to work.
     *
     * <p>A close-range Stand is close-range even when it is running errands. Star Platinum's whole
     * character is that it does enormous things within arm's reach, and letting it wander thirty
     * blocks out to lay a floor would quietly delete the one limitation that defines it. A
     * long-range Stand is defined by the opposite, so it gets double.
     */
    public static double reachFor(JojohaPlayerData data) {
        return StandTypes.byIdOrDefault(data.stand.standId()).range() == StandRange.LONG
                ? LONG_REACH
                : CLOSE_REACH;
    }

    /**
     * Matches vanilla's own right-click delay, and is the same number for the same reason: it is
     * the rate at which holding the button repeats. Making the Stand slower than your own hands
     * would be a difference the player feels as lag rather than as a limit.
     */
    private static final int COOLDOWN_TICKS = 4;

    private static final float ENERGY_COST = EnergyWeight.TRIVIAL.cost();

    /**
     * How far away a placement is still allowed to sound from.
     *
     * <p>Comfortably inside the roll-off, so a block laid at the end of a long-range Stand's reach
     * is as clear as one laid at your feet.
     */
    private static final double AUDIBLE_RANGE = 8.0;

    /** How long the used item stays visible in the Stand's hand afterwards. */
    private static final int HOLD_TICKS = 10;

    private StandHands() {
    }

    /**
     * Server-side handling of a delegated right-click.
     *
     * <p>The packet carries nothing at all, deliberately. Which hand, which item, what is being
     * aimed at and whether any of it is allowed are all re-derived here, so the worst a tampered
     * client can do is ask for a use it was entitled to anyway.
     */
    public static void handleUseRequest(ServerPlayer player, java.util.Optional<BlockPos> stretchAnchor) {
        JojohaPlayerData data = PlayerDataAccess.get(player);

        // Range work needs a whole Stand. PARTIAL is a pair of reinforcing arms fixed to the user -
        // they have no position of their own to act from, which is the only thing this offers.
        if (!data.standSummoned || !data.stand.isPresent() || !data.stand.trust().canActAtRange()) {
            return;
        }

        // And it needs the Stand to have been told this is what it is for. The client checks the
        // same thing before it gives the click away at all, so reaching here in the wrong stance
        // means the mode changed between the press and the packet - or that somebody is asking
        // directly. Either way the answer is the same one.
        if (!data.standMode.handlesItems()) {
            return;
        }

        // Caught in somebody else's stopped time. Their input is already refused at the keyboard
        // (see KeyboardInputMixin), but that is a client-side courtesy and this is a packet.
        if (data.isTimeStopFrozen()) {
            return;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            return;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(ID, now)) {
            return;
        }

        if (data.standEnergy < ENERGY_COST) {
            player.displayClientMessage(Component.translatable("message.jojoha.skill.no_energy"), true);
            return;
        }

        // One job at a time. A second click while the Stand is still crossing to the first would
        // otherwise queue behind it and land somewhere the player stopped meaning several seconds
        // ago - and holding the button makes that happen five times a second.
        if (StandUtilityWork.isBusy(player)) {
            return;
        }

        // Both hands, main first, exactly as vanilla's own use does - so an excluded item in the
        // main hand falls through to the off hand rather than swallowing the click.
        for (InteractionHand hand : InteractionHand.values()) {
            if (!canDelegate(player, player.getItemInHand(hand))) {
                continue;
            }

            double reach = reachFor(data);
            BlockHitResult hit = aimFrom(player, player.getEyePosition(),
                    player.getViewVector(1.0F), reach);
            data.setMoveCooldown(ID, now, COOLDOWN_TICKS);
            PlayerDataAccess.set(player, data);

            if (hit.getType() == HitResult.Type.BLOCK) {
                // Something in the world to go to. The Stand travels; the act happens on arrival,
                // with the aim exactly as it was at this moment - see StandUtilityWork.
                StandUtilityWork.queue(player, stand, run(player, stretchAnchor, hit, hand, reach),
                        hit.getDirection(), hand);
                return;
            }

            // Nothing under the crosshair, but a stretch is planted and the hand holds something
            // that fills a cell: the player is running a pillar into open air. The far end comes
            // from where they are looking rather than from what they hit - see PlacementRun.farEnd.
            if (stretchAnchor.isPresent() && fills(player.getItemInHand(hand))
                    && data(player).buildMode.stretches()) {
                java.util.List<BlockPos> pillar = reachable(player,
                        PlacementRun.between(stretchAnchor.get(),
                                PlacementRun.farEnd(stretchAnchor.get(), player.getEyePosition(),
                                        player.getViewVector(1.0F), reach, data(player).buildMode),
                                data(player).buildMode),
                        reach);
                if (!pillar.isEmpty()) {
                    // Placed against the face the run is growing away from, so each block sits on
                    // the one before it rather than being oriented by a click that never happened.
                    StandUtilityWork.queue(player, stand, pillar, Direction.UP, hand);
                    return;
                }
            }

            // Nothing under the crosshair, so there is nowhere to send it. A throw is the case this
            // covers - a pearl into open sky - and it happens from where the Stand already is,
            // which in this stance is at the player's shoulder.
            performQueued(player, stand, hand, hit);
            return;
        }
    }

    /**
     * Plays the placement back to the person who asked for it.
     *
     * <p>Vanilla already played it - to everybody except them. {@code BlockItem.place} and the
     * bucket both call {@code Level.playSound(player, ...)}, whose {@code Player} argument is the
     * one to <em>skip</em>, on the assumption that the placer's own client predicted the placement
     * and played the sound locally a tick earlier. That assumption is false here: the client
     * predicts nothing, because the server is doing the placing on the Stand's behalf. So the one
     * person who should certainly hear it is the only one who does not.
     *
     * <p>Sent to that player alone rather than replayed to the world, which would double it for
     * everyone standing nearby who already heard vanilla's.
     *
     * <p>Read off the world afterwards rather than from the item, so it is the sound of what
     * actually ended up there - a block's own material, or the bucket, whichever happened.
     */
    private static void echo(ServerPlayer player, ItemStack used, BlockPos cell) {
        BlockState placed = player.level().getBlockState(cell);

        SoundEvent sound;
        if (used.getItem() instanceof net.minecraft.world.item.BucketItem) {
            // Which way round the bucket went is not worth deducing from the item: what is in the
            // cell now says it plainly. Something wet there means it was poured out, and an empty
            // cell where a bucket was just used means it was scooped up.
            boolean poured = !placed.getFluidState().isEmpty();
            boolean lava = poured && placed.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
            sound = poured
                    ? (lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY)
                    : SoundEvents.BUCKET_FILL;
        } else if (!placed.isAir()) {
            sound = placed.getSoundType().getPlaceSound();
        } else {
            // Nothing landed - a refused placement, or an item that acts without leaving anything
            // behind. Silence is the honest answer.
            return;
        }

        net.minecraft.world.level.block.SoundType type = placed.getSoundType();

        // Reeled in toward the listener rather than played where it happened. A block sound rolls
        // off over about sixteen blocks and the Stand routinely works at thirty, so a truthful
        // position would be a truthful silence. Pulled along the line between the two, so it still
        // arrives from the direction the Stand is working in - only never from further away than
        // can be heard.
        Vec3 at = player.getEyePosition();
        Vec3 toCell = Vec3.atCenterOf(cell).subtract(at);
        double distance = toCell.length();
        Vec3 source = distance <= AUDIBLE_RANGE || distance < 1.0E-4
                ? Vec3.atCenterOf(cell)
                : at.add(toCell.scale(AUDIBLE_RANGE / distance));

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                net.minecraft.sounds.SoundSource.BLOCKS,
                source.x, source.y, source.z,
                (type.getVolume() + 1F) / 2F, type.getPitch() * 0.8F,
                player.level().getRandom().nextLong()));
    }

    /**
     * The cells this click will fill: one, or a stretched row.
     *
     * <p>The far end is always the server's own trace, never the client's - so the only thing taken
     * on trust is where the player planted the near corner, and a client that lies about it gets a
     * run in the wrong place rather than a longer one. Every cell is checked against the Stand's
     * reach, which is what stops an anchor dropped and then walked away from turning into a row
     * stretching back over the horizon.
     */
    private static java.util.List<BlockPos> run(ServerPlayer player, java.util.Optional<BlockPos> anchor,
                                                BlockHitResult hit, InteractionHand hand, double reach) {
        BlockPos far = new net.minecraft.world.item.context.BlockPlaceContext(
                player, hand, player.getItemInHand(hand), hit).getClickedPos();

        // SINGLE has no run to build, so a corner the client is still holding is ignored rather
        // than obeyed - the tool the player picked is what decides, not what they happen to be
        // leaning on.
        if (anchor.isEmpty() || !data(player).buildMode.stretches()) {
            return java.util.List.of(far);
        }

        java.util.List<BlockPos> cells = reachable(player,
                PlacementRun.between(anchor.get(), far, data(player).buildMode), reach);
        return cells.isEmpty() ? java.util.List.of(far) : cells;
    }

    /**
     * Drops the cells the Stand has no business travelling to.
     *
     * <p>The near corner is the client's and the far end is the server's, so a player who plants an
     * anchor and then walks away is asking for a row that stretches back over the horizon. Filtering
     * per cell rather than refusing the whole run means the part still within reach is built and the
     * rest is simply not, which is what a player who over-stretched actually wants.
     */
    /** Short hand for the player's own record; this class reads it in several places. */
    private static JojohaPlayerData data(ServerPlayer player) {
        return PlayerDataAccess.get(player);
    }

    private static java.util.List<BlockPos> reachable(ServerPlayer player,
                                                      java.util.List<BlockPos> cells, double reach) {
        java.util.List<BlockPos> kept = new java.util.ArrayList<>();
        double reachSqr = reach * reach;
        for (BlockPos cell : cells) {
            if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(cell)) <= reachSqr) {
                kept.add(cell);
            }
        }
        return kept;
    }

    /**
     * Whether this item ends up occupying a cell, as opposed to acting on one.
     *
     * <p>A block and a bucket do - a bucket is a block of water by another name. A pearl, a flint
     * and steel, a bone meal act on what is already there. Only the first kind can be run in a row,
     * because only the first kind has cells to fill.
     */
    public static boolean fills(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem
                || stack.getItem() instanceof net.minecraft.world.item.BucketItem;
    }

    /**
     * Runs a use the Stand has arrived for.
     *
     * <p>Called from {@code StandUtilityWork} once the Stand is at the block, and directly for
     * throws that have nowhere to travel to. Public for that reason and no other.
     *
     * <p>The player is relocated to the Stand for the length of the call, which is what makes this
     * the Stand's act rather than the player's: a thrown item leaves from the Stand's hand, and an
     * item that raycasts for itself resolves against the block the Stand is standing at. That was a
     * problem when the Stand hovered at the player's shoulder and the target was twenty blocks
     * away; it stopped being one when the Stand started going to the target first.
     */
    public static void performQueued(ServerPlayer player, StandEntity stand, InteractionHand hand,
                                     BlockHitResult hit) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.standEnergy < ENERGY_COST || !canDelegate(player, player.getItemInHand(hand))) {
            return;
        }

        // Copied before the use, because the use is what may spend it: a bucket empties and a pearl
        // leaves the stack entirely, so reading the hand afterwards to find out what the Stand just
        // threw would find the wrong item, or nothing.
        ItemStack shown = player.getItemInHand(hand).copy();

        // Worked out before the placement too, and for a sharper reason. For a queued job the hit
        // *is* the destination cell, so once the block is in it BlockPlaceContext no longer sees
        // somewhere replaceable and steps one further on - naming a cell the placement never
        // touched, which is air, which sounds like nothing at all. Asked while the world still
        // looks the way the placement is about to change.
        BlockPos cell = hit.getType() == HitResult.Type.BLOCK
                ? new net.minecraft.world.item.context.BlockPlaceContext(player, hand, shown, hit)
                        .getClickedPos()
                : null;
        if (!perform(player, stand, hand, hit).consumesAction()) {
            return;
        }

        stand.showHeldItem(shown, HOLD_TICKS);
        if (cell != null) {
            echo(player, shown, cell);
        }
        data.standEnergy -= ENERGY_COST;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /**
     * Whether this item can be handed over at all.
     *
     * <p>A use duration is the whole test, and it is a better one than a list of item classes would
     * be: it is exactly the property that decides whether the act finishes now or on some later
     * tick, which is exactly what the relocation cannot survive. It also costs nothing to keep
     * current - a modded item that draws like a bow is excluded because it draws like a bow, not
     * because somebody remembered to name it.
     */
    private static boolean canDelegate(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && stack.getUseDuration(player) <= 0;
    }

    /**
     * Runs the use with the player standing where the Stand is.
     *
     * <p>The order mirrors {@code Minecraft.startUseItem}: something in reach gets the item's own
     * block behaviour first, and anything that does not consume the click falls through to the
     * in-air use. A FAIL stops there rather than falling through, which is vanilla's behaviour and
     * matters - it is how an item says "not on that, and not instead of it either".
     */
    private static InteractionResult perform(ServerPlayer player, StandEntity stand,
                                             InteractionHand hand, BlockHitResult hit) {
        ServerLevel level = player.serverLevel();

        AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        double homeReach = reach == null ? 0 : reach.getBaseValue();
        Vec3 home = player.position();
        boolean homeBlocksBuilding = player.blocksBuilding;
        float homeYRot = player.getYRot();
        float homeXRot = player.getXRot();

        if (reach != null) {
            reach.setBaseValue(LONG_REACH);
        }

        // Stood where the Stand is, by the eyes rather than the feet, for the length of this call.
        // Everything vanilla does downstream measures from getEyePosition(), so this is what makes a
        // pearl leave from the Stand's hand rather than from the player's.
        player.setPos(stand.getX(), stand.getEyeY() - player.getEyeHeight(), stand.getZ());

        // And stops obstructing the cell it is filling. The proxy is standing where the Stand is,
        // which for a pillar run is often inside the column it is building - and BlockItem.place
        // asks Level.isUnobstructed, which refuses if any entity in the target has blocksBuilding
        // set. Measured, twice: a sixteen-block pillar came back seven blocks tall without this.
        player.blocksBuilding = false;

        // And made to look at the job. This is the half that was missing, and it is why a water
        // bucket landed several blocks from where it was aimed: an item with no useOn - a bucket is
        // the one that matters - ignores the hit result entirely and raycasts for itself, from the
        // proxy's eyes along the proxy's *current* view. Moving the body without turning it meant
        // that ray left the Stand pointing wherever the player happened to be looking a second
        // later, and the water went there instead.
        if (hit.getType() == HitResult.Type.BLOCK) {
            lookAt(player, hit.getLocation());
        }

        try {
            stand.reachTowards(hit.getType() == HitResult.Type.BLOCK
                    ? Vec3.atCenterOf(hit.getBlockPos())
                    : player.getEyePosition().add(player.getViewVector(1.0F).scale(LONG_REACH)));

            if (hit.getType() == HitResult.Type.BLOCK) {
                InteractionResult onBlock = player.getItemInHand(hand)
                        .useOn(new UseOnContext(player, hand, hit));
                if (onBlock != InteractionResult.PASS) {
                    return onBlock;
                }
            }

            // Re-read rather than reused: the block pass above may have emptied the hand, and
            // handing a spent stack to the in-air path would use an item that is no longer there.
            ItemStack inHand = player.getItemInHand(hand);
            if (inHand.isEmpty()) {
                return InteractionResult.PASS;
            }

            // useItem, never useItemOn - see the class note on why the block's own interaction is
            // left out. This is the same call vanilla makes for a click into empty air, and it
            // handles the item cooldown and the returned stack for us.
            InteractionResult thrown = player.gameMode.useItem(player, level, inHand, hand);
            if (thrown.consumesAction()) {
                empower(player, stand);
            }
            return thrown;
        } finally {
            player.setPos(home.x, home.y, home.z);
            player.blocksBuilding = homeBlocksBuilding;
            player.setYRot(homeYRot);
            player.setXRot(homeXRot);
            if (reach != null) {
                reach.setBaseValue(homeReach);
            }
        }
    }

    /**
     * Turns the proxy to face a point in the world.
     *
     * <p>Only ever set and restored inside one synchronous call, so the player never sees it. The
     * client owns rotation and will overwrite this on its next movement packet regardless; the
     * point is purely that anything raycasting during the call agrees with the aim that was
     * captured at the click.
     */
    private static void lookAt(ServerPlayer player, Vec3 target) {
        Vec3 delta = target.subtract(player.getEyePosition());
        double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        player.setYRot((float) (Mth.atan2(delta.z, delta.x) * (180F / Math.PI)) - 90F);
        player.setXRot((float) (-(Mth.atan2(delta.y, flat) * (180F / Math.PI))));
    }

    /**
     * How much harder a Stand throws than a pair of arms does.
     *
     * <p>A Stand's whole physical claim is that it is stronger and faster than the person it comes
     * out of, and a pearl lobbed exactly as far as the player could have lobbed it themselves makes
     * a liar of that. Applied to whatever left the hand rather than to a list of items, so anything
     * throwable inherits it.
     */
    private static final double THROW_POWER = 2.4;

    /**
     * Puts the Stand's strength behind anything it just threw.
     *
     * <p>Found by looking for what appeared rather than by knowing what the item spawns: everything
     * owned by this player, next to the Stand, on its very first tick. That covers pearls, snowballs,
     * eggs and anything a mod adds, without this code ever naming one of them.
     */
    private static void empower(ServerPlayer player, StandEntity stand) {
        for (Projectile projectile : player.serverLevel().getEntitiesOfClass(Projectile.class,
                stand.getBoundingBox().inflate(3.0),
                candidate -> candidate.tickCount == 0 && candidate.getOwner() == player)) {

            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(THROW_POWER));
            projectile.hasImpulse = true;
        }
    }

    /**
     * What the player is pointing at, as far out as the Stand can reach.
     *
     * <p>The client draws a box on this exact ray - see {@code StandPlacePreview} - so the two have
     * to be computed the same way or the preview would be a lie. Same origin, same reach, same clip
     * settings.
     *
     * <p>{@code Fluid.NONE} to match the client's own pick: a bucket held over water has to miss
     * here so that it falls through to {@code BucketItem.use}, which runs its own source-only trace
     * and is the only code that knows the difference between filling and placing.
     */
    /**
     * What the player is pointing at, in three passes of decreasing confidence.
     *
     * <p>Static and public because the client draws a box on this exact answer - see
     * {@code StandPlacePreview}. Two implementations of "what is under the crosshair" would drift,
     * and the drift would show up as a ghost box that is not where the block goes.
     *
     * <ol>
     *   <li>solid blocks, fluids ignored - the ordinary case, and deliberately blind to water so a
     *   full bucket held over a pond targets its floor rather than its surface;</li>
     *   <li>the same ray counting fluid surfaces, which is the only way an <em>empty</em> bucket
     *   has anything to aim at;</li>
     *   <li>failing both, a block next to where the ray ran out - see {@link #nearby}.</li>
     * </ol>
     */
    public static BlockHitResult aimFrom(Player player, Vec3 eye, Vec3 look, double reach) {
        Vec3 end = eye.add(look.scale(reach));

        BlockHitResult solid = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (solid.getType() == HitResult.Type.BLOCK) {
            return solid;
        }

        BlockHitResult wet = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (wet.getType() == HitResult.Type.BLOCK) {
            return wet;
        }

        return nearby(player, end);
    }

    /**
     * A block the crosshair very nearly found.
     *
     * <p>Aiming at the top of something you are standing beside means aiming at the thin strip of
     * air above it, and missing that strip by a pixel means the ray sails off into the sky and the
     * click does nothing. That is a miss the player did not make - they were pointing at the block,
     * the block simply has no top surface to be pointed at from where they stand.
     *
     * <p>So a ray that found nothing looks at the cell it died in and takes a solid neighbour, if
     * exactly one side has one. Down first, because building on top of things is the case this
     * exists for. One block of tolerance and no more: this must never fire when the player really is
     * pointing at open sky, which is why it is last and why it wants an actual neighbour rather than
     * a search radius.
     */
    private static BlockHitResult nearby(Player player, Vec3 end) {
        BlockPos at = BlockPos.containing(end);

        for (Direction side : SNAP_ORDER) {
            BlockPos neighbour = at.relative(side);
            if (!player.level().getBlockState(neighbour).isSolidRender(player.level(), neighbour)) {
                continue;
            }
            // The face pointing back at the cell the ray died in, which is the one the player was
            // looking over the edge of.
            return new BlockHitResult(Vec3.atCenterOf(neighbour), side.getOpposite(), neighbour, false);
        }

        return BlockHitResult.miss(end, Direction.UP, at);
    }

    /** Down first: the block you are standing next to and trying to build on top of. */
    private static final Direction[] SNAP_ORDER = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.UP};
}
