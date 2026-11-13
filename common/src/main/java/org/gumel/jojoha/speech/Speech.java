package org.gumel.jojoha.speech;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.StandTypes;

import java.util.Set;

public final class Speech {
    public static final double RANGE = 32.0;

    public static final ResourceLocation SHIMMER = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "shimmer");

    private Speech() {
    }

    public static void say(ServerPlayer speaker, Component line) {
        Component message = Component.translatable("chat.type.text", speaker.getDisplayName(), line);
        double reach = RANGE * RANGE;
        for (ServerPlayer listener : speaker.serverLevel().players()) {
            if (listener.distanceToSqr(speaker) <= reach) {
                listener.sendSystemMessage(message);
            }
        }
    }

    public static void say(ServerPlayer speaker, String key, ChatFormatting colour, Object... args) {
        say(speaker, Component.translatable(key, args).withStyle(colour));
    }

    public static int standColour(ServerPlayer speaker) {
        JojohaPlayerData data = PlayerDataAccess.get(speaker);
        return StandShadowForm.auraColourFor(data.stand, speaker.getUUID()) & 0xFFFFFF;
    }

    public static MutableComponent standName(ServerPlayer speaker) {
        JojohaPlayerData data = PlayerDataAccess.get(speaker);
        return Component.translatable("stand." + data.stand.standId().getNamespace() + "." + data.stand.standId().getPath())
                .withColor(standColour(speaker));
    }

    public static MutableComponent shimmer(MutableComponent text) {
        return text.withStyle(style -> style.withFont(SHIMMER));
    }

    private static final Set<ResourceLocation> OWN_TIME_STOP_LINES = Set.of(
            StandTypes.STAR_PLATINUM_ID, StandTypes.THE_WORLD_ID, StandTypes.SECRET_THE_WORLD_ID);

    public static void timeStopped(ServerPlayer speaker) {
        ResourceLocation id = PlayerDataAccess.get(speaker).stand.standId();
        String key = OWN_TIME_STOP_LINES.contains(id) ? "speech.jojoha.time_stop." + id.getPath() : "speech.jojoha.time_stop";
        say(speaker, shimmer(Component.translatable(key).withColor(standColour(speaker))));
    }

    public static void timeResumed(ServerPlayer speaker) {
        say(speaker, Component.translatable("speech.jojoha.time_resumed").withColor(standColour(speaker)));
    }

    public static void timeCount(ServerPlayer speaker, int seconds) {
        say(speaker, Component.translatable("speech.jojoha.time_count", seconds).withColor(standColour(speaker)));
    }

    public static void timeStopBroken(ServerPlayer speaker) {
        say(speaker, "speech.jojoha.time_stop_broken", ChatFormatting.RED);
    }

    public static void summoned(ServerPlayer speaker) {
        say(speaker, Component.translatable("speech.jojoha.summon", standName(speaker)));
    }

    public static void spent(ServerPlayer speaker) {
        say(speaker, Component.translatable("speech.jojoha.stand_spent", standName(speaker)).withStyle(ChatFormatting.GRAY));
    }
}
