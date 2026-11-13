package org.gumel.jojoha.combat;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.skill.moves.BurstSkill;
import org.gumel.jojoha.stand.skill.moves.DodgeSkill;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.StandLeapSkill;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;

import java.util.Locale;

public final class MovementCharges {
    public enum Kind {
        LEAP, DASH
    }

    public static final int CHARGES = 2;

    public static final int BETWEEN_TICKS = 10;

    public static final int RECHARGE_TICKS = 140;

    private static final ResourceLocation[][] SLOTS = new ResourceLocation[Kind.values().length][CHARGES];

    static {
        for (Kind kind : Kind.values()) {
            for (int i = 0; i < CHARGES; i++) {
                SLOTS[kind.ordinal()][i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                        "charge/" + kind.name().toLowerCase(Locale.ROOT) + "_" + i);
            }
        }
    }

    private MovementCharges() {
    }

    public static Kind kindOf(ResourceLocation moveId) {
        if (StandDashSkill.ID.equals(moveId) || DodgeSkill.ID.equals(moveId) || TimeSkipSkill.ID.equals(moveId)) {
            return Kind.DASH;
        }
        if (StandLeapSkill.ID.equals(moveId) || BurstSkill.ID.equals(moveId)) {
            return Kind.LEAP;
        }
        return null;
    }

    private static ResourceLocation[] movesOf(Kind kind) {
        return kind == Kind.DASH
                ? new ResourceLocation[] {StandDashSkill.ID, DodgeSkill.ID, TimeSkipSkill.ID}
                : new ResourceLocation[] {StandLeapSkill.ID, BurstSkill.ID};
    }

    public static ResourceLocation slot(Kind kind, int index) {
        return SLOTS[kind.ordinal()][index];
    }

    public static int ready(JojohaPlayerData data, Kind kind, long now) {
        int ready = 0;
        for (int i = 0; i < CHARGES; i++) {
            if (!data.isMoveOnCooldown(slot(kind, i), now)) {
                ready++;
            }
        }
        return ready;
    }

    public static void refresh(JojohaPlayerData data, Kind kind) {
        for (int i = 0; i < CHARGES; i++) {
            data.moveCooldowns.remove(slot(kind, i));
        }
        for (ResourceLocation move : movesOf(kind)) {
            data.moveCooldowns.remove(move);
        }
    }

    public static void lock(JojohaPlayerData data, Kind kind, long now, int ticks) {
        for (int i = 0; i < CHARGES; i++) {
            if (data.moveCooldowns.getOrDefault(slot(kind, i), now) - now < ticks) {
                data.setMoveCooldown(slot(kind, i), now, ticks);
            }
        }
    }

    public static void spend(JojohaPlayerData data, Kind kind, long now, int rechargeTicks) {
        for (int i = 0; i < CHARGES; i++) {
            if (!data.isMoveOnCooldown(slot(kind, i), now)) {
                data.setMoveCooldown(slot(kind, i), now, rechargeTicks);
                break;
            }
        }
        int wait = BETWEEN_TICKS;
        if (ready(data, kind, now) == 0) {
            long back = Long.MAX_VALUE;
            for (int i = 0; i < CHARGES; i++) {
                back = Math.min(back, data.moveCooldowns.getOrDefault(slot(kind, i), now));
            }
            wait = (int) Math.max(BETWEEN_TICKS, back - now);
        }
        for (ResourceLocation move : movesOf(kind)) {
            data.setMoveCooldown(move, now, wait);
        }
    }
}
