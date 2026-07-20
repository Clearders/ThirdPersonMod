package dev.thirdpersonmod.camera;

public enum CompositionPreset {
    CINEMATIC_RIGHT_SHOULDER(3.6, 0.75, 0.35, ShoulderSide.RIGHT),
    CINEMATIC_LEFT_SHOULDER(3.6, 0.75, 0.35, ShoulderSide.LEFT),
    COMPACT_RIGHT_SHOULDER(2.8, 0.55, 0.25, ShoulderSide.RIGHT),
    COMPACT_LEFT_SHOULDER(2.8, 0.55, 0.25, ShoulderSide.LEFT),
    VANILLA_SAFE(4.0, 0.0, 0.0, ShoulderSide.RIGHT);

    private final double distance;
    private final double shoulderOffset;
    private final double verticalOffset;
    private final ShoulderSide shoulder;

    CompositionPreset(double distance, double shoulderOffset, double verticalOffset, ShoulderSide shoulder) {
        this.distance = distance;
        this.shoulderOffset = shoulderOffset;
        this.verticalOffset = verticalOffset;
        this.shoulder = shoulder;
    }

    public double distance() {
        return this.distance;
    }

    public double shoulderOffset() {
        return this.shoulderOffset;
    }

    public double verticalOffset() {
        return this.verticalOffset;
    }

    public ShoulderSide shoulder() {
        return this.shoulder;
    }
}
