package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.fortune.Trickster;
import org.gumel.jojoha.item.Curios;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.anubis.Passing;
import org.gumel.jojoha.stand.passive.DuelistsFocusPassive;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.DrawSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Parry {
    public static final String[] BARE = {"hand_parry", "hand_parry2"};
    public static final String[] BARE_FIRST_PERSON = {"hand_parry_fp", "hand_parry2_fp"};
    public static final String[] SWORD = {"sword_parry", "sword_parry2"};
    public static final String[] SWORD_FIRST_PERSON = {"sword_parry_fp", "sword_parry2_fp"};

    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private Parry() {
    }

    private static int pickVariant(ServerPlayer player, int count) {
        Integer last = LAST.get(player.getUUID());
        int pick = pick(last == null ? -1 : last, count, player.getRandom());
        LAST.put(player.getUUID(), pick);
        return pick;
    }

    public static void forget(Player player) {
        LAST.remove(player.getUUID());
    }

    public static int pick(int last, int count, RandomSource random) {
        if (last >= count) {
            last = -1;
        }
        int pick = random.nextInt(last < 0 ? count : count - 1);
        if (last >= 0 && pick >= last) {
            pick++;
        }
        return pick;
    }

    public static void request(ServerPlayer player, int asked) {
        if (player.isSpectator() || !player.isAlive() || !canParry(player)) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (Anubis.out(player)) {
            if (player.isShiftKeyDown()) {
                Passing.letGo(player);
                return;
            }
            StandSkills.usePassive(player,
                    DrawSkill.INSTANCE);
            return;
        }
        if (standGuardsInstead(player, data)) {
            return;
        }
        if (GuardBreak.isBroken(player) || data.guardBreakCooldownTicks > 0) {
            return;
        }
        if (!Defence.openParry(player, windowFor(player))) {
            return;
        }
        SabreParry.onGuardRaised(player);
        parryLate(player, data);
        boolean armed = armed(player);
        String[] clips = armed ? SWORD : BARE;
        int variant = asked >= 0 && asked < clips.length ? asked : pickVariant(player, clips.length);
        LAST.put(player.getUUID(), variant);
        MovementFx.pose(player, clips[variant]);
        NetworkHandler.sendFirstPersonPose(player, (armed ? SWORD_FIRST_PERSON : BARE_FIRST_PERSON)[variant]);
    }

    public static void duel(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standGuarding || !DuelistsFocusPassive.on(data)
                || !Defence.openParry(player, windowFor(player)
                        + DuelistsFocusPassive.WINDOW_BONUS_TICKS)) {
            return;
        }
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.45F, 1.9F);
        parryLate(player, PlayerDataAccess.get(player));
    }

    public static boolean standGuardsInstead(ServerPlayer player, JojohaPlayerData data) {
        return data.standSummoned && data.stand.isPresent() && data.stand.trust().canGuard()
                && !data.standMode.handlesItems()
                && StandTypes.hasBody(data)
                && (player.getMainHandItem().isEmpty() || BarragePressure.on(player));
    }

    public static boolean canParry(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        return held.isEmpty() || held.is(ItemTags.SWORDS) && !WeaponClasses.reaps(player);
    }

    public static boolean armed(ServerPlayer player) {
        return player.getMainHandItem().is(ItemTags.SWORDS);
    }

    private static int windowFor(ServerPlayer player) {
        return Defence.PARRY_WINDOW_TICKS + Anubis.windowBonus(player)
                + Curios.parryBonus(player) + Trickster.parryBonus(player);
    }

    static boolean meets(ServerPlayer defender, DamageSource source) {
        JojohaPlayerData data = PlayerDataAccess.get(defender);
        if (data.standGuarding && !DuelistsFocusPassive.on(data)
                || !Defence.inParryWindow(defender)) {
            return false;
        }
        StandEntity stand = data.standSummoned ? StandEntity.findFor(defender) : null;
        if (!Defence.tryParry(defender, source, colour(data, stand))) {
            return false;
        }
        if (stand != null && source.getEntity() instanceof LivingEntity attacker) {
            stand.triggerPunchAt(attacker, false);
        }
        restore(defender, data, stand);
        Trickster.tricked(defender);
        return true;
    }

    private static void restore(ServerPlayer defender, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return;
        }
        StandCombatHandler.restoreFromParry(data);
        PlayerDataAccess.set(defender, data);
        PlayerDataAccess.syncSoon(defender);
    }

    private static void parryLate(ServerPlayer defender, JojohaPlayerData data) {
        StandEntity stand = data.standSummoned ? StandEntity.findFor(defender) : null;
        LivingEntity attacker = Defence.parryLate(defender, colour(data, stand));
        if (attacker != null) {
            Trickster.tricked(defender);
        }
        if (attacker != null && stand != null) {
            stand.triggerPunchAt(attacker, false);
            restore(defender, data, stand);
        }
    }

    private static int colour(JojohaPlayerData data, StandEntity stand) {
        return stand != null
                ? StandShadowForm.auraColourFor(stand)
                : Impacts.PLAYER;
    }
}
