package org.gumel.jojoha.client;

public enum BarCorner {
    TOP_LEFT(0F, true),
    TOP_CENTRE(0.5F, true),
    TOP_RIGHT(1F, true),
    BOTTOM_LEFT(0F, false),
    BOTTOM_RIGHT(1F, false);

    private final float across;
    private final boolean top;

    BarCorner(float across, boolean top) {
        this.across = across;
        this.top = top;
    }

    public boolean top() {
        return top;
    }

    public int x(int screenWidth, int barWidth, int margin) {
        int room = screenWidth - barWidth - 2 * margin;
        return margin + Math.round(room * across);
    }

    public int y(int screenHeight, int barHeight, int margin) {
        return top ? margin : screenHeight - barHeight - margin;
    }
}
