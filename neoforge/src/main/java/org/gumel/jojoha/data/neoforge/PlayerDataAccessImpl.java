package org.gumel.jojoha.data.neoforge;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class PlayerDataAccessImpl {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Jojoha.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<JojohaPlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () -> AttachmentType.builder(JojohaPlayerData::createDefault)
                    .serialize(JojohaPlayerData.CODEC)
                    .copyOnDeath()
                    .build());

    private PlayerDataAccessImpl() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static JojohaPlayerData get(Player player) {
        JojohaPlayerData data = player.getData(PLAYER_DATA);
        data.shareEndurance(true);
        return data;
    }

    public static void set(Player player, JojohaPlayerData data) {
        data.shareEndurance(false);
        player.setData(PLAYER_DATA, data);
    }
}
