package org.gumel.jojoha.vampire;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.block.HeadJarBlock;
import org.gumel.jojoha.block.HeadJarBlockEntity;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.combat.VampireTraits;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.item.HeadJarItem;
import org.gumel.jojoha.item.Provisions;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.HeadlessPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

public final class Headless {
    public static final int FREE = 0;
    public static final int HEAD = 1;
    public static final int JARRED = 2;

    public static final EntityDimensions HEAD_SIZE = EntityDimensions.scalable(0.5F, 0.5F).withEyeHeight(0.4F);

    private static final ResourceLocation SAVE = id("severed_head");
    private static final ResourceLocation SLOW = id("severed_head_speed");

    private static final int SAVE_COOLDOWN_TICKS = 24000;
    private static final int SAVE_BLOOD = 4;
    private static final int SAVE_PALETTE_TICKS = 40;
    private static final float HEAD_HEALTH = 6F;
    private static final double HEAD_SPEED = -0.55;

    private static final float TAKE_BODY_FRACTION = 0.4F;
    private static final float TAKE_BODY_MAX_HEALTH = 100F;
    private static final int TAKE_BODY_BLOOD = 6;
    private static final float REGROWN_FRACTION = 0.5F;

    private static final int SHOW_EVERY = 10;
    private static final double SHOW_RADIUS = 96.0;
    private static final double SPILL_SEARCH = 12.0;
    private static final double JAR_DRIFT = 0.04;

    private record Jar(@Nullable UUID carrier, ResourceKey<Level> level, BlockPos pos) {
    }

    private static final Map<UUID, Jar> JARS = new HashMap<>();
    private static final Map<UUID, Long> LET_DIE = new HashMap<>();

    public static ToIntFunction<Player> clientView = player -> FREE;

    private Headless() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(Headless::onDeath);
        EntityEvent.LIVING_HURT.register((victim, source, amount) -> {
            if (victim instanceof ServerPlayer player && JARS.containsKey(player.getUUID())
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer served) {
                tick(served);
            }
        });
        PlayerEvent.PLAYER_JOIN.register(Headless::joined);
        PlayerEvent.PLAYER_QUIT.register(Headless::quit);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (data.headless) {
                data.headless = false;
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
            }
            show(player);
        });

        InteractionEvent.INTERACT_ENTITY.register(Headless::onInteract);
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> is(player)
                ? CompoundEventResult.interruptFalse(player.getItemInHand(hand))
                : CompoundEventResult.pass());
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                is(player) && !(player.level().getBlockState(pos).getBlock() instanceof HeadJarBlock)
                        ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, face) ->
                is(player) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) ->
                is(player) ? EventResult.interruptFalse() : EventResult.pass());
        BlockEvent.PLACE.register((level, pos, state, placer) ->
                placer instanceof Player player && is(player) ? EventResult.interruptFalse() : EventResult.pass());
        BlockEvent.BREAK.register((level, pos, state, player, xp) ->
                is(player) ? EventResult.interruptFalse() : EventResult.pass());
    }

    public static int state(Player player) {
        if (player.level().isClientSide()) {
            return clientView.applyAsInt(player);
        }
        if (!(player instanceof ServerPlayer served) || !PlayerDataAccess.get(served).headless) {
            return FREE;
        }
        return JARS.containsKey(player.getUUID()) ? JARRED : HEAD;
    }

    public static boolean is(Player player) {
        return state(player) != FREE;
    }

    public static boolean inJar(Player player) {
        return state(player) == JARRED;
    }

    @Nullable
    public static UUID carrierOf(UUID head) {
        Jar jar = JARS.get(head);
        return jar == null ? null : jar.carrier();
    }

    public static boolean placedAt(UUID head, Level level, BlockPos pos) {
        Jar jar = JARS.get(head);
        return jar != null && jar.carrier() == null && jar.level() == level.dimension() && jar.pos().equals(pos);
    }

    private static EventResult onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player)) {
            return EventResult.pass();
        }
        long now = player.level().getGameTime();
        Long let = LET_DIE.get(player.getUUID());
        if (let != null && let == now) {
            return EventResult.pass();
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.headless || data.vampireStage != VampireStage.VAMPIRE || !survives(player, data, source)) {
            LET_DIE.put(player.getUUID(), now);
        }
        if (data.headless) {
            data.headless = false;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            leaveJar(player);
            return EventResult.pass();
        }
        if (data.vampireStage != VampireStage.VAMPIRE || !survives(player, data, source)) {
            return EventResult.pass();
        }

        VampireBlood.spend(player, SAVE_BLOOD);
        NetworkHandler.sendPalette(player, Palette.DREAD, SAVE_PALETTE_TICKS);
        data.setMoveCooldown(SAVE, now, SAVE_COOLDOWN_TICKS);
        data.headless = true;
        StandSummonHandler.dismiss(player, data);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        player.setHealth(HEAD_HEALTH);
        player.clearFire();
        player.stopRiding();
        player.refreshDimensions();

        ServerLevel level = player.serverLevel();
        Vec3 neck = player.position().add(0.0, 0.5, 0.0);
        for (double up = 0.0; up <= 1.4; up += 0.35) {
            VampireFx.blood(level, neck.x, neck.y + up, neck.z, 8, 0.25, 0.08);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_REANIMATE.get(), SoundSource.PLAYERS, 0.8F, 0.4F);
        player.displayClientMessage(Component.translatable("message.jojoha.vampire.severed"), true);
        show(player);
        return EventResult.interruptFalse();
    }

    private static boolean survives(ServerPlayer player, JojohaPlayerData data, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FIRE)
                || Provisions.staked(source)) {
            return false;
        }
        if (source.getEntity() instanceof ServerPlayer striker && Fate.breathesHamon(PlayerDataAccess.get(striker))) {
            return false;
        }
        if (VampireTraits.inSunlight(player)) {
            return false;
        }
        return !data.isMoveOnCooldown(SAVE, player.level().getGameTime())
                && VampireBlood.blood(player) >= SAVE_BLOOD;
    }

    private static void tick(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.headless && data.vampireStage == VampireStage.NONE) {
            regrow(player, null);
            return;
        }
        slow(player, data.headless);
        if (!data.headless) {
            return;
        }
        Jar jar = JARS.get(player.getUUID());
        if (jar != null) {
            hold(player, jar);
        }
        if (player.tickCount % SHOW_EVERY == 0) {
            show(player);
        }
    }

    private static void slow(ServerPlayer player, boolean headless) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.hasModifier(SLOW) == headless) {
            return;
        }
        if (headless) {
            speed.addTransientModifier(new AttributeModifier(SLOW, HEAD_SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(SLOW);
        }
    }

    private static void hold(ServerPlayer head, Jar jar) {
        if (jar.carrier() != null) {
            ServerPlayer carrier = head.server.getPlayerList().getPlayer(jar.carrier());
            if (carrier == null || !carrier.isAlive() || !holds(carrier, head.getUUID())) {
                spill(head, carrier == null ? head.position() : carrier.position());
                return;
            }
            if (head.getVehicle() != carrier) {
                head.startRiding(carrier, true);
            }
            return;
        }

        ServerLevel level = head.server.getLevel(jar.level());
        if (level == null || !(level.getBlockEntity(jar.pos()) instanceof HeadJarBlockEntity box)
                || !head.getUUID().equals(box.occupant())) {
            JARS.remove(head.getUUID());
            show(head);
            return;
        }
        if (head.isShiftKeyDown()) {
            box.empty();
            JARS.remove(head.getUUID());
            Vec3 top = Vec3.atBottomCenterOf(jar.pos()).add(0.0, 1.0, 0.0);
            head.teleportTo(level, top.x, top.y, top.z, head.getYRot(), head.getXRot());
            level.playSound(null, jar.pos(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 0.6F);
            show(head);
            return;
        }
        Vec3 inside = Vec3.atBottomCenterOf(jar.pos()).add(0.0, 0.1, 0.0);
        if (head.level() != level || head.position().distanceToSqr(inside) > JAR_DRIFT) {
            head.teleportTo(level, inside.x, inside.y, inside.z, head.getYRot(), head.getXRot());
        }
        head.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean holds(ServerPlayer carrier, UUID head) {
        if (head.equals(HeadJarItem.occupant(carrier.containerMenu.getCarried()))) {
            return true;
        }
        for (int i = 0; i < carrier.getInventory().getContainerSize(); i++) {
            if (head.equals(HeadJarItem.occupant(carrier.getInventory().getItem(i)))) {
                return true;
            }
        }
        return false;
    }

    private static void spill(ServerPlayer head, Vec3 near) {
        JARS.remove(head.getUUID());
        head.stopRiding();
        AABB around = AABB.ofSize(near, SPILL_SEARCH * 2, SPILL_SEARCH * 2, SPILL_SEARCH * 2);
        for (ItemEntity dropped : head.serverLevel().getEntitiesOfClass(ItemEntity.class, around,
                item -> head.getUUID().equals(HeadJarItem.occupant(item.getItem())))) {
            ItemStack empty = dropped.getItem().copy();
            HeadJarItem.clear(empty);
            dropped.setItem(empty);
            head.teleportTo(dropped.getX(), dropped.getY() + 0.2, dropped.getZ());
        }
        head.serverLevel().playSound(null, head.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F);
        show(head);
    }

    private static void leaveJar(ServerPlayer head) {
        Jar jar = JARS.remove(head.getUUID());
        if (jar == null) {
            return;
        }
        if (jar.carrier() == null) {
            ServerLevel level = head.server.getLevel(jar.level());
            if (level != null && level.getBlockEntity(jar.pos()) instanceof HeadJarBlockEntity box) {
                box.empty();
            }
        } else {
            head.stopRiding();
        }
        show(head);
    }

    private static EventResult onInteract(Player player, Entity target, InteractionHand hand) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer served)) {
            return EventResult.pass();
        }
        if (target instanceof ServerPlayer head && state(head) == HEAD
                && player.getMainHandItem().getItem() instanceof HeadJarItem
                && HeadJarItem.occupant(player.getMainHandItem()) == null && !is(served)) {
            carry(head, served);
            return EventResult.interruptTrue();
        }
        if (state(served) == HEAD && target instanceof LivingEntity prey && takeable(prey)) {
            takeBody(served, prey);
            return EventResult.interruptTrue();
        }
        return EventResult.pass();
    }

    private static boolean takeable(LivingEntity prey) {
        return prey.isAlive() && VampireBlood.hasBlood(prey) && prey.getMaxHealth() <= TAKE_BODY_MAX_HEALTH
                && prey.getHealth() <= prey.getMaxHealth() * TAKE_BODY_FRACTION
                && !(prey instanceof ServerPlayer other && is(other));
    }

    private static void carry(ServerPlayer head, ServerPlayer carrier) {
        ItemStack held = carrier.getMainHandItem();
        ItemStack full = held.split(1);
        HeadJarItem.fill(full, head);
        if (held.isEmpty()) {
            carrier.setItemInHand(InteractionHand.MAIN_HAND, full);
        } else if (!carrier.getInventory().add(full)) {
            carrier.drop(full, false);
        }
        JARS.put(head.getUUID(), new Jar(carrier.getUUID(), carrier.level().dimension(), carrier.blockPosition()));
        head.startRiding(carrier, true);
        carrier.swing(InteractionHand.MAIN_HAND, true);
        carrier.serverLevel().playSound(null, carrier.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 0.6F);
        show(head);
    }

    public static boolean enterPlaced(ServerPlayer head, HeadJarBlockEntity box) {
        if (state(head) != HEAD || box.occupant() != null || box.getLevel() == null) {
            return false;
        }
        box.fill(head.getUUID(), head.getGameProfile().getName());
        JARS.put(head.getUUID(), new Jar(null, box.getLevel().dimension(), box.getBlockPos()));
        box.getLevel().playSound(null, box.getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 0.6F);
        show(head);
        return true;
    }

    public static void placed(ServerPlayer carrier, UUID head, HeadJarBlockEntity box) {
        ServerPlayer occupant = carrier.server.getPlayerList().getPlayer(head);
        Jar jar = JARS.get(head);
        if (occupant == null || jar == null || !carrier.getUUID().equals(jar.carrier()) || box.getLevel() == null) {
            box.empty();
            return;
        }
        occupant.stopRiding();
        box.fill(head, occupant.getGameProfile().getName());
        JARS.put(head, new Jar(null, box.getLevel().dimension(), box.getBlockPos()));
        show(occupant);
    }

    public static ItemStack pickUp(ServerPlayer carrier, HeadJarBlockEntity box) {
        UUID head = box.occupant();
        ServerPlayer occupant = head == null ? null : carrier.server.getPlayerList().getPlayer(head);
        ItemStack jar = new ItemStack(box.getBlockState().getBlock().asItem());
        box.empty();
        if (occupant == null || !placedAt(head, box.getLevel(), box.getBlockPos())) {
            return jar;
        }
        HeadJarItem.fill(jar, occupant);
        JARS.put(head, new Jar(carrier.getUUID(), carrier.level().dimension(), carrier.blockPosition()));
        occupant.startRiding(carrier, true);
        show(occupant);
        return jar;
    }

    public static void shattered(Level level, BlockPos pos, @Nullable UUID head) {
        if (head == null || !(level instanceof ServerLevel served) || !placedAt(head, level, pos)) {
            return;
        }
        JARS.remove(head);
        ServerPlayer occupant = served.getServer().getPlayerList().getPlayer(head);
        if (occupant != null) {
            show(occupant);
        }
    }

    private static void takeBody(ServerPlayer head, LivingEntity prey) {
        ServerLevel level = head.serverLevel();
        Vec3 at = prey.position();
        prey.hurt(head.damageSources().playerAttack(head),
                DamageScale.vampire(head, prey, Float.MAX_VALUE));
        if (prey.isAlive()) {
            return;
        }
        head.teleportTo(level, at.x, at.y, at.z, head.getYRot(), head.getXRot());
        regrow(head, prey);
        VampireBlood.feed(head, TAKE_BODY_BLOOD);
    }

    public static void regrow(ServerPlayer player, @Nullable LivingEntity from) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.headless) {
            return;
        }
        leaveJar(player);
        data.headless = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        slow(player, false);
        player.refreshDimensions();
        player.setHealth(Math.max(player.getHealth(), player.getMaxHealth() * REGROWN_FRACTION));

        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        for (double up = 0.0; up <= 2.0; up += 0.35) {
            VampireFx.blood(level, at.x, at.y + up, at.z, 8, 0.32, 0.03);
        }
        VampireFx.ring(level, at.x, at.y + 0.15, at.z, 1.6, 2);
        level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_REANIMATE.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        if (from != null) {
            player.displayClientMessage(Component.translatable("message.jojoha.vampire.took_body",
                    from.getDisplayName()), true);
        }
        show(player);
    }

    private static void joined(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (data.headless && player.level().getBlockEntity(player.blockPosition()) instanceof HeadJarBlockEntity box
                && (box.occupant() == null || player.getUUID().equals(box.occupant()))) {
            box.empty();
            enterPlaced(player, box);
        }
        player.refreshDimensions();
        show(player);
    }

    private static void quit(ServerPlayer player) {
        LET_DIE.remove(player.getUUID());
        leaveJar(player);
        JARS.entrySet().removeIf(entry -> {
            if (!player.getUUID().equals(entry.getValue().carrier())) {
                return false;
            }
            ServerPlayer head = player.server.getPlayerList().getPlayer(entry.getKey());
            if (head != null) {
                head.stopRiding();
                head.teleportTo(player.getX(), player.getY(), player.getZ());
                NetworkManager.sendToPlayers(player.serverLevel().players(), new HeadlessPacket(head.getId(), HEAD));
            }
            return true;
        });
    }

    public static void show(ServerPlayer player) {
        HeadlessPacket packet = new HeadlessPacket(player.getId(), state(player));
        double radius = SHOW_RADIUS * SHOW_RADIUS;
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer == player || viewer.distanceToSqr(player) <= radius) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }
}
