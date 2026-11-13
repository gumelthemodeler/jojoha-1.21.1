package org.gumel.jojoha.fortune;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FortuneSensePacket;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Bizarre {
    public static final double SOCIAL_RANGE = 16.0;

    public static final double SOCIAL_LUCK = 1.0;

    public static final double SOCIAL_SPEED = 0.10;

    public static final float TRADE_DISCOUNT = 0.10F;

    public static final int PULL_EVERY = 6000;

    public static final double THREAT_RANGE = 48.0;

    public static final int THREAT_GLOW_TICKS = 60;

    public static final int THREAT_AGAIN_TICKS = 6000;

    public static final double DRAW_RANGE = 64.0;

    public static final int DRAW_EVERY = 6000;

    private static final int THREAT_EVERY = 40;

    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final ResourceLocation LUCK_ID = Fortunes.id("fortune_bizarre_luck");

    private static final ResourceLocation SPEED_ID = Fortunes.id("fortune_bizarre_speed");

    private static final Map<UUID, Map<UUID, Long>> THREATS = new HashMap<>();

    private Bizarre() {
    }

    static void tick(ServerPlayer player, JojohaPlayerData data, boolean bizarre) {
        boolean charming = bizarre && charismatic(player);
        boolean social = charming && company(player);
        float amp = Charms.fortune(player);
        Fortunes.modifier(player, Attributes.LUCK, LUCK_ID, social ? SOCIAL_LUCK * amp : 0.0,
                AttributeModifier.Operation.ADD_VALUE);
        Fortunes.modifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, social ? SOCIAL_SPEED * amp : 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (!bizarre) {
            return;
        }
        if (player.tickCount % THREAT_EVERY == 0) {
            threats(player);
        }
        if (player.tickCount % PULL_EVERY == 0 && !Pulls.structure(player, "message.jojoha.fortune.bizarre_pull")) {
            Pulls.biome(player, "message.jojoha.fortune.bizarre_pull");
        }
        if (charming && player.tickCount % DRAW_EVERY == 0) {
            drawOthers(player);
        }
    }

    public static boolean charismatic(Player player) {
        Set<String> kinds = new HashSet<>();
        boolean ours = true;
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) {
                return false;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(worn.getItem());
            ours &= id.getNamespace().equals(Jojoha.MOD_ID);
            kinds.add(worn.getItem() instanceof ArmorItem armour
                    ? armour.getMaterial().unwrapKey().map(key -> key.location().toString()).orElse(id.toString())
                    : id.toString());
        }
        return ours || kinds.size() == ARMOUR.length;
    }

    private static boolean company(ServerPlayer player) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && !other.isSpectator() && other.distanceToSqr(player) < SOCIAL_RANGE * SOCIAL_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static void threats(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        Map<UUID, Long> seen = THREATS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        seen.values().removeIf(at -> now - at > THREAT_AGAIN_TICKS);
        List<Integer> lurking = new ArrayList<>();
        for (LivingEntity near : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(THREAT_RANGE * Charms.fortune(player)),
                near -> near != player && near.isAlive())) {
            if (!standUser(near) || seen.containsKey(near.getUUID())) {
                continue;
            }
            seen.put(near.getUUID(), now);
            lurking.add(near.getId());
        }
        if (!lurking.isEmpty()) {
            NetworkHandler.sendFortuneSense(player, FortuneSensePacket.GLOW, THREAT_GLOW_TICKS, lurking);
            Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.threat"));
        }
    }

    private static boolean standUser(LivingEntity entity) {
        if (entity instanceof ServerPlayer other) {
            return PlayerDataAccess.get(other).stand.isPresent();
        }
        return entity instanceof Mob mob && MobStands.has(mob);
    }

    private static void drawOthers(ServerPlayer player) {
        double range = DRAW_RANGE * Charms.fortune(player);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && !other.isSpectator() && other.distanceToSqr(player) < range * range) {
                Fortunes.whisper(other, Component.translatable("message.jojoha.fortune.drawn",
                        Fortunes.direction(other.position(), player.position())));
            }
        }
    }

    public static void meteor(ServerLevel level, BlockPos at) {
        if (!Fortunes.on()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (Fortune.has(PlayerDataAccess.get(player), Fortune.BIZARRE)) {
                Fortunes.whisper(player, Component.translatable("message.jojoha.fortune.meteor",
                        at.getX(), at.getY(), at.getZ()));
            }
        }
    }

    public static void discount(Villager villager, Player player) {
        if (!Fortunes.on() || !(player instanceof ServerPlayer server)
                || !Fortune.has(PlayerDataAccess.get(server), Fortune.BIZARRE) || !charismatic(server)) {
            return;
        }
        for (MerchantOffer offer : villager.getOffers()) {
            offer.addToSpecialPriceDiff(-Math.round(offer.getBaseCostA().getCount() * TRADE_DISCOUNT
                    * Charms.fortune(server)));
        }
    }

    public static boolean finds(LivingEntity looter) {
        return Fortunes.on() && looter instanceof ServerPlayer player
                && Fortune.has(PlayerDataAccess.get(player), Fortune.BIZARRE);
    }

    static void forget(Player player) {
        THREATS.remove(player.getUUID());
    }
}
