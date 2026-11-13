package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayList;
import java.util.List;

public final class PassiveLoadout {
    public static final int STAND_SLOTS = 2;

    private PassiveLoadout() {
    }

    public static List<ResourceLocation> inEffect(JojohaPlayerData data) {
        List<ResourceLocation> running = new ArrayList<>();
        if (!data.stand.isPresent()) {
            return running;
        }
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        for (ResourceLocation id : type.archetype().passives()) {
            if (SkillTrees.skillUnlocked(data, id)) {
                running.add(id);
            }
        }
        for (ResourceLocation family : ownInEffect(data, type)) {
            running.add(member(data, type, family));
        }
        return running;
    }

    public static boolean isInEffect(JojohaPlayerData data, ResourceLocation id) {
        return inEffect(data).contains(id);
    }

    public static boolean isArchetype(JojohaPlayerData data, ResourceLocation id) {
        return data.stand.isPresent()
                && StandTypes.byIdOrDefault(data.stand.standId()).archetype().passives().contains(id);
    }

    public static ResourceLocation familyOf(ResourceLocation id) {
        StandPassive passive = StandPassives.byId(id);
        return passive == null || passive.replaces() == null ? id : passive.replaces();
    }

    public static boolean hasChoice(JojohaPlayerData data) {
        return data.stand.isPresent()
                && ownFamilies(data, StandTypes.byIdOrDefault(data.stand.standId())).size() > STAND_SLOTS;
    }

    public static ResourceLocation displacedBy(JojohaPlayerData data, ResourceLocation id) {
        if (!data.stand.isPresent() || isArchetype(data, id)) {
            return null;
        }
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        ResourceLocation family = familyOf(id);
        List<ResourceLocation> running = ownInEffect(data, type);
        if (running.contains(family) || !ownFamilies(data, type).contains(family)
                || running.size() < STAND_SLOTS) {
            return null;
        }
        return member(data, type, running.get(0));
    }

    public static void choose(JojohaPlayerData data, ResourceLocation id) {
        if (!data.stand.isPresent() || isArchetype(data, id)) {
            return;
        }
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        ResourceLocation family = familyOf(id);
        if (!ownFamilies(data, type).contains(family)) {
            return;
        }
        List<ResourceLocation> running = ownInEffect(data, type);
        if (running.contains(family)) {
            return;
        }

        List<ResourceLocation> next = new ArrayList<>(running);
        next.add(family);
        while (next.size() > STAND_SLOTS) {
            next.remove(0);
        }
        data.setChosenPassives(next);
    }

    private static List<ResourceLocation> ownInEffect(JojohaPlayerData data, StandType type) {
        List<ResourceLocation> available = ownFamilies(data, type);
        List<ResourceLocation> running = new ArrayList<>();

        for (ResourceLocation chosen : data.getEquippedPassives()) {
            ResourceLocation family = familyOf(chosen);
            if (available.contains(family) && !running.contains(family) && running.size() < STAND_SLOTS) {
                running.add(family);
            }
        }
        for (ResourceLocation family : available) {
            if (running.size() >= STAND_SLOTS) {
                break;
            }
            if (!running.contains(family)) {
                running.add(family);
            }
        }
        return running;
    }

    private static List<ResourceLocation> ownFamilies(JojohaPlayerData data, StandType type) {
        List<ResourceLocation> families = new ArrayList<>();
        for (ResourceLocation id : type.passives()) {
            StandPassive passive = StandPassives.byId(id);
            if (passive != null && passive.replaces() == null && SkillTrees.skillUnlocked(data, id)) {
                families.add(id);
            }
        }
        return families;
    }

    private static ResourceLocation member(JojohaPlayerData data, StandType type, ResourceLocation family) {
        for (ResourceLocation id : type.passives()) {
            StandPassive passive = StandPassives.byId(id);
            if (passive != null && family.equals(passive.replaces()) && SkillTrees.skillUnlocked(data, id)) {
                return id;
            }
        }
        return family;
    }
}
