package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.workshop.Unlocks;
import org.gumel.jojoha.supporter.SkinCatalog;
import org.gumel.jojoha.supporter.SupporterLink;

public final class StandSkinChoice {
    private StandSkinChoice() {
    }

    public static void equip(ServerPlayer player, int skin) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent() || data.stand.skin() == skin) {
            return;
        }
        if (!wearable(player, data, skin)) {
            return;
        }
        if (data.standArrowRitualTicks > 0 || data.standSkinSwapTicks > 0) {
            return;
        }

        data.stand = data.stand.withSkin(skin);
        StandEntity stand = data.standSummoned ? StandEntity.findFor(player) : null;
        if (stand != null) {
            stand.setSkin(skin);
        }
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static boolean wearable(ServerPlayer player, JojohaPlayerData data, int skin) {
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        boolean allowed = type.supporterSkin(skin)
                ? SupporterLink.hasSkins(player) || Unlocks.all(data)
                : data.canWearSkin(data.stand.standId(), skin);
        if (SkinCatalog.remote(skin)) {
            SkinCatalog.Entry entry = SkinCatalog.get(skin);
            return allowed && entry != null && entry.stand().equals(data.stand.standId());
        }
        return allowed && skin >= 0 && skin < type.skinCount();
    }
}
