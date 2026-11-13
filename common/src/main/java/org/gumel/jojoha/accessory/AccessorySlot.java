package org.gumel.jojoha.accessory;

public enum AccessorySlot {
    HEAD("head"),
    NECK("neck"),
    GLOVES("gloves"),
    RING("ring"),
    BACK("back"),
    CHARM("charm");

    private final String name;

    AccessorySlot(String name) {
        this.name = name;
    }

    public String translationKey() {
        return "accessory.jojoha.slot." + name;
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }
}
