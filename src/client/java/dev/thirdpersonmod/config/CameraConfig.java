package dev.thirdpersonmod.config;

import dev.thirdpersonmod.camera.CameraMath;
import dev.thirdpersonmod.camera.CompositionPreset;
import dev.thirdpersonmod.camera.ShoulderSide;

public final class CameraConfig {
    public boolean enabled = true;
    public double distance = 3.6;
    public double shoulderOffset = 0.75;
    public double verticalOffset = 0.35;
    public double minimumDistance = 0.35;
    public double collisionRadius = 0.14;
    public double collisionSafetyMargin = 0.1;
    public double positionSmoothingSpeed = 14.0;
    public double collisionInSpeed = 32.0;
    public double collisionOutSpeed = 9.0;
    public double shoulderTransitionSpeed = 11.0;
    public CompositionPreset compositionPreset = CompositionPreset.CINEMATIC_RIGHT_SHOULDER;
    public ShoulderSide defaultShoulder = ShoulderSide.RIGHT;
    public boolean disableWhileRiding = true;
    public boolean disableWhileSleeping = true;
    public boolean disableWhileSwimming = false;
    public boolean disableWhileCrawling = true;
    public boolean disableWhileFallFlying = true;
    public boolean debugCameraOwnership = false;

    public void validate() {
        this.distance = finiteClamp(this.distance, 1.0, 12.0, 3.6);
        this.shoulderOffset = finiteClamp(this.shoulderOffset, 0.0, 2.0, 0.75);
        this.verticalOffset = finiteClamp(this.verticalOffset, -1.0, 3.0, 0.35);
        this.minimumDistance = finiteClamp(this.minimumDistance, 0.1, 2.0, 0.35);
        this.minimumDistance = Math.min(this.minimumDistance, this.distance);
        this.collisionRadius = finiteClamp(this.collisionRadius, 0.01, 0.5, 0.14);
        this.collisionSafetyMargin = finiteClamp(this.collisionSafetyMargin, 0.0, 0.5, 0.1);
        this.positionSmoothingSpeed = finiteClamp(this.positionSmoothingSpeed, 0.0, 60.0, 14.0);
        this.collisionInSpeed = finiteClamp(this.collisionInSpeed, 0.0, 60.0, 32.0);
        this.collisionOutSpeed = finiteClamp(this.collisionOutSpeed, 0.0, 60.0, 9.0);
        this.shoulderTransitionSpeed = finiteClamp(this.shoulderTransitionSpeed, 0.0, 60.0, 11.0);
        if (this.compositionPreset == null) {
            this.compositionPreset = CompositionPreset.CINEMATIC_RIGHT_SHOULDER;
        }
        if (this.defaultShoulder == null) {
            this.defaultShoulder = this.compositionPreset.shoulder();
        }
    }

    public void applyPreset(CompositionPreset preset) {
        this.compositionPreset = preset == null ? CompositionPreset.CINEMATIC_RIGHT_SHOULDER : preset;
        this.distance = this.compositionPreset.distance();
        this.shoulderOffset = this.compositionPreset.shoulderOffset();
        this.verticalOffset = this.compositionPreset.verticalOffset();
        this.defaultShoulder = this.compositionPreset.shoulder();
        validate();
    }

    private static double finiteClamp(double value, double minimum, double maximum, double fallback) {
        return Double.isFinite(value) ? CameraMath.clamp(value, minimum, maximum) : fallback;
    }
}
