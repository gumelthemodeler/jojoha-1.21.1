package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.client.fp.FirstPersonPoses;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.Parry;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;

public final class ParryInput {
    private static boolean held;
    private static boolean fresh;
    private static int lastVariant = -1;

    private ParryInput() {
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || !mc.options.keyUse.isDown()) {
            held = false;
        }
    }

    public static boolean useStarted() {
        fresh = !held;
        held = true;
        return fresh;
    }

    public static void nothingElseTookIt() {
        Minecraft mc = Minecraft.getInstance();
        if (fresh && canParry(mc)) {
            parry();
        }
        fresh = false;
    }

    public static void parry() {
        fresh = false;
        Minecraft mc = Minecraft.getInstance();
        int variant = -1;
        if (mc.player != null && mc.level != null && predicts(mc)) {
            boolean armed = mc.player.getMainHandItem().is(ItemTags.SWORDS);
            String[] clips = armed ? Parry.SWORD : Parry.BARE;
            variant = Parry.pick(lastVariant, clips.length, mc.level.getRandom());
            lastVariant = variant;
            float now = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
            PlayerMoveAnimation.begin(mc.player.getId(), clips[variant], now);
            FirstPersonPoses.play((armed ? Parry.SWORD_FIRST_PERSON
                    : Parry.BARE_FIRST_PERSON)[variant]);
        }
        NetworkHandler.sendRequestParry(variant);
    }

    private static boolean predicts(Minecraft mc) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        if (data == null || data.guardBreakCooldownTicks > 0
                || data.isMoveOnCooldown(Defence.PARRY_CHARGE, mc.level.getGameTime())) {
            return false;
        }
        boolean empty = mc.player.getMainHandItem().isEmpty();
        return !(data.standSummoned && data.stand.isPresent() && empty) && !(data.anubis && !empty);
    }

    private static boolean canParry(Minecraft mc) {
        if (mc.player == null || mc.screen != null || mc.player.isSpectator() || mc.gameMode == null
                || mc.gameMode.isDestroying() || mc.player.isHandsBusy()) {
            return false;
        }
        ItemStack held = mc.player.getMainHandItem();
        return held.isEmpty() || held.is(ItemTags.SWORDS)
                && !WeaponClasses.reaps(mc.player);
    }
}
