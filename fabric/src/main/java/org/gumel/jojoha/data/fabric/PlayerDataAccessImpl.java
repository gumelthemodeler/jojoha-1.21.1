package org.gumel.jojoha.data.fabric;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class PlayerDataAccessImpl {
    public static final AttachmentType<JojohaPlayerData> PLAYER_DATA = AttachmentRegistry
            .<JojohaPlayerData>builder()
            .persistent(JojohaPlayerData.CODEC)
            .copyOnDeath()
            .initializer(JojohaPlayerData::createDefault)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "player_data"));

    private PlayerDataAccessImpl() {
    }

    public static void bootstrap() {
    }

    public static JojohaPlayerData get(Player player) {
        JojohaPlayerData data = player.getAttachedOrCreate(PLAYER_DATA, JojohaPlayerData::createDefault);
        data.shareEndurance(true);
        return data;
    }

    public static void set(Player player, JojohaPlayerData data) {
        data.shareEndurance(false);
        player.setAttached(PLAYER_DATA, data);
    }
}
