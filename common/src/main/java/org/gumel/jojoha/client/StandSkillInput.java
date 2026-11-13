package org.gumel.jojoha.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.ultimate.AdaptMove;

public final class StandSkillInput {
    private static boolean piloting;

    private static KeyMapping[] slotKeys = new KeyMapping[0];

    private static int[] held = new int[0];
    private static boolean[] fired = new boolean[0];

    private static boolean[] sustaining = new boolean[0];

    private static int[] sustainedSlot = new int[0];

    public static boolean slotNeedsModifier(int row) {
        return slotKeys.length > 0 && row >= slotKeys.length;
    }

    public static Component slotKeyLabel(int row) {
        if (row < 0 || slotKeys.length == 0) {
            return Component.empty();
        }

        boolean modified = row >= slotKeys.length;
        int index = modified ? row - slotKeys.length : row;
        if (index >= slotKeys.length) {
            return Component.empty();
        }

        Component key = abbreviate(slotKeys[index]);

        return modified ? Component.literal("[").append(key).append("]") : key;
    }

    private static Component abbreviate(KeyMapping mapping) {
        String name = mapping.saveString();
        if (!name.startsWith(MOUSE_PREFIX)) {
            return mapping.getTranslatedKeyMessage();
        }

        String button = switch (name.substring(MOUSE_PREFIX.length())) {
            case "left" -> "1";
            case "right" -> "2";
            case "middle" -> "3";
            default -> name.substring(MOUSE_PREFIX.length());
        };
        return Component.literal("M" + button);
    }

    private static final String MOUSE_PREFIX = "key.mouse.";

    private StandSkillInput() {
    }

    private static boolean canStart(StandSkill skill) {
        JojohaPlayerData data = ClientPlayerDataCache.data;

        if (skill.requiresStand() && !data.standSummoned) {
            return false;
        }

        if (!skill.isUnlocked(data)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && data.isMoveOnCooldown(skill.id(), minecraft.level.getGameTime())) {
            return false;
        }

        return !skill.isRunning(data);
    }

    private static boolean announcedCharge;

    private static void announceCharge(boolean charging) {
        if (charging == announcedCharge) {
            return;
        }
        announcedCharge = charging;
        NetworkHandler.sendStandCharge(charging);
    }

    private static float lastHealth = Float.NaN;

    private static boolean tookAHit(Minecraft minecraft) {
        if (minecraft.player == null) {
            lastHealth = Float.NaN;
            return false;
        }

        float health = minecraft.player.getHealth();
        boolean hit = !Float.isNaN(lastHealth) && health < lastHealth - 0.01F;
        lastHealth = health;
        return hit;
    }

    private static void endSustain(int row) {
        if (row >= sustaining.length || !sustaining[row]) {
            return;
        }
        sustaining[row] = false;
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkHandler.sendUseStandSkill(sustainedSlot[row], StandSkills.RELEASE);
        }
    }

    private static void releaseAll() {
        boolean anything = false;
        for (int row = 0; row < held.length; row++) {
            if (held[row] > 0) {
                anything = true;
            }
            held[row] = 0;
            fired[row] = false;
        }
        for (int row = 0; row < sustaining.length; row++) {
            endSustain(row);
        }

        if (anything) {
            TimeStopCharge.release();
        }

        announceCharge(false);
    }

    public static boolean isPiloting() {
        return piloting;
    }

    public static void tick(KeyMapping[] keys) {
        slotKeys = keys;
        Minecraft minecraft = Minecraft.getInstance();

        if (CentralBarOverlay.showing()) {
            tickSlots(minecraft, keys);
        } else {
            releaseAll();
        }

        tickPilot(minecraft);
    }

    private static void tickSlots(Minecraft minecraft, KeyMapping[] keys) {
        int pageOffset = CombatBarOverlay.currentSkillPage() * StandSkills.SLOTS_PER_PAGE;

        if (held.length != StandSkills.SLOTS_PER_PAGE) {
            held = new int[StandSkills.SLOTS_PER_PAGE];
            fired = new boolean[StandSkills.SLOTS_PER_PAGE];
            sustaining = new boolean[StandSkills.SLOTS_PER_PAGE];
            sustainedSlot = new int[StandSkills.SLOTS_PER_PAGE];
        }

        boolean modifierDown = RawKey.modifierDown();
        boolean hit = tookAHit(minecraft);

        for (int row = 0; row < StandSkills.SLOTS_PER_PAGE; row++) {
            boolean modified = row >= keys.length;
            int index = modified ? row - keys.length : row;
            if (index >= keys.length) {
                continue;
            }

            if (modifierDown != modified) {
                if (held[row] > 0) {
                    if (minecraft.player != null) {
                        NetworkHandler.sendUseStandSkill(pageOffset + row, held[row]);
                    }
                    held[row] = 0;
                    fired[row] = false;
                    TimeStopCharge.release();
                }
                endSustain(row);
                continue;
            }

            KeyMapping key = keys[index];
            int slot = pageOffset + row;
            StandSkill skill = StandSkills.skillInSlot(ClientPlayerDataCache.data, slot);

            if (skill == null) {
                ResourceLocation inSlot = StandSkills.idInSlot(ClientPlayerDataCache.data, slot);

                boolean pressed = RawKey.pressed(key);
                boolean down = RawKey.isDown(key);

                if (held[row] > 0) {
                    held[row] = 0;
                    TimeStopCharge.release();
                }
                fired[row] = false;

                if (minecraft.player == null) {
                    endSustain(row);
                    continue;
                }

                if (Moves.isSustained(inSlot)) {
                    if (pressed && !sustaining[row]) {
                        NetworkHandler.sendUseStandSkill(slot, 0);
                        sustaining[row] = true;
                        sustainedSlot[row] = slot;
                    } else if (!down && sustaining[row]) {
                        endSustain(row);
                    }
                    continue;
                }

                endSustain(row);
                if (pressed && AdaptMove.ID.equals(inSlot) && minecraft.player.isShiftKeyDown()) {
                    minecraft.setScreen(new AdaptationScreen());
                    continue;
                }
                if (pressed && inSlot != null) {
                    NetworkHandler.sendUseStandSkill(slot, 0);
                }
                continue;
            }

            int chargeMax = skill.chargeMaxTicks(ClientPlayerDataCache.data);

            if (skill.isSustained()) {
                held[row] = 0;

                boolean pressed = RawKey.pressed(key);
                boolean down = RawKey.isDown(key);
                boolean running = minecraft.player != null && skill.isSustainActive(minecraft.player);

                if (pressed && !running && !sustaining[row] && minecraft.player != null) {
                    NetworkHandler.sendUseStandSkill(slot, 0);
                    sustaining[row] = true;
                    sustainedSlot[row] = slot;
                } else if (!down && sustaining[row] && minecraft.player != null) {
                    endSustain(row);
                } else if (!down && running && minecraft.player != null) {
                    NetworkHandler.sendUseStandSkill(slot, StandSkills.RELEASE);
                }
                continue;
            }

            if (chargeMax <= 0) {
                held[row] = 0;
                if (RawKey.pressed(key) && minecraft.player != null
                        && (canStart(skill) || skill.canBeCalledOff(ClientPlayerDataCache.data))) {
                    NetworkHandler.sendUseStandSkill(slot, 0);
                }
                continue;
            }

            if (RawKey.pressed(key)) {
                if (skill.canBeCalledOff(ClientPlayerDataCache.data) && minecraft.player != null) {
                    NetworkHandler.sendUseStandSkill(slot, 0);
                    held[row] = 0;
                    fired[row] = true;
                    TimeStopCharge.release();
                    continue;
                }
            }

            if (RawKey.isDown(key)) {
                if (fired[row]) {
                    continue;
                }

                if (!canStart(skill)) {
                    if (held[row] > 0) {
                        held[row] = 0;
                        TimeStopCharge.release();
                        if (skill.chargesTime()) {
                            NetworkHandler.sendUseStandSkill(slot, StandSkills.CHARGE_DROPPED);
                        }
                    }
                    continue;
                }

                if (hit && skill.chargeBreaksOnDamage() && held[row] > 0) {
                    held[row] = 0;
                    TimeStopCharge.release();
                    if (skill.chargesTime()) {
                        NetworkHandler.sendUseStandSkill(slot, StandSkills.CHARGE_BROKEN);
                    }
                    continue;
                }

                int chargeEnd = chargeMax;
                held[row] = Math.min(held[row] + 1, chargeEnd);
                if (held[row] == 1 && skill.chargesTime()) {
                    NetworkHandler.sendUseStandSkill(slot, StandSkills.CHARGE_BEGIN);
                }
                if (skill.chargesTime()) {
                    TimeStopCharge.hold(held[row], chargeMax, skill.timeStopTicks(ClientPlayerDataCache.data));
                } else if (held[row] == 1 && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable(
                                    "message.jojoha.skill.focusing"), true);
                }

                if (held[row] >= chargeEnd && minecraft.player != null) {
                    NetworkHandler.sendUseStandSkill(slot, held[row]);
                    held[row] = 0;
                    fired[row] = true;
                    TimeStopCharge.release();
                }
            } else {
                fired[row] = false;

                if (held[row] > 0) {
                    if (minecraft.player != null) {
                        boolean tooShort = skill.chargesTime() && held[row] < chargeMax
                                && !TimeStopSkill.tapped(held[row]);
                        NetworkHandler.sendUseStandSkill(slot, tooShort ? StandSkills.CHARGE_DROPPED : held[row]);
                        if (tooShort) {
                            minecraft.player.displayClientMessage(Component.translatable(
                                    "message.jojoha.skill.time_stop_short").withStyle(ChatFormatting.GRAY), true);
                        }
                    }
                    held[row] = 0;
                    TimeStopCharge.release();
                }
            }
        }

        boolean contested = false;
        for (int row = 0; row < StandSkills.SLOTS_PER_PAGE && !contested; row++) {
            if (held[row] <= 0) {
                continue;
            }
            StandSkill charging = StandSkills.skillInSlot(ClientPlayerDataCache.data,
                    CombatBarOverlay.currentSkillPage() * StandSkills.SLOTS_PER_PAGE + row);
            contested = charging != null && charging.chargeBreaksOnDamage();
        }
        announceCharge(contested && minecraft.player != null);
    }

    private static void tickPilot(Minecraft minecraft) {
        boolean shouldPilot = ClientPlayerDataCache.data.standPiloting
                && minecraft.player != null
                && minecraft.player.isAlive()
                && minecraft.level != null;

        if (shouldPilot != piloting) {
            piloting = shouldPilot;
            applyCamera(minecraft, shouldPilot);
        } else if (piloting && !alive(minecraft.getCameraEntity())) {
            applyCamera(minecraft, true);
        } else if (piloting && minecraft.getCameraEntity() == minecraft.player) {
            applyCamera(minecraft, true);
        }

        if (!piloting || minecraft.player == null) {
            return;
        }

        var options = minecraft.options;
        float forward = (options.keyUp.isDown() ? 1F : 0F) - (options.keyDown.isDown() ? 1F : 0F);
        float strafe = (options.keyLeft.isDown() ? 1F : 0F) - (options.keyRight.isDown() ? 1F : 0F);
        boolean up = options.keyJump.isDown();
        boolean down = options.keyShift.isDown();

        StandEntityLookup.localStand(minecraft)
                .filter(entity -> entity instanceof StandEntity)
                .map(entity -> (StandEntity) entity)
                .filter(StandEntity::isPiloted)
                .ifPresent(stand -> {
                    stand.setLocallyPiloted(true);
                    PilotSystem.advance(
                            minecraft.player, stand, forward, strafe, up, down);
                    NetworkHandler.sendPilotPose(stand.getX(), stand.getY(), stand.getZ(),
                            stand.getYRot(), stand.getXRot());
                });
    }

    private static boolean alive(Entity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    private static void applyCamera(Minecraft minecraft, boolean pilot) {
        if (minecraft.player == null) {
            return;
        }

        if (!pilot) {
            minecraft.setCameraEntity(minecraft.player);

            StandEntityLookup.localStand(minecraft)
                    .filter(entity -> entity instanceof StandEntity)
                    .map(entity -> (StandEntity) entity)
                    .ifPresent(stand -> {
                        stand.setLocallyPiloted(false);
                        stand.setPilotVelocity(Vec3.ZERO);
                    });
            return;
        }

        StandEntityLookup.localStand(minecraft).ifPresentOrElse(
                minecraft::setCameraEntity,
                () -> minecraft.setCameraEntity(minecraft.player));
    }
}
