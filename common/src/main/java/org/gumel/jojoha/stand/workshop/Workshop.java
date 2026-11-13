package org.gumel.jojoha.stand.workshop;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandPose;
import org.gumel.jojoha.data.WorkshopLook;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.StandCustomization;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSkinChoice;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.Optional;

public final class Workshop {
    private static final int SCAN_TICKS = 20;
    public static final String OVER_HEAVEN = "skin.jojoha.the_world.over_heaven";

    private Workshop() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % SCAN_TICKS == 0) {
                scan(serverPlayer);
            }
        });
        EntityEvent.LIVING_DEATH.register(Workshop::onDeath);
    }

    private static void scan(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean added = false;
        if (data.stand.isPresent()) {
            added |= data.workshopUnlocks.add(Unlocks.standKey(data.stand.standId()));
        }
        if (data.anubis) {
            added |= data.workshopUnlocks.add(Unlocks.standKey(StandTypes.ANUBIS_ID));
        }
        if (added) {
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        }
    }

    private static EventResult onDeath(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayer player) {
            glimpseHeaven(player, source);
        }
        return EventResult.pass();
    }

    private static Entity killer(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof StandEntity stand && stand.getUser() != null) {
            return stand.getUser();
        }
        return entity;
    }

    private static void glimpseHeaven(ServerPlayer player, DamageSource source) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent() || !StandTypes.THE_WORLD_ID.equals(data.stand.standId())
                || !holdsHeavensArrow(player) || !(killer(source) instanceof Mob mob) || !MobStands.has(mob)) {
            return;
        }
        StandType type = StandTypes.byIdOrDefault(StandTypes.THE_WORLD_ID);
        int skin = type.skinIndex(OVER_HEAVEN);
        if (skin < 0 || !data.commitSkin(StandTypes.THE_WORLD_ID, skin)) {
            return;
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        player.sendSystemMessage(Component.translatable("message.jojoha.workshop.over_heaven")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static boolean holdsHeavensArrow(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.HEAVENS_ARROW.get())) {
                return true;
            }
        }
        return false;
    }

    public static void style(ServerPlayer player, int summon, int shout, int rest) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        SummonStyle style = SummonStyle.of(summon);
        BarrageShout cry = BarrageShout.of(shout);
        if (!Unlocks.style(data, style) || !Unlocks.shout(data, cry)) {
            return;
        }
        data.summonStyle = style.ordinal();
        data.barrageShout = cry.ordinal();
        RestPose pose = RestPose.of(rest);
        data.restPose = data.stand.isPresent() && pose.fits(StandTypes.byIdOrDefault(data.stand.standId()))
                ? pose.ordinal() : RestPose.CALM.ordinal();
        StandEntity stand = data.standSummoned ? StandEntity.findFor(player) : null;
        if (stand != null) {
            stand.setLook(data.restPose, data.barrageShout);
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void pose(ServerPlayer player, int slot, StandPose pose) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (slot < 0 || slot >= data.poses.length) {
            return;
        }
        data.poses[slot] = pose.tidy();
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void look(ServerPlayer player, int slot, boolean save, int skin) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (slot < 0 || slot >= data.looks.length || !data.stand.isPresent()) {
            return;
        }
        if (save) {
            data.looks[slot] = new WorkshopLook(Optional.of(data.stand.standId()),
                    StandSkinChoice.wearable(player, data, skin) ? skin : data.stand.skin(),
                    data.standPlacement, data.restPose, data.barrageShout, data.summonStyle, data.standIdle);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }
        WorkshopLook look = data.looks[slot];
        if (!look.saved()) {
            return;
        }
        if (look.stand().get().equals(data.stand.standId())) {
            StandSkinChoice.equip(player, look.skin());
        }
        StandCustomization.place(player, look.placement());
        style(player, look.summon(), look.shout(), look.rest());
    }
}
