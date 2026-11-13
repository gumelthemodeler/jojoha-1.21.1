package org.gumel.jojoha.stand.passive;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GrapplingVinePassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine");

    private static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine_speed");
    private static final ResourceLocation AIR_KNOCKBACK_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine_air");

    private static final double SPEED_BONUS = 0.12;
    private static final double AIR_KNOCKBACK = 1.0;

    private static final double KEEN_SPEED_BONUS = 0.24;
    private static final float KEEN_FALL_KEPT = 0.2F;

    private static final float FALL_KEPT = 0.55F;

    private static final Map<UUID, Float> LAST_FALL = new HashMap<>();

    public static final ResourceLocation KEEN_ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "grappling_vine_ii");

    public static final GrapplingVinePassive INSTANCE =
            new GrapplingVinePassive(ID, SPEED_BONUS, FALL_KEPT, null);

    public static final GrapplingVinePassive KEEN =
            new GrapplingVinePassive(KEEN_ID, KEEN_SPEED_BONUS, KEEN_FALL_KEPT, ID);

    private final ResourceLocation id;
    private final double speedBonus;
    private final float fallKept;
    private final ResourceLocation replaces;

    private GrapplingVinePassive(ResourceLocation id, double speedBonus, float fallKept,
                                 ResourceLocation replaces) {
        this.id = id;
        this.speedBonus = speedBonus;
        this.fallKept = fallKept;
        this.replaces = replaces;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ResourceLocation replaces() {
        return replaces;
    }

    @Override
    public String translationKey() {
        return "passive." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public void tick(ServerPlayer player, JojohaPlayerData data) {
        set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, speedBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);

        set(player, Attributes.KNOCKBACK_RESISTANCE, AIR_KNOCKBACK_ID, AIR_KNOCKBACK,
                AttributeModifier.Operation.ADD_VALUE, !player.onGround());

        soften(player, fallKept);
    }

    private static void soften(ServerPlayer player, float kept) {
        UUID id = player.getUUID();

        if (player.onGround() || player.fallDistance <= 0F) {
            LAST_FALL.remove(id);
            return;
        }

        float previous = LAST_FALL.getOrDefault(id, 0F);
        float growth = player.fallDistance - previous;

        if (growth > 0F) {
            player.fallDistance = previous + growth * kept;
        }
        LAST_FALL.put(id, player.fallDistance);
    }

    private static void set(ServerPlayer player,
                           Holder<Attribute> attribute,
                           ResourceLocation id, double amount,
                           AttributeModifier.Operation operation, boolean wanted) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (wanted == (existing != null)) {
            return;
        }

        if (wanted) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        } else {
            instance.removeModifier(id);
        }
    }
}
