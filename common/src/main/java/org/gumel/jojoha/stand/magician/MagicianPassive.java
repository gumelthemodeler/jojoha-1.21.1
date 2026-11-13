package org.gumel.jojoha.stand.magician;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.passive.StandPassive;

public final class MagicianPassive implements StandPassive {
    public static final MagicianPassive TERRITORIAL = new MagicianPassive("territorial");
    public static final MagicianPassive ENCROACHMENT = new MagicianPassive("encroachment");
    public static final MagicianPassive FLAME_IMMUNITY = new MagicianPassive("flame_immunity");
    public static final MagicianPassive BURNING_PRESSURE = new MagicianPassive("burning_pressure");

    private final ResourceLocation id;

    private MagicianPassive(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha." + id.getPath();
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target, float amount) {
        if (this == TERRITORIAL) {
            return Magician.inArea(player)
                    ? amount * StandTuning.value("territorial_damage", Magician.TERRITORIAL_DAMAGE)
                    : amount;
        }
        if (this != BURNING_PRESSURE) {
            return amount;
        }
        if (target.hasEffect(ModEffects.burning())
                && player.getRandom().nextFloat() < StandTuning.value("burning_pressure_chance", Magician.PRESSURE_CHANCE)) {
            Stun.stagger(target);
        }
        if (!Magician.inferno(player)) {
            return amount;
        }
        Burning.apply(target);
        return amount * StandTuning.value("inferno_damage", Magician.INFERNO_DAMAGE);
    }
}
