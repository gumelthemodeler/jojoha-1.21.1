package org.gumel.jojoha.stand.magician;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.function.Function;

public final class MagicianMove implements StandSkill {
    @FunctionalInterface
    public interface Cast {
        boolean cast(ServerPlayer player, JojohaPlayerData data, StandEntity stand);
    }

    public static final MagicianMove CONTROLLED_BLAZE =
            new MagicianMove("controlled_blaze", 400, EnergyWeight.HEAVY, Magician::controlledBlaze, null);
    public static final MagicianMove SWEEP_KICK =
            new MagicianMove("sweep_kick", 120, EnergyWeight.LIGHT, Magician::sweepKick, null);
    public static final MagicianMove CROSSFIRE_HURRICANE =
            new MagicianMove("crossfire_hurricane", 160, EnergyWeight.STANDARD, Magician::crossfire, null);
    public static final MagicianMove CROSSFIRE_HURRICANE_SPECIAL =
            new MagicianMove("crossfire_hurricane_special", 360, EnergyWeight.HEAVY, Magician::crossfireSpecial, null);
    public static final MagicianMove RED_BIND =
            new MagicianMove("red_bind", 280, EnergyWeight.STANDARD, Magician::redBind, null);
    public static final MagicianMove INFERNO =
            new MagicianMove("inferno", 600, EnergyWeight.STANDARD, Magician::beginInferno,
                    player -> Magician.inArea(player) ? null : "message.jojoha.magician.no_area");
    public static final MagicianMove CONTAINMENT =
            new MagicianMove("containment", 320, EnergyWeight.STANDARD, Magician::containment, null);
    public static final MagicianMove LIFE_SENSE =
            new MagicianMove("life_sense", 300, EnergyWeight.LIGHT, Magician::lifeSense, null);
    public static final MagicianMove INCREASE_TEMPERATURE =
            new MagicianMove("increase_temperature", 200, EnergyWeight.STANDARD, Magician::increaseTemperature, null);
    public static final MagicianMove FINGER_SNAP =
            new MagicianMove("finger_snap", 40, EnergyWeight.TRIVIAL, Magician::fingerSnap, null);
    public static final MagicianMove INFINITE_INFERNO_FIRE =
            new MagicianMove("infinite_inferno_fire", 6000, EnergyWeight.ULTIMATE, Magician::infiniteInferno, null);

    private final ResourceLocation id;
    private final int cooldown;
    private final EnergyWeight weight;
    private final Cast cast;
    private final Function<ServerPlayer, String> gate;

    private MagicianMove(String path, int cooldown, EnergyWeight weight, Cast cast,
                         Function<ServerPlayer, String> gate) {
        this.id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        this.cooldown = cooldown;
        this.weight = weight;
        this.cast = cast;
        this.gate = gate;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha." + id.getPath();
    }

    @Override
    public int cooldownTicks() {
        return cooldown;
    }

    @Override
    public float energyCost() {
        return weight.cost();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return gate == null ? null : gate.apply(player);
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return Magician.is(stand) && cast.cast(player, data, stand);
    }
}
