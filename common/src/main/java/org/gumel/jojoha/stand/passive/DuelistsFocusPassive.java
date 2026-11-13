package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

public final class DuelistsFocusPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "duelists_focus");
    public static final DuelistsFocusPassive INSTANCE = new DuelistsFocusPassive();

    public static final int WINDOW_BONUS_TICKS = 2;

    private DuelistsFocusPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.duelists_focus";
    }

    public static boolean on(JojohaPlayerData data) {
        return StandPassives.grants(data, ID);
    }

    public static boolean guards(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return data.standGuarding && on(data);
    }
}
