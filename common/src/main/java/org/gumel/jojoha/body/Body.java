package org.gumel.jojoha.body;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.fortune.Cursed;
import org.gumel.jojoha.network.packet.BodyStatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Body {
    public static final int GHOST = 16;

    public static final int HIDDEN = 32;

    public static final int CRAWL = 64;

    public static final double CRAWL_SLOW = -0.15;

    public static final double LIMP_SLOW = -0.35;

    public static final double LIMP_JUMP = -0.5;

    public static final double ARMLESS_MINING = -0.6;

    private static final int RESEND_TICKS = 100;

    private static final ResourceLocation SLOW = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "body_slow");
    private static final ResourceLocation JUMP = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "body_jump");
    private static final ResourceLocation MINING = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "body_mining");

    private static final Map<UUID, Integer> MIRROR = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> SENT = new HashMap<>();

    private Body() {
    }

    public static int state(JojohaPlayerData data) {
        int state = data.limbs & Limbs.ALL;
        if (Cursed.ghost(data)) {
            state |= GHOST;
            if (Cursed.has(data, Cursed.FRAGILE)) {
                state |= HIDDEN;
            }
        } else if (Cursed.has(data, Cursed.LEGLESS) || Limbs.legsLost(data) == 2) {
            state |= CRAWL;
        }
        return state;
    }

    public static int state(Player player) {
        if (player.level().isClientSide()) {
            return MIRROR.getOrDefault(player.getUUID(), 0);
        }
        return state(PlayerDataAccess.get(player));
    }

    public static boolean crawling(Player player) {
        return (state(player) & CRAWL) != 0 && !player.isPassenger() && !player.isSpectator()
                && !player.getAbilities().flying;
    }

    public static boolean missing(Player player, int limb) {
        return (state(player) & limb) != 0;
    }

    public static boolean hidden(Player player) {
        return (state(player) & HIDDEN) != 0;
    }

    public static void heard(UUID player, int state) {
        if (state == 0) {
            MIRROR.remove(player);
        } else {
            MIRROR.put(player, state);
        }
    }

    public static void clearMirror() {
        MIRROR.clear();
    }

    public static void forget(Player player) {
        SENT.remove(player.getUUID());
    }

    static void tick(ServerPlayer player, JojohaPlayerData data) {
        int state = state(data);
        Integer sent = SENT.get(player.getUUID());
        if (sent == null || sent != state || state != 0 && player.tickCount % RESEND_TICKS == 0) {
            SENT.put(player.getUUID(), state);
            BodyStatePacket packet = new BodyStatePacket(player.getUUID(), state);
            for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
        boolean crawl = (state & CRAWL) != 0;
        int legs = Limbs.legsLost(data);
        modifier(player, Attributes.MOVEMENT_SPEED, SLOW,
                crawl ? CRAWL_SLOW : legs == 1 ? LIMP_SLOW : 0.0);
        modifier(player, Attributes.JUMP_STRENGTH, JUMP,
                crawl ? -1.0 : legs == 1 ? LIMP_JUMP : 0.0);
        modifier(player, Attributes.BLOCK_BREAK_SPEED, MINING,
                Limbs.lost(data, Limbs.armFor(player, InteractionHand.MAIN_HAND))
                        ? ARMLESS_MINING : 0.0);
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute,
                                 ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
