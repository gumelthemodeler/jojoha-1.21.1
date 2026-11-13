package org.gumel.jojoha.stand.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;

public interface StandSkill {
    ResourceLocation id();

    String translationKey();

    default String translationKeyFor(JojohaPlayerData data) {
        return translationKey();
    }

    default boolean requiresStand() {
        return true;
    }

    default boolean passiveOnly() {
        return false;
    }

    default boolean isUnlocked(JojohaPlayerData data) {
        return SkillTrees.skillUnlocked(data, id());
    }

    default ResourceLocation replaces() {
        return null;
    }

    default boolean supplants() {
        return false;
    }

    default String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return null;
    }

    default boolean isAnswerToBeingHit() {
        return false;
    }

    default boolean movesTheUsersBody() {
        return false;
    }

    int cooldownTicks();

    default int chargeMaxTicks() {
        return 0;
    }

    default int chargeMaxTicks(JojohaPlayerData data) {
        return chargeMaxTicks();
    }

    default int timeStopTicks(JojohaPlayerData data) {
        return 0;
    }

    default boolean chargesTime() {
        return false;
    }

    default boolean isSustained() {
        return false;
    }

    default boolean isSustainActive(Player player) {
        return false;
    }

    default int cooldownTicks(int chargeTicks) {
        return cooldownTicks();
    }

    default int lockoutTicks(JojohaPlayerData data) {
        return 0;
    }

    default boolean chargeBreaksOnDamage() {
        return false;
    }

    default boolean canBeCalledOff(JojohaPlayerData data) {
        return false;
    }

    default void callOff(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
    }

    default boolean isRunning(JojohaPlayerData data) {
        return false;
    }

    float energyCost();

    default float energyCost(int chargeTicks) {
        return energyCost();
    }

    default TrustTier minimumTrust() {
        return TrustTier.EMERGING;
    }

    boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand);

    default boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand,
                             int chargeTicks) {
        return activate(player, data, stand);
    }
}
