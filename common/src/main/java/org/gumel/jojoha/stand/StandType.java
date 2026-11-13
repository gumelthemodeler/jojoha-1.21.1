package org.gumel.jojoha.stand;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.supporter.SkinCatalog;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record StandType(ResourceLocation id, ResourceLocation model, List<StandSkin> skins,
                        ResourceLocation animation, int auraColor,
                        StandRange range, StandArchetype archetype,
                        StandForm form,
                        Supplier<SoundEvent> voice,
                        List<ResourceLocation> signatureSkills,
                        List<ResourceLocation> passives) {

    public StandType(ResourceLocation id, ResourceLocation model, List<StandSkin> skins,
                     ResourceLocation animation, int auraColor,
                     StandRange range, StandArchetype archetype,
                     List<ResourceLocation> signatureSkills,
                     List<ResourceLocation> passives) {
        this(id, model, skins, animation, auraColor, range, archetype,
                StandForm.HUMANOID, null, signatureSkills, passives);
    }

    public StandType(ResourceLocation id, ResourceLocation model, List<StandSkin> skins,
                     ResourceLocation animation, int auraColor,
                     StandRange range, StandArchetype archetype, StandForm form,
                     List<ResourceLocation> signatureSkills,
                     List<ResourceLocation> passives) {
        this(id, model, skins, animation, auraColor, range, archetype, form, null,
                signatureSkills, passives);
    }

    public List<ResourceLocation> allPassives() {
        if (archetype.passives().isEmpty()) {
            return passives;
        }

        List<ResourceLocation> all = new ArrayList<>(archetype.passives());
        all.addAll(passives);
        return List.copyOf(all);
    }

    public ResourceLocation texture() {
        return skins.get(0).texture();
    }

    public ResourceLocation textureFor(int skin) {
        return skinAt(skin).texture();
    }

    public String skinNameKey(int skin) {
        return skinAt(skin).translationKey();
    }

    public MutableComponent skinName(int skin) {
        if (SkinCatalog.remote(skin)) {
            SkinCatalog.Entry entry = SkinCatalog.get(skin);
            return entry != null ? Component.literal(entry.name())
                    : Component.translatable("menu.jojoha.skins.supporter");
        }
        return Component.translatable(skinNameKey(skin));
    }

    public int auraColorFor(int skin) {
        SkinCatalog.Entry remote = SkinCatalog.get(skin);
        if (remote != null && remote.aura() != SkinCatalog.NO_AURA) {
            return remote.aura();
        }
        int own = skinAt(skin).auraColor();
        return own == StandSkin.INHERIT ? auraColor : own;
    }

    public float auraRedFor(int skin) {
        return ((auraColorFor(skin) >> 16) & 0xFF) / 255F;
    }

    public float auraGreenFor(int skin) {
        return ((auraColorFor(skin) >> 8) & 0xFF) / 255F;
    }

    public float auraBlueFor(int skin) {
        return (auraColorFor(skin) & 0xFF) / 255F;
    }

    public boolean supporterSkin(int skin) {
        return SkinCatalog.remote(skin)
                || skin >= 0 && skin < skins.size() && skins.get(skin).supporter();
    }

    public int rollableSkinCount() {
        int count = 0;
        for (StandSkin entry : skins) {
            if (entry.rollable()) {
                count++;
            }
        }
        return count;
    }

    public boolean rollableSkin(int skin) {
        return !SkinCatalog.remote(skin) && skin >= 0 && skin < skins.size() && skins.get(skin).rollable();
    }

    @Nullable
    public String skinUnlock(int skin) {
        return SkinCatalog.remote(skin) || skin < 0 || skin >= skins.size() ? null : skins.get(skin).unlock();
    }

    public int skinIndex(String translationKey) {
        for (int i = 0; i < skins.size(); i++) {
            if (skins.get(i).translationKey().equals(translationKey)) {
                return i;
            }
        }
        return -1;
    }

    public float skinWeight(int skin) {
        StandSkin entry = skinAt(skin);
        if (!entry.rollable() || SkinCatalog.remote(skin)) {
            return 0F;
        }
        return Math.max(0F, StandTuning.value(
                entry.translationKey().replace('.', '_'), entry.weight()));
    }

    private StandSkin skinAt(int skin) {
        if (SkinCatalog.remote(skin)) {
            return skins.get(0);
        }
        return skins.get(Mth.clamp(skin, 0, skins.size() - 1));
    }

    public int skinCount() {
        return skins.size();
    }

    public float auraRed() {
        return ((auraColor >> 16) & 0xFF) / 255F;
    }

    public float auraGreen() {
        return ((auraColor >> 8) & 0xFF) / 255F;
    }

    public float auraBlue() {
        return (auraColor & 0xFF) / 255F;
    }
}
