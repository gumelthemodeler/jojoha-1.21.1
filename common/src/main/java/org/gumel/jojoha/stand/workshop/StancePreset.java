package org.gumel.jojoha.stand.workshop;

import org.gumel.jojoha.data.StandPlacement;

public enum StancePreset {
    CLASSIC("classic", StandPlacement.NONE, RestPose.CALM),
    LEFT("left", new StandPlacement(-1.5F, 0F, 0F, 0F), RestPose.CALM),
    SHOULDER("shoulder", new StandPlacement(-0.5F, 0.4F, 0.5F, 0F), RestPose.CALM),
    LOOMING("looming", new StandPlacement(-0.9F, 1.2F, 0.2F, 0F), RestPose.READY),
    VANGUARD("vanguard", new StandPlacement(-0.9F, -0.2F, 1.5F, 0F), RestPose.READY),
    BACK_TO_BACK("back_to_back", new StandPlacement(-0.9F, 0F, 0.2F, 180F), RestPose.CALM);

    private final String key;
    private final StandPlacement placement;
    private final RestPose rest;

    StancePreset(String key, StandPlacement placement, RestPose rest) {
        this.key = key;
        this.placement = placement;
        this.rest = rest;
    }

    public String key() {
        return key;
    }

    public StandPlacement placement() {
        return placement;
    }

    public RestPose rest() {
        return rest;
    }
}
