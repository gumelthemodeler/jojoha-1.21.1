package org.gumel.jojoha.stand.passive;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SwordsmanshipPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "swordsmanship");

    public static final SwordsmanshipPassive INSTANCE = new SwordsmanshipPassive();

    private static final double ATTACK_SPEED = 0.4;

    private static final double REACH = 0.5;

    private static final float MARK_BONUS = 0.15F;
    private static final int MARK_TICKS = 5 * 20;

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "swordsmanship_speed");
    private static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "swordsmanship_reach");

    private static final Map<UUID, Map<UUID, Long>> MARKS = new HashMap<>();

    private SwordsmanshipPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.swordsmanship";
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        apply(player, Attributes.ATTACK_SPEED, SPEED_ID, ATTACK_SPEED);
        apply(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, REACH);
    }

    private static void apply(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || instance.getModifier(id) != null) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void clear(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
        }
        AttributeInstance reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(REACH_ID);
        }
    }

    public static void mark(ServerPlayer player, LivingEntity foe) {
        MARKS.computeIfAbsent(player.getUUID(), key -> new HashMap<>())
                .put(foe.getUUID(), player.level().getGameTime() + MARK_TICKS);
    }

    public static float markBonus(ServerPlayer player, LivingEntity foe) {
        Map<UUID, Long> marks = MARKS.get(player.getUUID());
        if (marks == null) {
            return 0F;
        }
        Long until = marks.get(foe.getUUID());
        long now = player.level().getGameTime();
        if (until == null || now > until || until - now > MARK_TICKS) {
            marks.remove(foe.getUUID());
            return 0F;
        }
        return MARK_BONUS;
    }

    public static void forget(Player player) {
        MARKS.remove(player.getUUID());
    }
}
