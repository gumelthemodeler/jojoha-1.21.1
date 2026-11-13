package org.gumel.jojoha.stand;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.packet.StingPacket;
import org.gumel.jojoha.stand.mob.MobStands;

public final class FightStings {
    public static final int DEFEAT = 1;
    public static final int DEFEAT_PREVIEW = 2;
    public static final float CHANCE = 0.2F;

    private FightStings() {
    }

    public static void init() {
        EntityEvent.LIVING_DEATH.register(FightStings::onDeath);
    }

    private static EventResult onDeath(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide() || !(source.getEntity() instanceof LivingEntity killer) || killer == victim) {
            return EventResult.pass();
        }
        boolean standFight = standUser(victim) || standUser(killer);
        if (!standFight) {
            return EventResult.pass();
        }
        if (victim instanceof ServerPlayer loser && victim.getRandom().nextFloat() < CHANCE) {
            NetworkManager.sendToPlayer(loser, new StingPacket(DEFEAT));
        }
        return EventResult.pass();
    }

    private static boolean standUser(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return PlayerDataAccess.get(player).stand.isPresent();
        }
        return entity instanceof Mob mob && MobStands.has(mob);
    }
}
